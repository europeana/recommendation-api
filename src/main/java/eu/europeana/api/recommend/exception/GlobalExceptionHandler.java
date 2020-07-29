package eu.europeana.api.recommend.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
                LOG.error("Caught exception: "+ e.getMessage());
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



}
