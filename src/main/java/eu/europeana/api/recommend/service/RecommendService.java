package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.config.WebClients;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    private static final String SOLR_ID_FIELD = "europeana_id";

    private RecommendSettings config;
    private WebClient searchApiClient;
    private WebClient rengineClient;

    public RecommendService(RecommendSettings config, WebClients webClients) {
        this.config = config;
        this.searchApiClient = webClients.getSearchApiClient();
        this.rengineClient = webClients.getRecommendEngineClient();
    }

    public Mono getRecommendationsForSet(String setId, int pageSize, String token, String apikey) {
        StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
                .append("?bucket=").append(setId)
                .append("&size=").append(pageSize);
        String[] recommendedIds = getRecommendations(s.toString(), token, apikey).block();
        if (recommendedIds == null || recommendedIds.length == 0) {
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
        String[] recommendedIds = getRecommendations(s.toString(), token, apikey).block();
        if (recommendedIds == null || recommendedIds.length == 0) {
            LOG.warn("No recommended records for record {}", recordId);
            return null;
        } else {
            LOG.debug("Recommend engine returned {} items for record {}", recommendedIds.length, recordId);
        }

        return getSearchApiResponse(recommendedIds, pageSize, apikey);
    }

    private Mono<String[]> getRecommendations(String recommendQuery, String token, String apikey) {
        String authValue = token;
        if (StringUtils.isBlank(authValue)) {
            authValue = "APIKEY " + apikey;
        }
        return rengineClient.get()
                .uri(recommendQuery)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, authValue)
                .retrieve()
                .bodyToMono(String[].class);
    }

    /**
     * We use reactive (non-blocking) WebClient to retrieve data from Search API
     */
    private Mono<Object> getSearchApiResponse(String[] recordIds, int maxResults, String wskey) {
        String query = generateSearchQuery(recordIds, maxResults, wskey);
        return searchApiClient.get()
                .uri(query)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(Object.class);
    }

    /**
     * Constructs a Search API query in the form
     * <pre>query=europeana_id:("/x1/y1" OR "/x2/y2 OR "/x3/y3")&pageSize=3&profile=minimal&wskey=[wskey]</pre>
     */
    private String generateSearchQuery(String[] recordIds, int maxResults, String wskey) {
        StringBuilder s = new StringBuilder(50).append("?query=");
        for  (int i = 0; i < maxResults && i < recordIds.length; i++) {
            if (i > 0) {
                s.append(" OR ");
            }
            s.append(SOLR_ID_FIELD).append(":").append("\"").append(recordIds[i]).append("\"");
        }
        s.append("&pageSize=").append(recordIds.length)
                .append("&profile=minimal")
                .append("&wskey=").append(wskey);
        return s.toString();
    }

}
