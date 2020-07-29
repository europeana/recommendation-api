package eu.europeana.api.recommend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown when there's a problem retrieving data from Search API or the recommendation engine
 *
 * @author Patrick Ehlert
 * Created on 29 Jul 2020
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RetrieveException extends RecommendException {

    public RetrieveException(Integer errorCode) {
        super("Error retrieving data from the backend (" + errorCode + " response)");
    }

    @Override
    public boolean logStacktrace() {
        return false;
    }

}
