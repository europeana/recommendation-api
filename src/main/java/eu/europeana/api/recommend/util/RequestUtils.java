package eu.europeana.api.recommend.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.function.Consumer;

/**
 * Utility class for sending requests to our APIs
 */
public final class RequestUtils {


    public static final String X_API_KEY_HEADER = "X-Api-Key";
    private RequestUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Generates headers for sending requests to any of our APIs
     * @param token optional if provided its sent in an Authorization header
     * @return headers to sent to an Europeana API
     */
    public static Consumer<HttpHeaders> generateHeaders(String token,String apikey) {
        return httpHeaders -> {
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            if (StringUtils.isNotBlank(token)) {
                httpHeaders.set(HttpHeaders.AUTHORIZATION, token);
            }
            if (StringUtils.isNotBlank(apikey)) {
                httpHeaders.set(X_API_KEY_HEADER, apikey);
            }
        };
    }



}
