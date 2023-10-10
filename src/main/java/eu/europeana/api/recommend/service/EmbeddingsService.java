package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.common.model.EmbeddingRecord;
import eu.europeana.api.recommend.common.model.EmbeddingRequestData;
import eu.europeana.api.recommend.common.model.EmbeddingResponse;
import eu.europeana.api.recommend.config.WebClients;
import eu.europeana.api.recommend.model.Entity;
import eu.europeana.api.recommend.model.Set;
import eu.europeana.api.recommend.util.LangUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  Service to interact with the Embeddings API. Given a set or entity, the Embeddings API
 *  can generate a vector for it
 *  @author Patrick Ehlert
 */
@Service
public class EmbeddingsService {

    private static final Logger LOG = LogManager.getLogger(EmbeddingsService.class);

    WebClient webClient;

    @Autowired
    EmbeddingsService(WebClients webclients) {
        this.webClient = webclients.getEmbeddingsClient();
    }

    /**
     * Given a set this returns an EmbeddingsResponse object containing the vectors created by Embeddings API
     * @param set to sent to Embeddings API
     * @return EmbeddingsResponse object
     */
    Mono<EmbeddingResponse> getVectorForSet(Set set) {
        String prefLanguage = LangUtils.getMostPreferredLanguageString(set.getTitle());
        if (prefLanguage == null) {
            LOG.warn("Title is mandatory field, but no title found in any preferred language: {}", set.getTitle());
            prefLanguage = LangUtils.getMostPreferredLanguageString(set.getDescription());
        }
        LOG.trace("Most preferred language for set {} is {}", set.getId(), prefLanguage);

        String title = "";
        String description = "";
        if (prefLanguage != null) {
            title = set.getTitle().get(prefLanguage);
            description = set.getDescription().get(prefLanguage);
            LOG.trace("Title for set {} to send to Embeddings API is {}", set.getId(), title);
            LOG.trace("Description for set {} to send to Embeddings API is {}", set.getId(), description);
        }

        EmbeddingRecord embedding = new EmbeddingRecord(set.getId(), // TODO not sure what's better, empty string or an id
                new String[] {(title == null ? "" : title)},
                new String[] {(description == null ? "" : description)},
                null, null, null, null);
        return doRequest(embedding);
    }

    /**
     * Given an entity this returns an EmbeddingsResponse object containing the vectors created by Embeddings API
     * @param entity to sent to Embeddings API
     * @return EmbeddingsResponse object
     */
    Mono<EmbeddingResponse> getVectorForEntity(Entity entity) {
        String prefLanguage = LangUtils.getMostPreferredLanguageString(entity.getPrefLabel());
        if (prefLanguage == null) {
            prefLanguage = LangUtils.getMostPreferredLanguageList(entity.getAltLabel());
        }
        LOG.trace("Most preferred language for entity {} is {}", entity.getId(), prefLanguage);

        List<String> labels = new ArrayList<>();
        String prefLabel = entity.getPrefLabel().get(prefLanguage);
        List<String> altLabels = entity.getAltLabel().get(prefLanguage);
        if (prefLabel != null) {
            labels.add(prefLabel);
        }
        if (altLabels != null) {
            labels.addAll(altLabels);
        }
        LOG.trace("Labels for entity {} to send to Embeddings API are {}", entity.getId(), labels);

        return doRequest(generateEmbeddingsForEntity(entity, labels));
    }

    private EmbeddingRecord generateEmbeddingsForEntity(Entity entity, List<String> labels) {
        EmbeddingRecord result = null;
        // TODO as with sets, not sure if it's best to use entity id or an empty string for id
        if (entity.isAgentType()) {
            result = new EmbeddingRecord(entity.getId(), new String[]{}, null,
                    labels.toArray(new String[0]), null, null, null);
        } else if (entity.isConceptType()) {
            result = new EmbeddingRecord(entity.getId(), new String[]{}, null,
                    null, labels.toArray(new String[0]), null, null);
        } else if (entity.isPlaceType()) {
            result = new EmbeddingRecord(entity.getId(), new String[]{}, null,
                    null, null,  labels.toArray(new String[0]), null);
        } else if (entity.isTimespanType()) {
            result = new EmbeddingRecord(entity.getId(), new String[]{}, null,
                    null, null, null, labels.toArray(new String[0]));
        } else {
            LOG.error("Unsupported entity {} with type {}!", entity.getId(), entity.getType());
        }
        return result;
    }

    private Mono<EmbeddingResponse> doRequest(EmbeddingRecord embedding) {
        EmbeddingRecord[] embeddings = new EmbeddingRecord[1];
        embeddings[0] = embedding;
        return webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(new EmbeddingRequestData(embeddings))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class);
    }

    /**
     * Given an embeddingsResponse return the vector in it.
     * Note that this method assumes only 1 vector is generated
     * @param embeddingResponse
     * @return vector
     */
    public static List<Double> getVectors(EmbeddingResponse embeddingResponse) {
        return Arrays.stream(embeddingResponse.getData()[0].getEmbedding()).toList();
    }

}
