package eu.europeana.api.recommend.util;

import eu.europeana.api.recommend.exception.MilvusException;
import io.milvus.param.R;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Embeddings API returns vectors as a List<Double>, but Milvus currently only accepts searches with List<Float>
     * so we use this method to convert.
     * Milvus v2.4 is expected to support List<Double> (see also https://github.com/milvus-io/milvus/discussions/18094)
     * so eventually this method should become obsolete
     * @param listToConvert list of double
     * @return list of float
     */
    public static List<Float> convertToFloatList(List<Double> listToConvert) {
        return listToConvert.stream().map(Double::floatValue).collect(Collectors.toList());
    }

}
