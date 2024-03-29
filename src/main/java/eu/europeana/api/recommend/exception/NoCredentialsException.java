package eu.europeana.api.recommend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown when no API key and no authorization token was received
 *
 * @author Patrick Ehlert
 * Created on 25 aug 2020
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class NoCredentialsException extends RecommendException {

    public static final String ERROR_MSG = "No credentials provided. ";

    public NoCredentialsException() {
        super(ERROR_MSG);
    }

    public NoCredentialsException(String msg) {
        super(msg);
    }

    @Override
    public boolean logStacktrace() {
        return false;
    }

}
