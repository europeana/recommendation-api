package eu.europeana.api.recommend.web;

import eu.europeana.api.recommend.common.RecordId;
import eu.europeana.api.recommend.exception.InvalidRecordIdException;
import eu.europeana.api.recommend.exception.InvalidTokenException;
import eu.europeana.api.recommend.exception.NoCredentialsException;
import eu.europeana.api.recommend.exception.RecommendException;
import eu.europeana.api.recommend.model.SearchApiResponse;
import eu.europeana.api.recommend.service.RecommendService;
import eu.europeana.api.recommend.util.RecommendationConstants;
import eu.europeana.api.recommend.util.RequestUtils;
import eu.europeana.api.recommend.util.TokenUtils;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Controller to handle recommendation requests
 *
 * @author Patrick Ehlert Created on 22 Jul 2020
 */
@RestController
@Validated
public class RecommendController {

  private static final Logger LOG = LogManager.getLogger(RecommendController.class);

  private static final String SET_ID_REGEX = "^\\d*$";
  private static final String ENTITY_ID_REGEX = "^\\d*$";
  private static final String ENTITY_TYPE_REGEX = "^(?i)(agent|concept|place)*$";
  private static final String EUROPEANA_ID_FIELD_REGEX = "^\\w*$";
  private static final String APIKEY_REGEX = "^\\w*$";
  private static final String SEED_REGEX = "-?[1-9]\\d*|0";
  private static final String DEFAULT_PAGE_SIZE = "10";
  private static final int MAX_PAGE_SIZE = 50;
  private static final String DEFAULT_PAGE = "0";
  private static final int MAX_PAGE = 40;

  private static final String INVALID_SET_ID_MESSAGE = "Invalid set identifier";
  private static final String INVALID_ENTITY_ID_MESSAGE = "Invalid entity identifier. Id is not a number  ";
  private static final String INVALID_ENTITY_TYPE_MESSAGE = "Invalid entity identifier. Valid types are agent, concept and place";
  private static final String INVALID_RECORD_ID_MESSAGE = "Invalid record identifier. Only alpha-numeric characters and underscore are allowed";
  private static final String INCORRECT_PAGE_SIZE ="The page size is not a number between 1 and " + MAX_PAGE_SIZE;
  private static final String INCORRECT_PAGE ="The page value is not a number between 0 and " + MAX_PAGE;
  private static final String INVALID_SEED_MESSAGE = "Invalid seed value. Seed is an Integer, only numbers are allowed";

  private static final String INVALID_APIKEY_MESSAGE = "Invalid API key format";

  private static final java.util.regex.Pattern EUROPEANA_ID = java.util.regex.Pattern.compile("^/\\w*/\\w*$");



  private RecommendService recommendService;

  public RecommendController(RecommendService recommendService) {
    this.recommendService = recommendService;
  }

