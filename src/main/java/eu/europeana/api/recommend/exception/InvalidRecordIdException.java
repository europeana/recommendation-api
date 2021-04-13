package eu.europeana.api.recommend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown when we receive ids in an accept or reject request that are invalid
 *
 * @author Patrick Ehlert
 * Created on 9 April 20201
 * */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRecordIdException extends RecommendException {

    private static final String ERROR_MSG = "Invalid item id: ";

    public InvalidRecordIdException(String invalidId) {
        super(ERROR_MSG + invalidId);
    }

    @Override
    public boolean logStacktrace() {
        return false;
    }

}
