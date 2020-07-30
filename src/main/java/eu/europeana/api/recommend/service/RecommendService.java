package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.config.WebClients;
import eu.europeana.api.recommend.exception.RecommendException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${tmp.dummy.apikey}")
    private String dummyApiKey;
    // TODO until we have a valid token for testing we use an API key provided in the property files + we always return
    // the same 2 records as recommendation for a set, plus the same record as recommendation for a record

    private RecommendSettings config;
    private WebClient searchApiClient;
    private WebClient rengineClient;

    public RecommendService(RecommendSettings config, WebClients webClients) {
        this.config = config;
        this.searchApiClient = webClients.getSearchApiClient();
        this.rengineClient = webClients.getRecommendEngineClient();
    }

    public Mono getRecommendationsForSet(String setId, int pageSize, String token) throws RecommendException {
        StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
                .append("?bucket=").append(setId)
                .append("&size=").append(pageSize);
        //String[] recs = getRecommendations(s.toString(), token).block();
        // TODO get recommendations from engine (replace hard-coded ids)
        String[] recommendedIds = new String[]{"/2032004/10877", "/92092/BibliographicResource_1000086018920"};

        String apiKey = TokenUtils.getApiKey(token);
        // TODO most tokens we use have an API key unknown to Search API, so in that case we replace (temporarily)
        if (apiKey.contains("_")) {
            LOG.warn("Unrecognized apikey, replacing with known key...");
        }
        apiKey = dummyApiKey;
        return getSearchApiResponse(recommendedIds, pageSize, apiKey);
    }

    public Mono getRecommendationsForRecord(String recordId, int pageSize, String token) throws RecommendException {
        StringBuilder s = new StringBuilder(config.getREngineRecommendPath())
                .append("?item=").append(recordId)
                .append("&size=").append(pageSize);
        //String[] recommendedIds =  getRecommendations(s.toString(), token).block();
        // TODO get recommendations from engine (replace hard-coded ids)
        String[] recommendedIds = new String[]{recordId};

        String apiKey = TokenUtils.getApiKey(token);
        // TODO most tokens we use have an API key unknown to Search API, so in that case we replace (temporarily)
        if (apiKey.contains("_")) {
            LOG.warn("Unrecognized apikey, replacing with known key...");
        }
        apiKey = dummyApiKey;

        return getSearchApiResponse(recommendedIds, pageSize, apiKey);
    }

    private Mono<String[]> getRecommendations(String recommendQuery, String token) {
        return rengineClient.get()
                .uri(recommendQuery)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(String[].class);
    }

    /**
     * We use reactive (non-blocking) WebClient to retrieve data from Search API
     */
    private Mono<Object> getSearchApiResponse(String[] recordIds, int maxResults, String wskey) {
        if (recordIds == null || recordIds.length == 0) {
            LOG.error("No recommended records");
            return null;
        }
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
        StringBuilder s = new StringBuilder("?query=");
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
