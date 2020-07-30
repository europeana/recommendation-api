package eu.europeana.api.recommend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown when we receive a token we can't decode
 *
 * @author Patrick Ehlert
 * Created on 29 Jul 2020
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTokenException extends RecommendException {

    public InvalidTokenException(String msg, Throwable t) {
        super(msg, t);
    }

    public InvalidTokenException(String msg) {
        super(msg);
    }

    @Override
    public boolean logStacktrace() {
        return false;
    }

}
