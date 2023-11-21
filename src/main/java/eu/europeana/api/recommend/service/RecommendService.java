package eu.europeana.api.recommend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.recommend.common.RecordId;
import eu.europeana.api.recommend.common.model.EmbeddingResponse;
import eu.europeana.api.recommend.exception.*;
import eu.europeana.api.recommend.model.Set;
import eu.europeana.api.recommend.model.*;
import eu.europeana.api.recommend.util.MilvusUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Service that interacts with Milvus to retrieve similar records
 * and then uses Search API to convert the record-ids Milvus returned into a proper response
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 * Major refactoring Aug 2023
 */
@Service
public class RecommendService {

    private static final Logger LOG = LogManager.getLogger(RecommendService.class);

    private static final int WEIGHT_SET_METADATA = 3;
    private static final int WEIGHT_SET_ITEMS    = 1;
    private static final int WEIGHT_ENTITY_METADATA  = 10;
    private static final int WEIGHT_ENTITY_SET_ITEMS = 1;

    private final MilvusService milvus;
    private final EmbeddingsService embeddings;
    private final SearchApiService searchApi;
    private final SetApiService setApi;
    private final EntityApiService entityApi;

    @Autowired
    public RecommendService(MilvusService milvus, EmbeddingsService embeddings, SearchApiService searchApi,
                            SetApiService setApiService, EntityApiService entityApi) {
        this.milvus = milvus;
        this.embeddings = embeddings;
        this.searchApi = searchApi;
        this.entityApi = entityApi;
        this.setApi = setApiService;
    }

    /**
     * Given a record id, this returns a json response containing basic data about similar records or null
     * @param recordId record id for which similar records need to be found
     * @param pageSize optional, number of similar records to return, between 1 and 50
     * @param page optional, extra page of similar records, between 1 and 40 // TODO not supported yet
     * @param seed // TODO not supported yet
     * @param apikey optional API key
     * @param token optional authentication token (not used at the moment)
     * @return json response from Search API with similar records data
     * @throws RecommendException when the record is not in Milvus and cannot be found with Search API
     */
    public Mono<SearchApiResponse> getRecommendationsForRecord(RecordId recordId, int pageSize, int page, String seed,
                                                               String apikey, String token) throws RecommendException {
        List<Float> vector = milvus.getVectorForRecord(recordId);
        if (vector == null || vector.isEmpty()) {
            LOG.warn("Record {} not in Milvus", recordId);
            if (!searchApi.checkRecordExists(recordId, apikey, token)) {
                throw new RecordNotFoundException("Record with id " + recordId.getEuropeanaId() + " not found");
            }
        } else {
            LOG.trace("Vector for record {} = {}", recordId, vector);

            if (!vector.isEmpty()) {
                Collection<Recommendation> unsorted = milvus.getSimilarRecords(List.of(vector), pageSize, List.of(recordId), 1).values();
                List<Recommendation> sorted = unsorted.stream().sorted(Comparator.reverseOrder()).toList();
                LOG.trace("{} recommendations for record {} = {}", sorted.size(), recordId, sorted);
                return searchApi.generateResponse(sorted, pageSize, apikey, token);
            }
        }
        return null;
    }

    /**
     * Given a set id, this returns a json response containing basic data about records that are similar to records in that
     * set. Note that the set has to be a 'closed' set, we return 0 results for open sets (query-based).
     * @param setId set id for which similar records need to be found
     * @param pageSize optional, number of similar records to return, between 1 and 50
     * @param page optional, extra page of similar records, between 1 and 40 // TODO not supported yet
     * @param seed // TODO not supported yet
     * @param apikey optional API key (used for requests to Set API and Search API if provided)
     * @param token optional authentication token (used for requests to Set API and Search API if provided)
     * @return json response with similar records data or null if the provided set is an open set
     * @throws RecommendException when the set cannot be found in Set API
     */
    public Mono<SearchApiResponse> getRecommendationsForSet(String setId, int pageSize, int page, String seed,
                                                            String apikey, String token) throws RecommendException {
        // TODO better use of Mono and parallelism

        // 1. get relevant data from setId
        Set set = null;
        try {
            set = setApi.getSetData(setId, apikey, token).block();
            LOG.trace("Contents of set {} = {}", setId, set);
            if (set == null) {
                throw new SetNotFoundException("Set " + setId + " not found");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.getDefault()).contains("not found")) {
                throw new SetNotFoundException("Set " + setId + " not found");
            }
            throw e; // rethrow other errors than 404
        }


