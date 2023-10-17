package eu.europeana.api.recommend.exception;

/**
 * Exception thrown when there is a problem communicating with Milvus or in the state of the Milvus database
 */
public class MilvusException extends RuntimeException {

    /**
     * Initialise a new milvus exception
     * @param msg error message
     * @param e original exception (cause)
     */
    public MilvusException(String msg, Exception e) {
        super(msg, e);
    }

}
