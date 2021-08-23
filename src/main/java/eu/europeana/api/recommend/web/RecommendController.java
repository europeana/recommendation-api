package eu.europeana.api.recommend.web;

import eu.europeana.api.recommend.exception.InvalidRecordIdException;
import eu.europeana.api.recommend.exception.NoCredentialsException;
import eu.europeana.api.recommend.exception.RecommendException;
import eu.europeana.api.recommend.model.SearchAPIEmptyResponse;
import eu.europeana.api.recommend.service.RecommendService;
import eu.europeana.api.recommend.service.TokenUtils;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

/**
 * Controller to handle recommendation requests
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@RestController
@Validated
public class RecommendController {

    private static final Logger LOG = LogManager.getLogger(RecommendController.class);

    private static final String SET_ID_REGEX = "^[0-9]*$";
    private static final String ENTITY_ID_REGEX = "^[0-9]*$";
    private static final String ENTITY_BASE_REGEX = "^(?i)base$";
    private static final String ENTITY_TYPE_REGEX = "^(?i)[agent|concept|place]*$";
    private static final String EUROPEANA_ID_FIELD_REGEX = "^[a-zA-Z0-9_]*$";
    private static final String APIKEY_REGEX = "^[a-zA-Z0-9_]*$";
    private static final String SEED_REGEX = "-?[1-9]\\d*|0";
    private static final String DEFAULT_PAGE_SIZE = "10";
    private static final int MAX_PAGE_SIZE = 50;
    private static final String DEFAULT_PAGE = "0";
    private static final int MAX_PAGE = 40;

    private static final String INVALID_SET_ID_MESSAGE = "Invalid set identifier";
    private static final String INVALID_ENTITY_ID_MESSAGE = "Invalid entity identifier. Id is not a number  ";
    private static final String INVALID_ENTITY_TYPE_MESSAGE = "Invalid entity identifier. Valid types are agent, concept and place";
    private static final String INVALID_ENTITY_BASE_MESSAGE = "Invalid entity identifier. Missing 'base' keyword";
    private static final String INVALID_RECORD_ID_MESSAGE = "Invalid record identifier. Only alpha-numeric characters and underscore are allowed";
    private static final String INCORRECT_PAGE_SIZE = "The page size is not a number between 1 and " + MAX_PAGE_SIZE;
    private static final String INCORRECT_PAGE = "The page value is not a number between 0 and " + MAX_PAGE;
    private static final String INVALID_SEED_MESSAGE = "Invalid seed value. Seed is an Integer, only numbers are allowed";

    private static final String INVALID_APIKEY_MESSAGE = "Invalid API key format";

    private static final java.util.regex.Pattern EUROPEANA_ID = java.util.regex.Pattern.compile("^/[a-zA-Z0-9_]*/[a-zA-Z0-9_]*$");

    private RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping(value = {"/recommend/set/{setId}.json", "/recommend/set/{setId}",
                         "/set/{setId}/recommend.json", "/set/{setId}/recommend" },
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity recommendSet(
            @PathVariable(value = "setId")
                @Pattern(regexp = SET_ID_REGEX, message = INVALID_SET_ID_MESSAGE) String setId,
            @RequestParam(value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "page", required = false, defaultValue = DEFAULT_PAGE)
                @Min(value = 0, message = INCORRECT_PAGE)
                @Max(value = MAX_PAGE, message = INCORRECT_PAGE) int page,
            @RequestParam(value = "seed", required = false)
                @Pattern(regexp = SEED_REGEX, message = INVALID_SEED_MESSAGE) String seed,
            @RequestParam(value = "wskey", required = false)
                @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        String apikey = checkCredentials(authToken, wskey);

        Mono result = recommendService.getRecommendationsForSet(setId, pageSize, page, seed, authToken, apikey);
        if (result == null) {
            return new ResponseEntity(new SearchAPIEmptyResponse(apikey), HttpStatus.OK);
        }

        return new ResponseEntity(result.block(), HttpStatus.OK);
    }

    @PostMapping(value = {"/recommend/set/{setId}.json", "/recommend/set/{setId}",
                          "/set/{setId}/recommend.json", "/set/{setId}/recommend" },
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity acceptSet(
            @PathVariable(value = "setId")
                @Pattern(regexp = SET_ID_REGEX, message = INVALID_SET_ID_MESSAGE) String setId,
            @RequestParam(value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "wskey", required = false)
                @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @Valid @RequestBody String[] ids,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        //String apikey = checkCredentials(authToken, wskey);
        validateRecordIds(ids);

        // TODO for now we simply return the same number of recommendations as received
        return recommendSet(setId, ids.length, 0, null,  wskey, authToken);
    }

    @DeleteMapping(value = {"/recommend/set/{setId}.json", "/recommend/set/{setId}",
                            "/set/{setId}/recommend.json", "/set/{setId}/recommend" },
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity rejectSet(
            @PathVariable(value = "setId")
            @Pattern(regexp = SET_ID_REGEX, message = INVALID_SET_ID_MESSAGE) String setId,
            @RequestParam(value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
            @Min(value = 1, message = INCORRECT_PAGE_SIZE)
            @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "wskey", required = false)
            @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @Valid @RequestBody String[] ids,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        //String apikey = checkCredentials(authToken, wskey);
        validateRecordIds(ids);

        // TODO for now we simply return the same number of recommendations as received
        return recommendSet(setId, ids.length, 0, null,  wskey, authToken);
    }


    @GetMapping(value = {"/recommend/entity/{type}/{base}/{id}.json", "/recommend/entity/{type}/{base}/{id}",
                         "/entity/{type}/{base}/{id}/recommend.json", "/entity/{type}/{base}/{id}/recommend"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity recommendEntity(
            @PathVariable(value = "type")
                @Pattern(regexp = ENTITY_TYPE_REGEX, message = INVALID_ENTITY_TYPE_MESSAGE) String type,
            @PathVariable(value = "base")
                @Pattern(regexp = ENTITY_BASE_REGEX, message = INVALID_ENTITY_BASE_MESSAGE) String base,
            @PathVariable(value = "id")
                @Pattern(regexp = ENTITY_ID_REGEX, message = INVALID_ENTITY_ID_MESSAGE) String id,
            @RequestParam(value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "wskey", required = false)
                @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        String apikey = checkCredentials(authToken, wskey);

        Mono result = recommendService.getRecommendationsForEntity(type, id, pageSize, authToken, apikey);
        if (result == null) {
            return new ResponseEntity(new SearchAPIEmptyResponse(apikey), HttpStatus.OK);
        }

        return new ResponseEntity(result.block(), HttpStatus.OK);
    }

    @PostMapping(value = {"/recommend/entity/{type}/{base}/{id}.json", "/recommend/entity/{type}/{base}/{id}",
                          "/entity/{type}/{base}/{id}/recommend.json", "/entity/{type}/{base}/{id}/recommend"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity acceptEntity(
            @PathVariable(value = "type")
               @Pattern(regexp = ENTITY_TYPE_REGEX, message = INVALID_ENTITY_TYPE_MESSAGE) String type,
            @PathVariable(value = "base")
                @Pattern(regexp = ENTITY_BASE_REGEX, message = INVALID_ENTITY_BASE_MESSAGE) String base,
            @PathVariable(value = "id")
                @Pattern(regexp = ENTITY_ID_REGEX, message = INVALID_ENTITY_ID_MESSAGE) String id,
            @RequestParam(value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "wskey", required = false)
                @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @Valid @RequestBody String[] ids,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        //String apikey = checkCredentials(authToken, wskey);
        validateRecordIds(ids);

        // TODO for now we simply return the same number of recommendations as received
        return recommendEntity(type, base, id, ids.length, wskey, authToken);
    }

    @DeleteMapping(value = {"/recommend/entity/{type}/{base}/{id}.json", "/recommend/entity/{type}/{base}/{id}",
                            "/entity/{type}/{base}/{id}/recommend.json", "/entity/{type}/{base}/{id}/recommend"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity rejectEntity(
            @PathVariable(value = "type")
                @Pattern(regexp = ENTITY_TYPE_REGEX, message = INVALID_ENTITY_TYPE_MESSAGE) String type,
            @PathVariable(value = "base")
                @Pattern(regexp = ENTITY_BASE_REGEX, message = INVALID_ENTITY_BASE_MESSAGE) String base,
            @PathVariable(value = "id")
                @Pattern(regexp = ENTITY_ID_REGEX, message = INVALID_ENTITY_ID_MESSAGE) String id,
            @RequestParam(value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "wskey", required = false)
                @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @Valid @RequestBody String[] ids,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        //String apikey = checkCredentials(authToken, wskey);
        validateRecordIds(ids);

        // TODO for now we simply return the same number of recommendations as received
        return recommendEntity(type, base, id, ids.length, wskey, authToken);
    }

    @GetMapping(value = {"/recommend/record/{datasetId}/{localId}.json", "/recommend/record/{datasetId}/{localId}",
                         "/record/{datasetId}/{localId}/recommend.json", "/record/{datasetId}/{localId}/recommend"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity recommendRecord(
            @PathVariable(value = "datasetId")
                @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String datasetId,
            @PathVariable(value = "localId")
                @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String localId,
            @RequestParam (value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "page", required = false, defaultValue = DEFAULT_PAGE)
                @Min(value = 0, message = INCORRECT_PAGE)
                @Max(value = MAX_PAGE, message = INCORRECT_PAGE) int page,
            @RequestParam(value = "seed", required = false)
                @Pattern(regexp = SEED_REGEX, message = INVALID_SEED_MESSAGE) String seed,
            @RequestParam(value = "wskey", required = false)
                @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        String apikey = checkCredentials(authToken, wskey);

        String recordId = "/" + datasetId + "/" + localId;
        Mono result = recommendService.getRecommendationsForRecord(recordId, pageSize,page, seed, authToken, apikey);
        if (result == null) {
            return new ResponseEntity(new SearchAPIEmptyResponse(apikey), HttpStatus.OK);
        }

        return new ResponseEntity(result.block(), HttpStatus.OK);

    }

    @PostMapping(value = {"/recommend/record/{datasetId}/{localId}.json", "/recommend/record/{datasetId}/{localId}",
                          "/record/{datasetId}/{localId}/recommend.json", "/record/{datasetId}/{localId}/recommend"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity acceptRecord(
            @PathVariable(value = "datasetId")
                @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String datasetId,
            @PathVariable(value = "localId")
                @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String localId,
            @RequestParam (value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "wskey", required = false)
                @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @Valid @RequestBody String[] ids,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        //String apikey = checkCredentials(authToken, wskey);
        validateRecordIds(ids);

        // TODO for now we simply return the same number of recommendations as received
        return recommendRecord(datasetId, localId, ids.length, 0, null, wskey, authToken);
    }

    @DeleteMapping(value = {"/recommend/record/{datasetId}/{localId}.json", "/recommend/record/{datasetId}/{localId}",
                            "/record/{datasetId}/{localId}/recommend.json", "/record/{datasetId}/{localId}/recommend"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity rejectRecord(
            @PathVariable(value = "datasetId")
                @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String datasetId,
            @PathVariable(value = "localId")
                @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String localId,
            @RequestParam (value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestParam(value = "wskey", required = false)
                @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
            @Valid @RequestBody String[] ids,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken)
            throws RecommendException {
        //String apikey = checkCredentials(authToken, wskey);
        validateRecordIds(ids);

        // TODO for now we simply return the same number of recommendations as received
        return recommendRecord(datasetId, localId, ids.length,0, null, wskey, authToken);
    }

    /**
     * Check if we have a token and/or API key. If not we throw an error, otherwise we'll return the API key that we'll
     * use for querying Search API
     */
    private String checkCredentials(String authToken, String wskey) throws RecommendException {
        if (StringUtils.isBlank(wskey) && StringUtils.isBlank(authToken)) {
            throw new NoCredentialsException();
        }
        // if we have a token, we use the API key embedded in that
        String apikey;
        if (StringUtils.isNotBlank(authToken)) {
            apikey = TokenUtils.getApiKey(authToken);
            LOG.debug("Using API key {} from token", apikey);
        } else {
            apikey = wskey;
            LOG.debug("Using received API key {}", apikey);
        }
        return apikey;
    }

    /**
     * Validate the received set of ids in a accept or reject request
     */
    private void validateRecordIds(String [] ids) throws RecommendException {
        if (ids == null || ids.length == 0) {
            throw new InvalidRecordIdException("no ids provided");
        }
        for (String id : ids) {
            if (!isValidRecordId(id)) {
                throw new InvalidRecordIdException(id);
            }
        }
    }

    private boolean isValidRecordId(String id) {
        return EUROPEANA_ID.matcher(id).matches();
    }
}
