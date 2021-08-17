package eu.europeana.api.recommend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.config.WebClients;
import eu.europeana.api.recommend.model.EntityRecommendRequest;
import eu.europeana.api.recommend.model.Labels;
import eu.europeana.api.recommend.util.RecommendServiceUtils;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for requesting recommendations from the recommendation engine
 * and using Search API to convert to returned record-ids into a proper response
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@Service
public class RecommendService {

    private static final Logger LOG = LogManager.getLogger(RecommendService.class);

    private RecommendSettings config;
    private RecommendServiceUtils recommendServiceUtils;
    private WebClient searchApiClient;
    private WebClient entityApiClient;
    private WebClient setApiClient;
    private WebClient rengineClient;

    public RecommendService(RecommendSettings config, WebClients webClients) {
        this.config = config;
        recommendServiceUtils = new RecommendServiceUtils();
        this.searchApiClient = webClients.getSearchApiClient();
        this.entityApiClient = webClients.getEntityApiClient();
        this.setApiClient = webClients.getSetApiClient();
        this.rengineClient = webClients.getRecommendEngineClient();
    }

    public RecommendServiceUtils getRecommendServiceUtils() {
        return recommendServiceUtils;
    }

    public Mono getRecommendationsForSet(String setId, int pageSize, String token, String apikey) {
        StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
                .append("?bucket=").append(setId)
                .append("&size=").append(pageSize);
        String[] recommendedIds = getRecommendations(s.toString(), null, token, apikey).block();
        if (recommendedIds.length == 0) {
            LOG.warn("No recommended records for set {}", setId);
            return null;
        } else {
            LOG.debug("Recommend engine returned {} items for set {}", recommendedIds.length, setId);
        }

        return getSearchApiResponse(recommendedIds, pageSize, apikey);
    }

    public Mono getRecommendationsForRecord(String recordId, int pageSize, String token, String apikey) {
        StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
                .append("?item=").append(recordId)
                .append("&size=").append(pageSize);
        String[] recommendedIds = getRecommendations(s.toString(), null, token, apikey).block();
        if (recommendedIds.length == 0) {
            LOG.warn("No recommended records for record {}", recordId);
            return null;
        } else {
            LOG.debug("Recommend engine returned {} items for record {}", recommendedIds.length, recordId);
        }

        return getSearchApiResponse(recommendedIds, pageSize, apikey);
    }

    /**
     * Get recommendations for entity
     *
     * @param entityId
     * @param jsonBody
     * @param pageSize
     * @param token
     * @param apikey
     * @return
     */
    public Mono getRecommendationsForEntity(String entityId, String jsonBody, int pageSize, String token, String apikey) {
       StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
              .append("/entity")
             .append("?size=").append(pageSize);
       System.out.println(s.toString());
       String[] recommendedIds = getRecommendations(s.toString(), jsonBody, token, apikey).block();
        if (recommendedIds.length == 0) {
            LOG.warn("No recommended records for entity {}", entityId);
            return null;
        } else {
            LOG.debug("Recommend engine returned {} items for entity {}", recommendedIds.length, entityId);
        }

        return getSearchApiResponse(recommendedIds, pageSize, apikey);
    }

    /**
     * Generates the request body for entity recommendations
     * 1) calls Entity API to get all the skos:prefLabel(s) and skos:altLabel(s) in all languages
     * 2) calls User Sets API to check whether an Entity Set exists and obtain all the items IDs
     *    Default page size : (page=0&pageSize=20);
     * 3) construct a JSON object composed of the labels and items to be sent to the Recommendation Engine API
     *
     * @param entityId
     * @param apikey
     * @return
     */
    public String getEntityRecommendationRequest(String entityId, String apikey) {
        EntityRecommendRequest request = new EntityRecommendRequest();

        // fetch the skos:prefLabels and skos:altlabels from Entity API
        StringBuilder entityApiUrl = new StringBuilder(config.getEntityApiEndpoint())
                .append(getRecommendServiceUtils().entityApiSearchQuery(entityId, apikey));
        getLabels(request, entityApiUrl.toString());

        // fetch Entity Set items from set api
        StringBuilder setApiUrl = new StringBuilder(config.getSetApiEndpoint()).
                append(getRecommendServiceUtils().setApiSearchQuery(entityId, config.getSetApiPageSize(), apikey));
        getItems(request, setApiUrl.toString());

        // serialise the request
        return serialiseEntityRequest(request);
    }

