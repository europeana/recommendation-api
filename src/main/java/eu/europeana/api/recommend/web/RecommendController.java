package eu.europeana.api.recommend.web;

import eu.europeana.api.recommend.exception.RecommendException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Pattern;

/**
 * Controller to handle recommendation requests
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@RestController
@RequestMapping("/recommend")
@Validated
public class RecommendController {

    private static final String SET_ID_REGEX = "^[0-9]*$";
    private static final String EUROPEANA_ID_REGEX = "^[a-zA-Z0-9_]*$";

    private static final String INVALID_SETID_MESSAGE = "Invalid set identifier";
    private static final String INVALID_RECORDID_MESSAGE = "Invalid record identifier. Only alpha-numeric characters and underscore are allowed";


    @GetMapping(value = {"/set/{setId}.json", "/set/{setId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> recommendSet(
            @PathVariable @Pattern(regexp = SET_ID_REGEX, message = INVALID_SETID_MESSAGE) String setId,
            @RequestParam (required = false, defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String authorization,
            HttpServletRequest request,
            HttpServletResponse response) throws RecommendException {

        return null;
    }

    @GetMapping(value = {"/record/{datasetId}/{localId}.json", "/record/{datasetId}/{localId}"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> recommendRecord(
            @PathVariable @Pattern(regexp = EUROPEANA_ID_REGEX, message = INVALID_RECORDID_MESSAGE) String datasetId,
            @PathVariable @Pattern(regexp = EUROPEANA_ID_REGEX, message = INVALID_RECORDID_MESSAGE) String localId,
            @RequestParam (required = false, defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String authorization,
            HttpServletRequest request,
            HttpServletResponse response) throws RecommendException {

        return null;
    }
}
