package eu.europeana.api.recommend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown when the entity in an entity recommendation request cannot be found
 **/
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RecommendException {

    public EntityNotFoundException(String msg) {
        super(msg);
    }

    @Override
    public boolean doLog() {
        return false;
    }

    @Override
    public boolean logStacktrace() {
        return false;
    }
}
