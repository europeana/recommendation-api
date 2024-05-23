package eu.europeana.api.recommend.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * For returning an Recommendation responses response when there are no recommendations
 */
public class SearchApiResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = -7906946941888358365L;

    private String apikey = null;
    protected boolean success = true;
    protected int      itemsCount    = 0 ;
    protected int      totalResults  = 0 ;
    @SuppressWarnings("java:S1948") // Jackson will make sure we use a serializable map
    private Map[] items = new LinkedHashMap[0];

    SearchApiResponse() {
        // empty constructor required by Jackson
    }

    /**
     * Initialize a new Search API response with the provided API key
     * @param apikey API key to include in the response
     */
    public SearchApiResponse(String apikey) {
        this.apikey = apikey;
    }

    public String getApikey() {
        return apikey;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public Map[] getItems() {
        return items.clone();
    }

}