  /**
   * Given a record id, this returns a json response containing basic data about similar records
   *
   * @param datasetId first part of record id for which similar records need to be found
   * @param localId   second part of record id for which similar records need to be found
   * @param pageSize  optional, number of similar records to return, between 1 and 50
   * @param page      optional, extra page of similar records, between 1 and 40
   * @param seed      // TODO not supported yet
   * @param wskey     optional API key
   * @param authToken optional authentication token // TODO not validated yet
   * @return Search API json response with similar records data
   * @throws RecommendException when there's a problem retrieving similar records
   */
  @GetMapping(value = {"/recommend/record/{datasetId}/{localId}.json","/recommend/record/{datasetId}/{localId}",
      "/record/{datasetId}/{localId}/recommend.json", "/record/{datasetId}/{localId}/recommend"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> recommendRecord(
      @PathVariable(value = "datasetId")
      @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String datasetId,
      @PathVariable(value = "localId")
      @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String localId,
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
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String apikey = extractApiKey(authToken, wskey, xApiKey);
    Mono<SearchApiResponse> result = recommendService.getRecommendationsForRecord(new RecordId(datasetId, localId), pageSize, page, seed, apikey, authToken);
    if (result == null) {
      return new ResponseEntity<>(new SearchApiResponse(apikey), HttpStatus.OK);
    }
    return new ResponseEntity<>(result.block(), HttpStatus.OK);
  }

  /**
   * @deprecated
   */
  @Deprecated(since = "Aug 2023")
  @PostMapping(value = {"/recommend/record/{datasetId}/{localId}.json","/recommend/record/{datasetId}/{localId}",
      "/record/{datasetId}/{localId}/recommend.json", "/record/{datasetId}/{localId}/recommend"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> acceptRecord(
      @PathVariable(value = "datasetId")
      @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String datasetId,
      @PathVariable(value = "localId")
      @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String localId,
      @RequestParam(value = "wskey", required = false)
      @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
      @Valid @RequestBody String[] ids,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String userId = extractUserFromToken(authToken);
    validateRecordIds(ids);
    recommendService.submitUserSignals(ids, RecommendationConstants.USER_SIGNAL_ACCEPT, userId,wskey, authToken);
    return recommendRecord(datasetId, localId, ids.length, 0, null, wskey, authToken, xApiKey);
  }

  /**
   * @deprecated
   */
  @Deprecated(since = "Aug 2023")
  @DeleteMapping(value = {"/recommend/record/{datasetId}/{localId}.json","/recommend/record/{datasetId}/{localId}",
      "/record/{datasetId}/{localId}/recommend.json", "/record/{datasetId}/{localId}/recommend"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> rejectRecord(
      @PathVariable(value = "datasetId")
      @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String datasetId,
      @PathVariable(value = "localId")
      @Pattern(regexp = EUROPEANA_ID_FIELD_REGEX, message = INVALID_RECORD_ID_MESSAGE) String localId,
      @RequestParam(value = "wskey", required = false)
      @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
      @Valid @RequestBody String[] ids,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String userId = extractUserFromToken(authToken);
    validateRecordIds(ids);
    recommendService.submitUserSignals(ids, RecommendationConstants.USER_SIGNAL_REJECT, userId,wskey, authToken);
    return recommendRecord(datasetId, localId, ids.length, 0, null, wskey, authToken, xApiKey);
  }

  /**
   * Given a set id, this returns a json response containing basic data about similar records
   *
   * @param setId     id of the set for which we need to find similar items
   * @param pageSize  optional, number of similar records to return, between 1 and 50
   * @param page      optional, extra page of similar records, between 1 and 40
   * @param seed      // TODO not supported yet
   * @param wskey     optional API key
   * @param authToken optional authentication token // TODO not validated yet
   * @return Search API json response with similar records data
   * @throws RecommendException when there's a problem retrieving similar records
   */
  @GetMapping(value = {"/recommend/set/{setId}.json", "/recommend/set/{setId}",
      "/set/{setId}/recommend.json", "/set/{setId}/recommend"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> recommendSet(
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
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String apikey = extractApiKey(authToken, wskey, xApiKey);
    Mono<SearchApiResponse> result = recommendService.getRecommendationsForSet(setId, pageSize,page, seed, apikey, authToken);
    if (result == null) {
      return new ResponseEntity<>(new SearchApiResponse(apikey), HttpStatus.OK);
    }
    return new ResponseEntity<>(result.block(), HttpStatus.OK);
  }

  /**
   * @deprecated
   */
  @Deprecated(since = "Aug 2023")
  @PostMapping(value = {"/recommend/set/{setId}.json", "/recommend/set/{setId}",
      "/set/{setId}/recommend.json", "/set/{setId}/recommend"},
      consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> acceptSet(
      @PathVariable(value = "setId")
      @Pattern(regexp = SET_ID_REGEX, message = INVALID_SET_ID_MESSAGE) String setId,
      @RequestParam(value = "wskey", required = false)
      @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
      @Valid @RequestBody String[] ids,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String userId =  extractUserFromToken(authToken);
    validateRecordIds(ids);
    recommendService.submitUserSignals(ids, RecommendationConstants.USER_SIGNAL_ACCEPT, userId,wskey, authToken);
    return recommendSet(setId, ids.length, 0, null, wskey, authToken, xApiKey);
  }

  /**
   * @deprecated
   */
  @Deprecated(since = "Aug 2023")
  @DeleteMapping(value = {"/recommend/set/{setId}.json", "/recommend/set/{setId}",
      "/set/{setId}/recommend.json", "/set/{setId}/recommend"},
      consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> rejectSet(
      @PathVariable(value = "setId")
      @Pattern(regexp = SET_ID_REGEX, message = INVALID_SET_ID_MESSAGE) String setId,
      @RequestParam(value = "wskey", required = false)
      @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
      @Valid @RequestBody String[] ids,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String userId =  extractUserFromToken(authToken);
    validateRecordIds(ids);
    recommendService.submitUserSignals(ids, RecommendationConstants.USER_SIGNAL_REJECT, userId,wskey, authToken);
    return recommendSet(setId, ids.length, 0, null, wskey, authToken, xApiKey);
  }

  /**
   * Given an entity type and id number, this returns a json response containing basic data about
   * similar records
   *
   * @param type      type of entity, e.g. agent, concept, place
   * @param id        id number of the entity
   * @param pageSize  optional, number of similar records to return, between 1 and 50
   * @param wskey     optional API key
   * @param authToken optional authentication token // TODO not validated yet
   * @return Search API json response with similar records data
   * @throws RecommendException when there's a problem retrieving similar records
   */
  @GetMapping(value = {"/recommend/entity/{type}/{id}.json", "/recommend/entity/{type}/{id}",
      "/entity/{type}/{id}/recommend.json", "/entity/{type}/{id}/recommend"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> recommendEntity(
      @PathVariable(value = "type")
      @Pattern(regexp = ENTITY_TYPE_REGEX, message = INVALID_ENTITY_TYPE_MESSAGE) String type,
      @PathVariable(value = "id")
      @Pattern(regexp = ENTITY_ID_REGEX, message = INVALID_ENTITY_ID_MESSAGE) String id,
      @RequestParam(value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
      @Min(value = 1, message = INCORRECT_PAGE_SIZE)
      @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
      @RequestParam(value = "wskey", required = false)
      @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String apikey = extractApiKey(authToken, wskey, xApiKey);
    Mono<SearchApiResponse> result = recommendService.getRecommendationsForEntity(type,Integer.valueOf(id), pageSize,  apikey, authToken);
    if (result == null) {
      return new ResponseEntity<>(new SearchApiResponse(apikey), HttpStatus.OK);
    }

    return new ResponseEntity<>(result.block(), HttpStatus.OK);
  }

  /**
   * @deprecated
   */
  @Deprecated(since = "Aug 2023")
  @PostMapping(value = {"/recommend/entity/{type}/{base}/{id}.json",
      "/recommend/entity/{type}/{base}/{id}",
      "/entity/{type}/{base}/{id}/recommend.json", "/entity/{type}/{base}/{id}/recommend"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> acceptEntity(
      @PathVariable(value = "type")
      @Pattern(regexp = ENTITY_TYPE_REGEX, message = INVALID_ENTITY_TYPE_MESSAGE) String type,
      @PathVariable(value = "id")
      @Pattern(regexp = ENTITY_ID_REGEX, message = INVALID_ENTITY_ID_MESSAGE) String id,
      @RequestParam(value = "wskey", required = false)
      @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
      @Valid @RequestBody String[] ids,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String userId =  extractUserFromToken(authToken);
    validateRecordIds(ids);
    recommendService.submitUserSignals(ids, RecommendationConstants.USER_SIGNAL_ACCEPT, userId,wskey, authToken);
    return recommendEntity(type, id, ids.length, wskey, authToken, xApiKey);
  }

  /**
   * @deprecated
   */
  @Deprecated(since = "Aug 2023")
  @DeleteMapping(value = {"/recommend/entity/{type}/{base}/{id}.json","/recommend/entity/{type}/{base}/{id}",
      "/entity/{type}/{base}/{id}/recommend.json", "/entity/{type}/{base}/{id}/recommend"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SearchApiResponse> rejectEntity(
      @PathVariable(value = "type")
      @Pattern(regexp = ENTITY_TYPE_REGEX, message = INVALID_ENTITY_TYPE_MESSAGE) String type,
      @PathVariable(value = "id")
      @Pattern(regexp = ENTITY_ID_REGEX, message = INVALID_ENTITY_ID_MESSAGE) String id,
      @RequestParam(value = "wskey", required = false)
      @Pattern(regexp = APIKEY_REGEX, message = INVALID_APIKEY_MESSAGE) String wskey,
      @Valid @RequestBody String[] ids,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authToken,
      @RequestHeader(value = RequestUtils.X_API_KEY_HEADER, required = false) String xApiKey)
      throws RecommendException {
    String userId =  extractUserFromToken(authToken);
    validateRecordIds(ids);
    recommendService.submitUserSignals(ids, RecommendationConstants.USER_SIGNAL_REJECT, userId,
        wskey, authToken);
    return recommendEntity(type, id, ids.length, wskey, authToken, xApiKey);
  }


  /**
   * Check if we have a token and/or API key. If not we throw an error, otherwise we'll return
   * apikey - from the token provided or wskey passed or xApiKey passed. Note that
   * we don't actually do API key validation ourselves. It's done implicitely when reusing API key
   */
  private static String extractApiKey(String authToken, String wskey, String xApiKey)
      throws InvalidTokenException, NoCredentialsException {

    String apiKeyValue;
    if (StringUtils.isNotBlank(authToken)) {
      apiKeyValue = TokenUtils.getApiKey(authToken);
      LOG.debug("Using API key {} from token", apiKeyValue);
      return apiKeyValue;
    }
    if(StringUtils.isNotBlank(xApiKey)){
     apiKeyValue = xApiKey;
     LOG.debug("Using received API key from header x_api_key {}", apiKeyValue);
      return apiKeyValue;
   }
    if(StringUtils.isNotBlank(wskey)) {
      apiKeyValue = wskey;
      LOG.debug("Using received API key {}", apiKeyValue);
      return apiKeyValue;
    }
    throw new NoCredentialsException();
  }

  /**
   * Get the user from the authToken passed.
     Used for accept and reject recommendation methods.
   */
  private static String extractUserFromToken(String authToken)
      throws NoCredentialsException, InvalidTokenException {

        if (StringUtils.isBlank(authToken)) {
      throw new NoCredentialsException("User is not authorised to perform this action");
    }
    String userId = TokenUtils.getUserId(authToken);
    LOG.debug("User {} fetched from the token", userId);
    return userId;
  }

  /**
   * Validate the received set of ids in an accept or reject request
   */
  private void validateRecordIds(String[] ids) throws RecommendException {
    if (ids == null || ids.length == 0) {
      throw new InvalidRecordIdException("No ids provided");
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
