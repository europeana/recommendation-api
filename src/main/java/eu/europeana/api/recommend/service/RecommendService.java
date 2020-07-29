package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.config.WebClients;
import eu.europeana.api.recommend.exception.RecommendException;
import eu.europeana.api.recommend.exception.RetrieveException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
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

    private static final String TMP_WSKEY_FROM_TOKEN = "[REMOVED]";
    private static final String SOLR_ID_FIELD = "europeana_id";

    private WebClient searchApiClient;
    private WebClient rengineClient;

    public RecommendService(WebClients webClients) {
        searchApiClient = webClients.getSearchApiClient();
        rengineClient = webClients.getRecommendEngineClient();
    }

    public Mono getRecommendationsForSet(String setId, int pageSize, String token) {
        // TODO get apikey from token
        // TODO get recommendations from engine (and replace hard-coded ids)
        String[] recommendedIds = new String[]{"/2032004/10877", "/92092/BibliographicResource_1000086018920"};

        return getSearchApiResponse(recommendedIds, pageSize, TMP_WSKEY_FROM_TOKEN);
    }

    public Mono getRecommendationsForRecord(String recordId, int pageSize, String token) {
        // TODO get apikey from token
        // TODO get recommendations from engine (and replace hard-coded ids)
        String[] recommendedIds = null;

        return getSearchApiResponse(recommendedIds, pageSize, TMP_WSKEY_FROM_TOKEN);
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
        LOG.info("Sending search query {}", query);
        return searchApiClient.get()
                .uri(query)
                .retrieve()
                .onStatus((HttpStatus::isError), (response -> handleError(response))
                )
                .bodyToMono(Object.class);
    }

    // TODO get error message from Search API error response
    // TODO different error for 401 or 500 responses?
    private Mono<? extends RecommendException> handleError(ClientResponse response) {
        return Mono.error(new RetrieveException(response.rawStatusCode()));
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
