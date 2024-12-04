package eu.europeana.api.recommend.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.commons_sb3.error.EuropeanaGlobalExceptionHandler;
import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.model.SearchApiError;
import io.micrometer.core.instrument.util.StringEscapeUtils;
import io.micrometer.core.instrument.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@ControllerAdvice
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    private static final String FAILING_RECORD_API = "Record API";
    private static final String FAILING_SET_API = "Set API";
    private static final String FAILING_ENTITY_API = "Entity API";
    private static final String FAILING_EMBEDDINGS_API = "Embeddings API";

    private static final ObjectMapper JSON_ERROR_TO_OBJECT = new ObjectMapper();
    static {
        JSON_ERROR_TO_OBJECT.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private RecommendSettings config;

    /**
     * Initialize new global exception handler
     * @param config application configuration
     */
    public GlobalExceptionHandler(RecommendSettings config) {
        this.config = config;
    }

    /**
     * Make sure we return 401 instead of 400 when there's no authorization header
     * @param e caught {@link MissingRequestHeaderException}
     * @param response the response of the failing request
     * @throws IOException if there's an error sending back the response
     */
    @ExceptionHandler
    @SuppressWarnings("findsecbugs:XSS_SERVLET") // we control error message and use StringEscapeUtils so very low risk
    public void handleMissingAuthHeader(MissingRequestHeaderException e, HttpServletResponse response) throws IOException {
        if ("Authorization".equalsIgnoreCase(e.getHeaderName())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(),StringEscapeUtils.escapeJson(e.getMessage()));
        } else {
            response.sendError(HttpStatus.BAD_REQUEST.value(), StringEscapeUtils.escapeJson(e.getMessage()));
        }
    }

    /**
     * Make sure we return 502 instead of 500 when there's a problem with Milvus
     * @param e caught {@link MilvusException}
     * @param response the response of the failing request
     * @throws IOException if there's an error sending back the response
     */
    @ExceptionHandler
    public void handleMilvusException(MilvusException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_GATEWAY.value(), StringEscapeUtils.escapeJson(e.getMessage()));
    }

    /**
     * Handle all exceptions from API backend systems (WebClient calls) and return 502 response instead
     * @param ex caught {@link WebClientResponseException}
     * @param response the response of the failing request
     * @throws IOException if there's an error sending back the response
     */
    @ExceptionHandler(WebClientResponseException.class)
    @SuppressWarnings("findsecbugs:XSS_SERVLET") // we control error message and use StringEscapeUtils so very low risk
    public void handleWebClientResponseException(WebClientResponseException ex, HttpServletResponse response) throws IOException {
        LOG.error("Error from backend: {} {} (Message = {})", ex.getRawStatusCode(), ex.getStatusText(), ex.getMessage());

        String failingApi = getFailingAPI(ex);
        String errorMsg = "Error from " + failingApi + ": ";
        // Check if error was from Search API or other system because we parse that error to get more detailed info
        if (FAILING_RECORD_API.equals(failingApi)) {
            // Decode Search API message if available
            if (StringUtils.isNotBlank(ex.getResponseBodyAsString())) {
                try {
                    SearchApiError searchApiError = JSON_ERROR_TO_OBJECT.readValue(ex.getResponseBodyAsString(), SearchApiError.class);
                    errorMsg = errorMsg + searchApiError.getError();
                } catch (JsonProcessingException e) {
                    LOG.warn("Cannot deserialize error response from Search API: {}", ex.getResponseBodyAsString(), e);
                    errorMsg = errorMsg + ex.getMessage();
                }
            } else {
                errorMsg = errorMsg + ex.getMessage();
            }
        } else {
            errorMsg = errorMsg + ex.getMessage();
        }

        // For all 500 responses we return a 502 ourselves
        if (ex.getRawStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            response.sendError(HttpStatus.BAD_GATEWAY.value(),
                    StringEscapeUtils.escapeJson(filterOutSensitiveInformation(errorMsg)));
        } else {
            // For all other error responses we simply relay the status code and errormessage
            response.sendError(ex.getRawStatusCode(),
                    StringEscapeUtils.escapeJson(filterOutSensitiveInformation(errorMsg)));
        }
    }

    @SuppressWarnings("java:S2259") // we get a false possible nullpointer warning in SQ here
    private String getFailingAPI(WebClientResponseException ex) {
        String result = "unknown service";
        if (ex != null && ex.getRequest() != null) {
            String request = ex.getRequest().getURI().toString();
            if (request.startsWith(config.getSearchApiEndpoint())) {
                result = FAILING_RECORD_API;
            } else if (request.startsWith(config.getEntityApiEndpoint())) {
                result = FAILING_ENTITY_API;
            } else if (request.startsWith(config.getSetApiEndpoint())) {
                result = FAILING_SET_API;
            } else if (request.startsWith(config.getEmbeddingsApiEndpoint())) {
                result = FAILING_EMBEDDINGS_API;
            }
        }
        return result;
    }

    private String filterOutSensitiveInformation(String originalMessage) {
        String result = originalMessage.replaceAll(config.getMilvusHostName(), "<DATABASE_HOST>");
        result = result.replaceAll(config.getSearchApiEndpoint(), "<SEARCH_API_ENDPOINT>");
        result = result.replaceAll(config.getSetApiEndpoint(), "<SET_API_ENDPOINT>");
        result = result.replaceAll(config.getEntityApiEndpoint(), "<ENTITY_API_ENDPOINT>");
        result = result.replaceAll(config.getEmbeddingsApiEndpoint(), "<EMBEDDINGS_API_ENDPOINT>");
        return result;
    }

}