    /**
     * Sets the labels in the EntityRecommendRequest
     * extracts the prefLabel and altLabel from entity api json response
     *
     * @param request
     * @param entityUrl
     */
    private void getLabels(EntityRecommendRequest request, String entityUrl) {
        try {
            JSONObject jsonObject = new JSONObject(getEntityApiSearchResponse(entityUrl));
            List<String> extractedLabels = getRecommendServiceUtils().extractLabels(jsonObject);
            if (!extractedLabels.isEmpty()) {
                List<Labels> labels = new ArrayList<>();
                for (String label : extractedLabels) {
                    labels.add(new Labels(label, ""));
                }
                request.setLabels(labels);
            }
        } catch (JSONException e) {
            LOG.error("Error parsing entity api response for url {}", entityUrl);
        }
    }

    /**
     * Sets the items in the EntityRecommendRequest.
     * Extracts the items from the set api json response
     *
     * @param request
     * @param setApiUrl
     */
    private void getItems(EntityRecommendRequest request, String setApiUrl) {
        try {
            JSONObject jsonObject = new JSONObject(getSetApiSearchResponse(setApiUrl));
            List<String> items=getRecommendServiceUtils().extractItems(jsonObject);
            if (!items.isEmpty()) {
                request.setItems(items.toArray(new String[0]));
            }
        } catch (JSONException e) {
            LOG.error("Error parsing set api response for url {}", setApiUrl);
        }
    }

    /**
     * Build the entity id/url
     *
     * @param type
     * @param id
     * @return
     */
    public String buildEntityId(String type, String id) {
        StringBuilder entityId = new StringBuilder("http://data.europeana.eu");
        entityId.append("/").append(type);
        entityId.append("/base/").append(id);
        return entityId.toString();
    }

    /**
     * Serialises the EntityRecommendRequest
     * @param request
     * @return
     */
    private String serialiseEntityRequest(EntityRecommendRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            LOG.error("Error serialising the Entity recommendation request");
        }
        return "";
    }

    private Mono<String[]> getRecommendations(String recommendQuery, String jsonBody, String token, String apikey) {
        String authValue = token;
        if (StringUtils.isBlank(authValue)) {
            authValue = "APIKEY " + apikey;
        }
        if (jsonBody != null) {
            return executePostRequest(recommendQuery, jsonBody, authValue);
        } else {
            return executeGetRequest(recommendQuery, authValue);
        }
    }

    private Mono<String[]> executeGetRequest(String recommendQuery, String authValue) {
        return rengineClient.get()
                .uri(recommendQuery)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, authValue)
                .retrieve()
                .bodyToMono(String[].class);
    }

    private Mono<String[]> executePostRequest(String recommendQuery, String jsonBody, String authValue) {
        return rengineClient.post()
                .uri(recommendQuery)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, authValue)
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve()
                .bodyToMono(String[].class);
    }

    private String getEntityApiSearchResponse(String url) {
       return entityApiClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String  getSetApiSearchResponse(String url) {
        return setApiClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * We use reactive (non-blocking) WebClient to retrieve data from Search API
     */
    private Mono<Object> getSearchApiResponse(String[] recordIds, int maxResults, String wskey) {
        String query = getRecommendServiceUtils().generateSearchQuery(recordIds, maxResults, wskey);
        return searchApiClient.get()
                .uri(query)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(Object.class);
    }
}
