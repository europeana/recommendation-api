package eu.europeana.api.recommend.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;

/**
 * Base error class for this application
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
public class RecommendException extends EuropeanaApiException {

    public RecommendException(String msg, Throwable t) {
        super(msg, t);
    }

    public RecommendException(String msg) {
        super(msg);
    }

    /**
     * @return boolean indicating whether this type of exception should be logged or not
     */
    public boolean doLog() {
        return true; // default we log all exceptions
    }

    /**
     * @return boolean indicating whether we should include the stacktrace in the logs (if doLog is enabled)
     */
    public boolean logStacktrace() {
        return true;
    }

}
