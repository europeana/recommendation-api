package eu.europeana.api.recommend.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.model.SearchAPIError;
import io.micrometer.core.instrument.util.StringEscapeUtils;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.io.IOException;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    private static final String FAILING_RECORD_API = "Record API";
    private static final String FAILING_SET_API = "Set API";
    private static final String FAILING_ENTITY_API = "Entity API";
    private static final String FAILING_RECOMMENDATION_ENGINE = "Recommendation engine";

    private static final ObjectMapper JSON_ERROR_TO_OBJECT = new ObjectMapper();
    static {
        JSON_ERROR_TO_OBJECT.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private RecommendSettings config;

    public GlobalExceptionHandler(RecommendSettings config) {
        this.config = config;
    }

    /**
     * Checks if we should log an error and rethrows it
     * @param e caught exception
     * @throws RecommendException rethrown exception
     */
    @ExceptionHandler(RecommendException.class)
    public void handleBaseException(RecommendException e) throws RecommendException {
        if (e.doLog()) {
            if (e.logStacktrace()) {
                LOG.error("Caught exception", e);
            } else {
                LOG.error("Caught exception: {}", e.getMessage());
            }
        }
        throw e;
    }

    /**
     * Make sure we return 401 instead of 400 when there's no authorization header
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
     * Make sure we return 400 instead of 500 when input validation fails
     * @param e
     * @param response
     * @throws IOException
     */
    @ExceptionHandler
    @SuppressWarnings("findsecbugs:XSS_SERVLET") // we control error message and use StringEscapeUtils so very low risk
    public void handleInputValidationError(ConstraintViolationException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), StringEscapeUtils.escapeJson(e.getMessage()));
    }

    /**
     * Handle all exceptions from backend systems
     * @param ex
     * @return
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
                    SearchAPIError searchApiError = JSON_ERROR_TO_OBJECT.readValue(ex.getResponseBodyAsString(), SearchAPIError.class);
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
            } else if (request.startsWith(config.getREngineHost())) {
                result = FAILING_RECOMMENDATION_ENGINE;
            }
        }
        return result;
    }

    private String filterOutSensitiveInformation(String originalMessage) {
        String result = originalMessage.replaceAll(config.getREngineHost(), "<ENGINE_HOST>");
        result = result.replaceAll(config.getSearchApiEndpoint(), "<SEARCH_API_ENDPOINT>");
        return result;
    }

}
