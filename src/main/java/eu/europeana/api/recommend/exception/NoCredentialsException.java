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

    public NoCredentialsException() {
        super("No credentials provided");
    }

    @Override
    public boolean logStacktrace() {
        return false;
    }

}
