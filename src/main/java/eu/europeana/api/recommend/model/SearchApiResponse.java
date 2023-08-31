package eu.europeana.api.recommend.model;

import java.util.Map;

/**
 * For returning an Recommendation responses response when there are no recommendations
 */
public class SearchApiResponse {

    private String apikey = null;

    protected boolean success = true;
    private int      itemsCount    = 0 ;
    private int      totalResults  = 0 ;
    private Map[] items         = new Map[0];

    SearchApiResponse() {
        // empty constructor required by Jackson
    }

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
