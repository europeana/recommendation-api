package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.common.MilvusConstants;
import eu.europeana.api.recommend.common.RecordId;
import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.exception.MilvusException;
import eu.europeana.api.recommend.model.Recommendation;
import eu.europeana.api.recommend.util.MilvusUtils;
import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.CheckHealthResponse;
import io.milvus.grpc.GetLoadStateResponse;
import io.milvus.grpc.LoadState;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.GetLoadStateParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.highlevel.dml.GetIdsParam;
import io.milvus.param.highlevel.dml.response.GetResponse;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.*;

/**
 * Service for interacting with Milvus; give a recordId get the associated vector, or do
 * a similarity search
 * @author Patrick Ehlert
 */
@Service
public class MilvusService {

    private static final Logger LOG = LogManager.getLogger(MilvusService.class);

    /* TODO We are not sure yet what the highest score is that Milvus can return. In theory if the generated Embeddings
        are properly normalised it should be 1, but we regularly get values higher than 1. So until we figure out why
        this is, we set MAX_SCORE to 1 and ignore (and log) items with any value higher than that.
     */
    private static final float MAX_SCORE = 1F;

    private RecommendSettings config;
    private MilvusClient milvusClient;

    @Autowired
    public MilvusService(RecommendSettings config) {
        this.config = config;
        this.milvusClient = setupMilvusConnection();
        loadCollectionIfNecessary(config.getMilvusCollection());
    }

    private MilvusClient setupMilvusConnection() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(config.getMilvusHostName())
                .withPort(config.getMilvusPort())
                .build();
        MilvusClient result = new MilvusServiceClient(connectParam);
        R<CheckHealthResponse> response = MilvusUtils.checkResponse(result.checkHealth(), "Error checking Milvus health");
        LOG.info("Milvus connection setup, isHealthy = {}", response.getData().getIsHealthy());

