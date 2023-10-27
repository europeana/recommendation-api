package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.common.RecordId;
import eu.europeana.api.recommend.config.WebClients;
import eu.europeana.api.recommend.model.Recommendation;
import eu.europeana.api.recommend.model.SearchApiResponse;
import eu.europeana.api.recommend.util.RequestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service for interacting with the Europeana Search API. The search API is primarily used to
 * <ol>
 * <li>check if the found recommendations exists (Milvus may not be 100% in sync with Search API) and</li>
 * <li>generate the recommendation responses that we sent back</li>
 * </ol>
 * @author Patrick Ehlert
 */
@Service
public class SearchApiService {

    private static final Logger LOG = LogManager.getLogger(SearchApiService.class);

    private static final String SOLR_ID_FIELD = "europeana_id";

    private WebClient webClient;

    @Autowired
    public SearchApiService(WebClients webclients) {
        this.webClient = webclients.getSearchApiClient();
    }

    /**
     * Return true if the record exists, otherwise false
     * @param recordId id of the record to check
     * @param apikey optional, if empty apikey parameter is not included (token should be provided)
     * @param token optional, if empty the apikey parameter is used
     * @return true if the Search API can find the record, otherwise false
     */
    public boolean checkRecordExists(RecordId recordId, String apikey,String token) {
        String query = "search.json?query="
            + SOLR_ID_FIELD + ":\""
            + recordId.getEuropeanaId()
            + '"'
            + "rows=1"
            + "&profile=minimal";

        SearchApiResponse response = webClient.get()
                .uri(query)
                .headers(RequestUtils.generateHeaders(token,apikey))
                .retrieve()
                .bodyToMono(SearchApiResponse.class).block();
        if (response != null) {
            return response.getTotalResults() == 1;
        }
        return false;
    }

    /**
     * Given a set of recommendations, we use a reactive (non-blocking) WebClient to verify with Search API if these
     * records still exist. Also, the response of Search API is used as our final recommendation response.
     * @param recommendations the recommendations we want to return
     * @param maxResults the maximum number of results
     * @param apikey optional, if empty apikey parameter is not included (token should be provided)
     * @param token optional, if empty the apikey parameter is used
     * @return response from Search API
     */
    public Mono<SearchApiResponse> generateResponse(List<Recommendation> recommendations, int maxResults, String apikey,String token) {
        if (recommendations == null || recommendations.isEmpty()) {
            return Mono.just(new SearchApiResponse(apikey));
        }

        String query = this.generateSearchQuery(recommendations, maxResults, apikey);   //Shweta: Impact for search to be verified
        Mono<SearchApiResponse> response = webClient.get()
                .uri(query)
                .headers(RequestUtils.generateHeaders(token,null)) //Shweta: Search APi not supporting API key header - Impact to be verified
                .retrieve()
                .bodyToMono(SearchApiResponse.class);

        if (LOG.isDebugEnabled()) {
            // WARNING since we do a block here when DEBUG is enabled this will cause us to do the request to
            //  Search API twice, once here and once in the controller where the other block() is
            SearchApiResponse searchResponse = response.block();
            if (searchResponse != null) {
                int nrResults = searchResponse.getTotalResults();
                if (nrResults != recommendations.size()) {
                    LOG.warn("{} results from Search API, expected {}", nrResults, recommendations.size());
                } else {
                    LOG.debug("{} results from Search API", nrResults);
                }
            }
        }

        return response;
    }

    /**
     * Constructs a Search API query in the form
     * <pre>query=europeana_id:("/x1/y1" OR "/x2/y2 OR "/x3/y3")&pageSize=3&profile=minimal&wskey=[wskey]</pre>
     * @param recommendations list of recordIds that we want to include in our results (if available)
     * @param maxResults maximum number of results (should match size of the provided recordIds list)
     * @param wskey the apikey to use for this query
     */
    private String generateSearchQuery(List<Recommendation> recommendations, int maxResults, String wskey) {
        StringBuilder s = new StringBuilder().append("search.json?query=");
        for  (int i = 0; i < maxResults && i < recommendations.size(); i++) {
            if (i > 0) {
                s.append(" OR ");
            }
            s.append(SOLR_ID_FIELD).append(":\"")
                    .append(recommendations.get(i).getRecordId().getEuropeanaId())
                    .append('"');
        }
        s.append("&rows=").append(recommendations.size())
                .append("&profile=minimal")
                .append("&wskey=").append(wskey);
        return s.toString();
    }
}
