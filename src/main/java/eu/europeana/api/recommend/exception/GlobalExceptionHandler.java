package eu.europeana.api.recommend.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.recommend.model.SearchAPIError;
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

    private static final ObjectMapper JSON_ERROR_TO_OBJECT = new ObjectMapper();
    static {
        JSON_ERROR_TO_OBJECT.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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
    public void handleMissingAuthHeader(MissingRequestHeaderException e, HttpServletResponse response) throws IOException {
        if ("Authorization".equalsIgnoreCase(e.getHeaderName())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), e.getMessage());
        } else {
            response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    // TODO figure out why message is empty?

    /**
     * Make sure we return 400 instead of 500 when input validation fails
     * @param e
     * @param response
     * @throws IOException
     */
    @ExceptionHandler
    public void handleInputValidationError(ConstraintViolationException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    /**
     * Handle all exceptions from backend systems
     * @param ex
     * @return
     */
    @ExceptionHandler(WebClientResponseException.class)
    public void handleWebClientResponseException(WebClientResponseException ex, HttpServletResponse response) throws IOException {
        String errorMsg = ex.getResponseBodyAsString();
        LOG.error("Error from backend: {} - {}, {}", ex.getRawStatusCode(), ex.getStatusText(), errorMsg);

        // TODO Recommendation engine may not return json, check if this works for error message they send.
        try {
            SearchAPIError sApiError = JSON_ERROR_TO_OBJECT.readValue(errorMsg, SearchAPIError.class);
            errorMsg = sApiError.getError();
        } catch (JsonProcessingException e) {
            LOG.warn("Cannot deserialize error message from backend system: {}", errorMsg, e);
        }

        // we simply relay all other error messages
        if (ex.getRawStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            response.sendError(HttpStatus.BAD_GATEWAY.value(), errorMsg);
        }
        response.sendError(ex.getRawStatusCode(), errorMsg);
    }

}
