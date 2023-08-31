package eu.europeana.api.recommend.util;

import eu.europeana.api.recommend.exception.MilvusException;
import io.milvus.param.R;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility functions to using Milvus recommendations database.
 *
 * The contents of this file were partially copied from the recommendations-updater project but modified for the
 * purposes here
 *
 * @author Patrick Ehlert
 */
@SuppressWarnings("java:S3740") // Intentionally do not provide generic type for responses
public final class MilvusUtils {

    private MilvusUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Checks the provided response and throws an error if it's not a successful response
     * @param response to check
     * @param errorMsg message in the thrown error
     * @return the original response (if response is success)
     * @throws MilvusException when we did not receive a success response
     */
    public static R checkResponse(R response, String errorMsg) throws MilvusException {
        if (R.Status.Success.getCode() != response.getStatus()) {
            if (StringUtils.isBlank(errorMsg)) {
                errorMsg = response.getMessage();
            } else {
                errorMsg = errorMsg + ": " + response.getMessage();
            }
            throw new MilvusException(errorMsg, response.getException());
        }
        return response;
    }

    /**
     * Checks the provided response and throws an error if it's not a successful response
     * @param response to check
     * @return the original response (if response is success)
     * @throws MilvusException when we did not receive a success response
     */
    public static R checkResponse(R response) throws MilvusException {
        return checkResponse(response, null);
    }



}
