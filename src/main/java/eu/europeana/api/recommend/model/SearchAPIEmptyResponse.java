package eu.europeana.api.recommend.model;

/**
 * For returning an empty response when there are no recommendations
 */
public class SearchAPIEmptyResponse {

    private String   apiKey;
    private boolean  success       = true;
    private long     requestNumber = 999L;
    private int      itemsCount    = 0 ;
    private int      totalResults  = 0 ;
    private String[] items         = new String[]{};

    public SearchAPIEmptyResponse(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getRequestNumber() {
        return requestNumber;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public String[] getItems() {
        return items;
    }

}
