package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.config.WebClients;
import eu.europeana.api.recommend.model.Set;
import eu.europeana.api.recommend.model.SetSearch;
import eu.europeana.api.recommend.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


/**
 * Service for interacting with Set API
 */
@Service
public class SetApiService {

    private static final int MAX_SET_ITEMS = 100;

    private WebClient webClient;

    @Autowired
    public SetApiService(WebClients webclients) {
        this.webClient = webclients.getSetApiClient();
    }

    /**
     * Return the relevant set data for doing a set recommendation based on its metadata
     * 2024-09-16: PE Note that as part of ticket EA-3933 and changes to Set API we no longer return the items in a set!
     *
     * @param setId the id of the set to retrieve
     * @param apikey optional, if empty apikey  is not included (authToken should be provided)
     * @param token optional, if empty the apikey parameter is used
     * @return set object
     */
    public Mono<Set> getSetData(String setId, String apikey, String token) {
        String query = setId;
            //+ "?pageSize=" + MAX_SET_ITEMS
            //+ "&profile=standard;    // No longer possible to sent with new Set API, instead we retrieve only title and description
        return this.webClient.get()
                .uri(query)
                .headers(RequestUtils.generateHeaders(token,apikey))
                .retrieve()
                .bodyToMono(Set.class);
    }


    /**
     * Generates a Set API search query to fetch entity set items for the given entity uri.
     * example : ?query=type:EntityBestItemsSet&qf=subject:<entityUri>&pageSize=<pageSize>&profile=standard&wskey=
     * @param entityUri uri of the entity for which we want set items
     * @param apikey optional, if empty apikey header is not included (authToken should be provided)
     * @param token optional, if empty the apikey parameter is used
     * @return setsearch object
     */
    public Mono<SetSearch> getSetDataForEntity(String entityUri, String apikey, String token) {
        StringBuilder query = new StringBuilder("search.json?query=type:EntityBestItemsSet");
        query.append("&qf=subject:").append(entityUri);
        query.append("&pageSize=").append(MAX_SET_ITEMS);
        query.append("&profile=standard");

        return this.webClient.get()
                .uri(query.toString())
                .headers(RequestUtils.generateHeaders(token,apikey))
                .retrieve()
                .bodyToMono(SetSearch.class);
    }



    /**
     * Check if the returned set is an open set (based on a query) of closed (with items).
     * @param s set to check
     * @return true if the set is an open set, otherwise false
     */
    public boolean isOpenSet(Set s) {
        return StringUtils.isNotBlank(s.getIsDefinedBy());
    }

}