        // 2. check if it's a closed set (no recommendations for open sets)
        if (setApi.isOpenSet(set)) {
            return null;
        }

        // 3. Use set metadata to generate vector with Embeddings API and use that to get recommendations, multiply with factor 3
        List<RecordId> setRecordIds = set.getItemsRecordId();
        Map<String, Recommendation> recommendMetadata = getRecommendationsForSetMetadata(set, setRecordIds, pageSize);
        LOG.trace("{} recommendations for set meta data {} = {}", recommendMetadata.size(), set.getId(), recommendMetadata);

        // 4. get milvus recommendations for the items in the set
        Map<String, Recommendation> recommendItems = getRecommendationsForSetItems(setRecordIds, pageSize);
        LOG.error("{} recommendations for set items {} = {}", recommendItems.size(), set.getId(), recommendItems);

        // 5. merge, sort per weight and get most relevant ones
        List<Recommendation> result = mergeAndSortRecommendations(recommendMetadata, recommendItems);
        if (result.size() > pageSize) {
            result = result.subList(0, pageSize);
        }
        LOG.trace("Sorted recommendations for set {} = {}", setId, result);

        // 6. generate response using Search API
        return searchApi.generateResponse(result, pageSize, apikey, token);
    }

    // Try to get generate vector with Embeddings API and use that to get Recommendations, but if Embeddings API is not
    // available, we'll return empty result
    private Map<String, Recommendation> getRecommendationsForSetMetadata(Set set, List<RecordId> setRecordIds, int pageSize) {
        EmbeddingResponse embeddingResponse = null;
        try {
            embeddingResponse = embeddings.getVectorForSet(set).block();
        } catch (RuntimeException e) {
            LOG.error("Error sending request to Embeddings API", e);
        }

        if (embeddingResponse == null || embeddingResponse.getData().length == 0) {
            LOG.error("No response from Embeddings API for set {}", set.getId());
            return Collections.emptyMap();
        }
        List<Float> vector = EmbeddingsService.getVectors(embeddingResponse);
        LOG.trace("Vector for set {} = {}", set.getId(), vector);
        return milvus.getSimilarRecords(List.of(vector), pageSize, setRecordIds, WEIGHT_SET_METADATA);
    }

    private Map<String, Recommendation> getRecommendationsForSetItems(List<RecordId> setRecordIds, int pageSize) {
        List<List<Float>> vectors = milvus.getVectorForRecords(setRecordIds);
        if (vectors.isEmpty()) {
            return Collections.emptyMap();
        }
        return milvus.getSimilarRecords(vectors, pageSize, setRecordIds, WEIGHT_SET_ITEMS);
    }

    /**
     * Given an entity type and id, this returns a json response containing basic data about records that are related
     * to this entity. For this we use the entity's preflabels and altlabels and check with Entity API if there's a
     * "best items set" for this entity.
     * @param type entity type (e.g. agent, concept, timespan)
     * @param id entity id
     * @param pageSize optional, number of similar records to return, between 1 and 50
     * @param apikey optional API key (used for requests to other APIs if provided)
     * @param token optional authentication token (used for requests to other APIs if provided)
     * @return json response with similar records data or null if the provided set is an open set
     * @throws RecommendException when we can't retrieve the requested entity
     */
    public Mono<SearchApiResponse> getRecommendationsForEntity(String type, int id, int pageSize, String apikey, String token)
        throws RecommendException {
        // TODO better use of Mono and parallelism
        Entity entity = null;
        try {
            entity = entityApi.getEntity(type, id, apikey, token).block();
            LOG.trace("Contents of entity {}/{} = {}", type, id, entity);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.getDefault()).contains("not found")) {
                throw new EntityNotFoundException("Entity " + type + "/" + id +" not found");
            }
            throw e; // rethrow other errors than 404
        }

        // 1. get recommendations for items in associated set (if any), plus the item ids
        EntitySetItemsResult recommendEntitySetItems = getRecommendationsForEntitySetItems(type, id, pageSize, apikey, token);
        List<RecordId> setRecords = recommendEntitySetItems.itemsInSet;
        Map<String, Recommendation> recommendSetItems = recommendEntitySetItems.recommendations;
        LOG.trace("{} recommendations for entity set items {}/{} = {}", recommendSetItems.size(), type, id, recommendSetItems);

        // 2. get recommendations for entity metadata
        Map<String, Recommendation> recommendMetadata = Collections.emptyMap();
        if (entity != null) {
            recommendMetadata = getRecommendationsForEntityMetadata(entity, pageSize, setRecords);
            LOG.trace("{} recommendations for entity metadata {}/{} = {}", recommendMetadata.size(), type, id, recommendMetadata);
        }

        // 3. Merge recommendations, then sort and get the most relevant ones
        List<Recommendation> result = mergeAndSortRecommendations(recommendMetadata, recommendSetItems);
        if (result.size() > pageSize) {
            result = result.subList(0, pageSize);
        }
        LOG.trace("Sorted recommendations for entity {}/{} = {}", type, id, result);

        // 6. generate response using Search API
        return searchApi.generateResponse(result, pageSize, apikey, token);
    }

    /**
     * Use set API to see if there's a set associated with this entity. If so, generate recommendations for the
     * (top 100) items in that set. This step can be skipped if set API is not available/malfunctioning.
     * Milvus is seen as 'must-have' so if there's an error there we'll propagate that.
     */
    private EntitySetItemsResult getRecommendationsForEntitySetItems(String type, int id, int pageSize, String apikey, String token) {
        EntitySetItemsResult result = new EntitySetItemsResult();
        result.itemsInSet = Collections.emptyList();
        result.recommendations = Collections.emptyMap();

        SetSearch setSearch = null;
        try {
            setSearch = setApi.getSetDataForEntity(Entity.generateUri(type, id), apikey, token).block();
        } catch (RuntimeException e) {
            LOG.error("Error retrieving associated set data for entity {}/{}", type, id, e);
            return result;
        }

        Set entitySet = null;
        if (setSearch == null || setSearch.getTotal() == 0) {
            LOG.trace("No set associated with entity {}/{}", type, id);
        } else if (setSearch.getTotal() == 1) {
            if (LOG.isTraceEnabled()) {
                int count = 0;
                if (setSearch.getItems()[0].getItems() != null) {
                    count = setSearch.getItems()[0].getItems().length;
                }
                LOG.trace("Found {} items associated with entity {}/{}", count, type, id);
            }
            entitySet = setSearch.getItems()[0];
        } else {
            LOG.warn("Multiple sets associated with entity {}/{}, using first", type, id);
            entitySet = setSearch.getItems()[0];
        }

        // generate vectors
        if (entitySet != null) {
            result.itemsInSet = entitySet.getItemsRecordId();
            List<List<Float>> vectors = milvus.getVectorForRecords(result.itemsInSet);
            LOG.trace("Vectors of items associated with entity {}/{} = {}", type, id, vectors);

            if (vectors.isEmpty()) {
                LOG.trace("No recommendations for entity set items {}/{}", type, id);
            } else {
                // Use vectors to get recommendations from Milvus
                result.recommendations = milvus.getSimilarRecords(vectors, pageSize, result.itemsInSet, WEIGHT_ENTITY_SET_ITEMS);
                LOG.trace("{} recommendations for entity set items {}/{}: {}", result.recommendations.size(), type, id, result.recommendations);
            }
        }
        return result;
    }

    /**
     * Return recommendations for entity metadata. If the Embeddings API is not available/malfunctioning we skip
     * this step. Entity API or Milvus are seen as 'must have' so if there's a problem there we'll propagate the error
     */
    private Map<String, Recommendation> getRecommendationsForEntityMetadata(Entity entity, int pageSize, List<RecordId> recordsToExclude) {
        // 2b. Generate entity metadata vector
        EmbeddingResponse embeddingResponse = null;
        try {
            embeddingResponse = embeddings.getVectorForEntity(entity).block();
        } catch (RuntimeException e) {
            LOG.error("Error sending request to Embeddings API for entity {}/{}", entity.getType(), entity.getId(), e);
        }

        if (embeddingResponse == null || embeddingResponse.getData().length == 0) {
            LOG.error("No response from Embeddings API for entity {}/{}", entity.getType(), entity.getId());
            return Collections.emptyMap();
        }
        List<Float> vector = EmbeddingsService.getVectors(embeddingResponse);
        LOG.trace("Vector for entity {}/{} = {}", entity.getType(), entity.getId(), vector);
        return milvus.getSimilarRecords(List.of(vector), pageSize, recordsToExclude, WEIGHT_ENTITY_METADATA);
    }

    List<Recommendation> mergeAndSortRecommendations(Map<String, Recommendation> map1, Map<String, Recommendation> map2) {
        if (map1 == null) {
            return (map2 == null ? null : map2.values().stream().toList());
        } else if (map2 == null) {
            return map1.values().stream().toList();
        }

        List<Recommendation> result = new ArrayList<>(map1.size() + map2.size());
        for (Map.Entry<String, Recommendation> keyValue : map1.entrySet()) {
            Recommendation target = keyValue.getValue();
            Recommendation toMerge = map2.get(keyValue.getKey());
            if (toMerge != null) {
                LOG.trace("Merging {} with {}", toMerge, target);
                target.merge(toMerge);
                map2.remove(keyValue.getKey()); // remove from map2 so we can simply add remaining items at the end
            }
            result.add(target);
        }
        result.addAll(map2.values()); // add remaining (unmerged) items from map2

        return result.stream().sorted(Comparator.reverseOrder()).toList();
    }


    /**
     * Submits the user signals to Engine
     * @param ids the ids provided in the request
     * @param signalType either accept OR reject
     * @param userId the user-id extracted from token provided
     * @param token
     * @param apikey
     * @deprecated
     */
    @Deprecated(since="Aug 2023")
    public void submitUserSignals(String [] ids, String signalType,  String userId, String apikey, String token) {
//       StringBuilder s = new StringBuilder(config.getMilvusCollection());
//       String requestBody = getSubmitUserSignalRequest(ids, signalType, userId);
//       String [] response = getRecommendations(s.toString(), requestBody, token, apikey).block();
//       if (response == null || response.length == 0) {
//           if (LOG.isDebugEnabled()) {
//               LOG.debug("Signal {} submitted successfully for {}", signalType.toUpperCase(Locale.ROOT), Arrays.toString(ids));
//           }
//       } else {
//           if (LOG.isWarnEnabled()) {
//               LOG.warn("No response from recommendation engine for {}", Arrays.toString(ids));
//           }
//       }
    }

    /**
     * returns the request body for submit user Signals
     * @param ids
     * @param signalType accept OR reject
     * @param userId
     * @return
     * @deprecated
     */
    @Deprecated(since="Aug 2023")
    private String getSubmitUserSignalRequest(String [] ids, String signalType, String userId) {
        List<UserSignalRequest> request = new ArrayList<>();
        for(String id : ids) {
            request.add(new UserSignalRequest(userId, id, signalType));
        }
        return serialiseRequest(request);
    }

    /**
     * Serialises the Recommendation Request
     * @param request
     * @return
     */
    private String serialiseRequest(Object request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            LOG.error("Error serialising the {} recommendation request. ", request.getClass(),  e);
        }
        return "";
    }

    private static class EntitySetItemsResult {
        List<RecordId> itemsInSet;
        Map<String, Recommendation> recommendations;
    }


}