        return result;
    }

    /**
     * Before we can get vectors or similar records we need to load a collection. It looks like only the first client
     * using a collection needs to do that, so unloading doesn't seem to be necessary (see also EA-3580).
     * Asking Milvus to load a collection multiple times is not a problem, so making this thread safe is not
     * necessary. See also https://milvus.io/docs/load_collection.md
     */
    private LoadState loadCollectionIfNecessary(String collectionName) {
        LoadState result = getCollectionLoadState(collectionName);
        switch (result) {
            case LoadStateLoaded ->
                LOG.info("Milvus collection {} is loaded", collectionName);
            case LoadStateLoading ->
                LOG.info("Milvus collection {} is being loaded", collectionName);
            case LoadStateNotLoad -> {
                LOG.info("Sending request to load Milvus collection {}...", collectionName);
                MilvusUtils.checkResponse(milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                        .withCollectionName(config.getMilvusCollection())
                        .build()));
                result = LoadState.LoadStateLoaded;
            }
            case LoadStateNotExist ->
                    throw new MilvusException("Collection " + collectionName + " not found in Milvus server " +
                            config.getMilvusHostName() + ":" + config.getMilvusPort(), null);
            case UNRECOGNIZED ->
                    throw new MilvusException("Milvus collection " + collectionName + " is in unknown state: "
                            + result, null);
        }
        return result;
    }

    private LoadState getCollectionLoadState(String collectionName) {
        LoadState result;
        GetLoadStateParam param = GetLoadStateParam.newBuilder().withCollectionName(collectionName).build();
        R<GetLoadStateResponse> loadResponse = milvusClient.getLoadState(param);
        if (loadResponse.getStatus() == R.Status.Success.getCode()) {
            result = loadResponse.getData().getState();
        } else {
            throw new MilvusException("Error checking Milvus collection " + collectionName + " load state: " + loadResponse, null);
        }
        return result;
    }

    /**
     * Release the loaded collection and shutdown the milvus client
     */
    @PreDestroy
    public void close() {
        if (milvusClient != null) {
            // Do not unload a collection as loading is not client specific!
            LOG.info("Closing Milvus client...");
            milvusClient.close();
        }
    }

    /**
     * Return the vectors for one or more provided RecordId (if available in Milvus)
     * @param recordIds list with ids of the records to retrieve
     * @return list of vectors, or an empty vector for items not available in Milvus.
     * Note that we use a list of Floats here since that's what Milvus supports when doing search queries
     */
    public List<List<Float>> getVectorForRecords(List<RecordId> recordIds) {
        List<String> milvusRecordIds = new ArrayList<>(recordIds.size());
        for (RecordId recordId : recordIds) {
            milvusRecordIds.add(recordId.getMilvusIdQuotes());
        }

        R<GetResponse> response = MilvusUtils.checkResponse(
                milvusClient.get(GetIdsParam.newBuilder()
                        .withCollectionName(config.getMilvusCollection())
                        .withPrimaryIds((milvusRecordIds))
                        .build()));

        List<QueryResultsWrapper.RowRecord> result = response.getData().getRowRecords();
        if (result == null || result.isEmpty()) {
            LOG.debug("No record(s) with id(s) {} found in Milvus", recordIds);
            return Collections.emptyList();
        }
        List<List<Float>> results = new ArrayList<>(recordIds.size());
        for (QueryResultsWrapper.RowRecord rowRecord : result) {
            results.add((List<Float>) rowRecord.get(MilvusConstants.VECTOR_FIELD_NAME));
        }
        return results;
    }

    /**
     * Return the vector for the provided RecordId (if available in Milvus)
     * @param recordId id of the record to retrieve
     * @return a vector (with vector being a list of floating point numbers), or an empty list if the record is not
     * available in Milvus.
     */
    public List<Float> getVectorForRecord(RecordId recordId) {
        List<List<Float>> results = getVectorForRecords(List.of(recordId));
        if (results.isEmpty()) {
            return Collections.emptyList();
        } else if (results.size() > 1) {
            // Should not happen, but for now we keep this check to verify the updater works fine deleting old sets and
            // adding new sets
            LOG.warn("{} records found in Milvus with id {}", results.size(), recordId.getMilvusId());
        }
        return results.get(0);
    }

    /**
     * Given one or more vectors, do a similarity search and return a list of similar items
     * @param vectors the vectors to search for
     * @param pageSize the number of desired results
     * @param recordIdsToExclude optional, if provided these recordIds will be excluded in the results
     * @param weight integer, used to multiply returned similarity score with this factor
     * @return a map containing the ids of the recommend items (for later ease of use) and the corresponding
     * recommendation object (the similar record and its similarity score)
     */
    // TODO Milvus v2.4. is said to support doubles, so  when that is out there should be no need for data conversion
    //  for data from Embeddings API see also https://github.com/milvus-io/milvus/discussions/18094
    public Map<String, Recommendation> getSimilarRecords(List<List<Float>> vectors, int pageSize, List<RecordId> recordIdsToExclude, int weight) {
        // create request
        SearchParam.Builder builder = SearchParam.newBuilder()
                .withCollectionName(config.getMilvusCollection())
                .withMetricType(MilvusConstants.INDEX_METRIC_TYPE) // has to match type in index
                .withOutFields(List.of(MilvusConstants.RECORD_ID_FIELD_NAME))
                .withTopK(pageSize) // max 3 results
                .withVectors(vectors)
                .withVectorFieldName(MilvusConstants.VECTOR_FIELD_NAME);
        if (recordIdsToExclude != null && !recordIdsToExclude.isEmpty()) {
            StringBuilder excludeExpr = new StringBuilder(MilvusConstants.RECORD_ID_FIELD_NAME + " not in [");
            for (RecordId recordId : recordIdsToExclude) {
                excludeExpr.append(recordId.getMilvusIdQuotes()).append(',');
            }
            // replace trailing comma with closing bracket
            excludeExpr.deleteCharAt(excludeExpr.length() - 1).append(']');
            builder.withExpr(excludeExpr.toString());
        }
        SearchResultsWrapper data = new SearchResultsWrapper(milvusClient.search(builder.build()).getData().getResults());
        if (recordIdsToExclude == null) {
            LOG.debug("Retrieved {} similar items", data.getRowRecords().size());
        } else {
            LOG.debug("Retrieved {} items excluding {}", data.getRowRecords().size(), recordIdsToExclude);
        }

        // get ids from response
        Map<String, Recommendation> result = new HashMap<>(data.getRowRecords().size());
        for (int i = 0; i < data.getRowRecords().size(); i++) {
            QueryResultsWrapper.RowRecord r = data.getRowRecords().get(i);

            // milvus returns sorted results, most similar first so no need to order ourselves
            String recordId = r.get(MilvusConstants.RECORD_ID_FIELD_NAME).toString();
            float score = (float) r.get(MilvusConstants.MILVUS_SCORE_FIELD_NAME);
            Recommendation recommendation = (new Recommendation(new RecordId(recordId), (MAX_SCORE -  score) * weight));
            LOG.trace(recommendation);
            if (recommendation.getScore() < 0) {
                LOG.warn("Record {} has milvus score {} which is higher than the current maximum {}, so ignoring item", recordId, score, MAX_SCORE);
            } else {
                result.put(recordId, recommendation);
            }
        }
        return result;
    }


}
