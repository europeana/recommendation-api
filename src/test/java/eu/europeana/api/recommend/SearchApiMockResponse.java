package eu.europeana.api.recommend;


import eu.europeana.api.recommend.model.SearchApiResponse;

/**
 * So we can generate our own Search API responses for tests
 */
public class SearchApiMockResponse extends SearchApiResponse {
    public SearchApiMockResponse(String apikey, int itemsCount, int totalResults) {
        super(apikey);
        this.itemsCount = itemsCount;
        this.totalResults = totalResults;
    }
}