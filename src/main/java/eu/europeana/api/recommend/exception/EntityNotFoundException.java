package eu.europeana.api.recommend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown entity doesn't exist
 *
 * @author Srishti Singh
 * Created on 13 Sep 2021
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RecommendException{

    public EntityNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }

    public EntityNotFoundException(String msg) {
        super(msg);
    }

    @Override
    public boolean logStacktrace() {
        return false;
    }
}
