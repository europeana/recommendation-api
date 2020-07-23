package eu.europeana.api.recommend.exception;

/**
 * Base error class for this application
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
public class RecommendException extends Exception {

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

}
