package eu.europeana.api.recommend.model;

/**
 * The most important fields we want to check when there's an error response from Search API
 *
 * @author Patrick Ehlert
 * Created on 29 Jul 2020
 */
public class SearchAPIError {

    private String error;
    private String code;

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }
}
