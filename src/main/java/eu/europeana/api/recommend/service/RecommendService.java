package eu.europeana.api.recommend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.config.WebClients;
import eu.europeana.api.recommend.exception.EntityNotFoundException;
import eu.europeana.api.recommend.model.EntityRecommendRequest;
import eu.europeana.api.recommend.model.Labels;
import eu.europeana.api.recommend.model.UserSignalRequest;
import eu.europeana.api.recommend.util.EntityAPIUtils;
import eu.europeana.api.recommend.util.SearchAPIUtils;
import eu.europeana.api.recommend.util.SetAPIUtils;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

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
    private WebClient searchApiClient;
    private WebClient entityApiClient;
    private WebClient setApiClient;
    private WebClient rengineClient;


    public RecommendService(RecommendSettings config, WebClients webClients) {
        this.config = config;
        this.searchApiClient = webClients.getSearchApiClient();
        this.entityApiClient = webClients.getEntityApiClient();
        this.setApiClient = webClients.getSetApiClient();
        this.rengineClient = webClients.getRecommendEngineClient();
        LOG.info("Allow API keys only = {}", config.getTestApikeyOnly());
    }

    public Mono getRecommendationsForSet(String setId, int pageSize, int page, String seed, String token, String apikey) {
        StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
                .append("?bucket=").append(setId)
                .append("&size=").append(pageSize)
                .append("&skip=").append(pageSize * page);
        if (seed != null) {
            s.append("&seed=").append(seed);
        }
        // tmp add session_id if there's no token for testing purposes
        if (config.getTestApikeyOnly() && token == null) {
            s.append("&session_id=test");
        }

        String[] recommendedIds = getRecommendations(s.toString(), null, token, apikey).block();
        if (recommendedIds == null || recommendedIds.length == 0) {
            LOG.warn("No recommended records for set {}", setId);
            return null;
        } else {
            LOG.debug("Recommend engine returned {} items for set {}", recommendedIds.length, setId);
        }

        return getSearchApiResponse(recommendedIds, pageSize, apikey);
    }

    /**
     * Submits the user signals to Engine
     * @param ids the ids provided in the request
     * @param signalType either accept OR reject
     * @param userId the user-id extracted from token provided
     * @param token
     * @param apikey
     */
    public void submitUserSignals(String [] ids, String signalType,  String userId, String token, String apikey) {
       StringBuilder s = new StringBuilder(config.getREngineEventsPath());
       String requestBody = getSubmitUserSignalRequest(ids, signalType, userId);
       String [] response = getRecommendations(s.toString(), requestBody, token, apikey).block();
       if (response == null || response.length == 0) {
           if (LOG.isDebugEnabled()) {
               LOG.debug("Signal {} submitted successfully for {}", signalType.toUpperCase(Locale.ROOT), Arrays.toString(ids));
           }
       } else {
           if (LOG.isWarnEnabled()) {
               LOG.warn("No response from recommendation engine for {}", Arrays.toString(ids));
           }
       }
    }

    /**
     * returns the request body for submit user Signals
     * @param ids
     * @param signalType accept OR reject
     * @param userId
     * @return
     */
    private String getSubmitUserSignalRequest(String [] ids, String signalType, String userId) {
        List<UserSignalRequest> request = new ArrayList<>();
        for(String id : ids) {
            request.add(new UserSignalRequest(userId, id, signalType));
        }
        return serialiseRequest(request);
    }

    public Mono getRecommendationsForRecord(String recordId, int pageSize, int page, String seed, String token, String apikey) {
        StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
                .append("?item=").append(recordId)
                .append("&size=").append(pageSize)
                .append("&skip=").append(pageSize * page);
        if (seed != null) {
            s.append("&seed=").append(seed);
        }
        // tmp add session_id if there's no token for testing purposes
        if (config.getTestApikeyOnly() && token == null) {
            s.append("&session_id=test");
        }

        String[] recommendedIds = getRecommendations(s.toString(), null, token, apikey).block();
        if (recommendedIds == null || recommendedIds.length == 0) {
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
     * @param type
     * @param id
     * @param pageSize
     * @param token
     * @param apikey
     * @return
     * @throws EntityNotFoundException
     */
    public Mono getRecommendationsForEntity(String type, String id, int pageSize, String token, String apikey) throws EntityNotFoundException {
       // generate entity ID
       String entityId = EntityAPIUtils.buildEntityId(type, id);
       // create request body for entity api
       String requestBody = getEntityRecommendationRequest(entityId, apikey);

       StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
              .append("/entity")
              .append("?size=").append(pageSize);
        // tmp add session_id if there's no token for testing purposes
        if (config.getTestApikeyOnly()  && token == null) {
            s.append("&session_id=test");
        }

       String[] recommendedIds = getRecommendations(s.toString(), requestBody, token, apikey).block();
       if (recommendedIds == null || recommendedIds.length == 0) {
           LOG.warn("No recommended records for entity {}", entityId);
           return null;
       } else {
            LOG.debug("Recommend engine returned {} items for entity {} : {}", recommendedIds.length, entityId, recommendedIds);
       }
       return getSearchApiResponse(recommendedIds, pageSize, apikey);
    }

    /**
     * Generates the request body for entity recommendations
     * 1) calls Entity API to get all the skos:prefLabel(s) and skos:altLabel(s) in all languages
     * 2) calls User Sets API to check whether an Entity Set exists and obtain all the items IDs
     *    Default page size : (page=0&pageSize=20);
     * 3) construct a JSON object composed of the labels and items to be sent to the Recommendation Engine API
     * 4) if there are no results from any of the above api : empty request will be formed and sent to engine
     * ex: {"labels":[{"title":"","descriptions":null}],"items":[""]}
     * This is to avoid Internal Server errors from engine due to null postBody
     *
     * @param entityId
     * @param apikey
     * @return
     * @throws EntityNotFoundException
     */
    public String getEntityRecommendationRequest(String entityId, String apikey) throws EntityNotFoundException {
        EntityRecommendRequest request = new EntityRecommendRequest();

        // fetch the skos:prefLabels and skos:altlabels from Entity API
        StringBuilder entityApiUrl = new StringBuilder(config.getEntityApiEndpoint())
                .append(EntityAPIUtils.entityApiSearchQuery(entityId, apikey));
        getLabels(request, entityApiUrl.toString(), entityId);

        // fetch Entity Set items from set api
        StringBuilder setApiUrl = new StringBuilder(config.getSetApiEndpoint()).
                append(SetAPIUtils.setApiSearchQuery(entityId, config.getSetApiPageSize(), apikey));
        getItems(request, setApiUrl.toString());

        // serialise the request
        return serialiseRequest(request);
    }

    /**
     * Sets the labels in the EntityRecommendRequest
     * extracts the prefLabel and altLabel from entity api json response
     *
     * @param request
     * @param entityUrl
     */
    private void getLabels(EntityRecommendRequest request, String entityUrl, String entityId) throws EntityNotFoundException {
        try {
            String response = entityApiClient.get()
                    .uri(entityUrl)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JSONObject jsonObject = new JSONObject(response);
            // if entity doesn't exist, throw EntityNotFoundException exception
            if (!checkIfEntityExists(jsonObject)) {
                throw new EntityNotFoundException("Entity " + entityId + " not found.");
            }
            List<String> extractedLabels = EntityAPIUtils.extractLabels(jsonObject);

            if (LOG.isDebugEnabled()) {
                LOG.debug("{} labels for entity {}", extractedLabels.size(), entityId);
            }

            List<Labels> labels = new ArrayList<>();
            if (!extractedLabels.isEmpty()) {
                for (String label : extractedLabels) {
                    labels.add(new Labels(label));
                }
                request.setLabels(labels);
            } else {
                labels.add(new Labels(""));
                request.setLabels(labels);
            }
        } catch (JSONException e) {
            LOG.error("Error parsing entity api response for url {}. {}", entityUrl, e);
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
            String response = setApiClient.get()
                    .uri(setApiUrl)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JSONObject jsonObject = new JSONObject(response);
            if (checkIfEntityExists(jsonObject)) {
                List<String> items= SetAPIUtils.extractItems(jsonObject);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} results from Set API", items.size());
                }
                if (!items.isEmpty()) {
                    request.setItems(items.toArray(new String[0]));
                }
            }
        } catch (JSONException e) {
            LOG.error("Error parsing set api response for url {}. {}", setApiUrl, e);
        }
    }

    /**
     * Will check in json if total value is not 0 in the ResultPage
     *
     * @param jsonObject
     * @throws JSONException
     */
    private boolean checkIfEntityExists(JSONObject jsonObject) throws JSONException {
        return Integer.parseInt(String.valueOf(jsonObject.get(EntityAPIUtils.TOTAL))) != 0;
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("  POST Body = {}", jsonBody);
        }
        return rengineClient.post()
                .uri(recommendQuery)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, authValue)
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve()
                .bodyToMono(String[].class);
    }

    /**
     * We use reactive (non-blocking) WebClient to retrieve data from Search API
     */
    private Mono getSearchApiResponse(String[] recordIds, int maxResults, String wskey) {
        String query = SearchAPIUtils.generateSearchQuery(recordIds, maxResults, wskey);
        Mono<Object> response = searchApiClient.get()
                .uri(query)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(Object.class);

        if (LOG.isDebugEnabled()) {
            // TODO WARNING since we do a block here this will cause us to do the request to Search API twice, once here
            // and once in the controller where the other block() is
            LinkedHashMap map = (LinkedHashMap) response.block();
            if (map != null) {
                Integer nrResults = (Integer) map.get(SearchAPIUtils.TOTAL_RESULTS);
                if (nrResults != recordIds.length) {
                    LOG.warn("{} results from Search API, expected {}", nrResults, recordIds.length);
                } else {
                    LOG.debug("{} results from Search API", nrResults);
                }
            }
        }

        return response;
    }
}
