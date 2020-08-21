package eu.europeana.api.recommend.web;

import eu.europeana.api.recommend.exception.RecommendException;
import eu.europeana.api.recommend.model.SearchAPIEmptyResponse;
import eu.europeana.api.recommend.service.RecommendService;
import eu.europeana.api.recommend.service.TokenUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
@RequestMapping("/recommend")
@Validated
public class RecommendController {

    private static final String SET_ID_REGEX = "^[0-9]*$";
    private static final String EUROPEANA_ID_REGEX = "^[a-zA-Z0-9_]*$";
    private static final String DEFAULT_PAGE_SIZE = "10";
    private static final int MAX_PAGE_SIZE = 50;

    private static final String INVALID_SETID_MESSAGE = "Invalid set identifier";
    private static final String INVALID_RECORDID_MESSAGE = "Invalid record identifier. Only alpha-numeric characters and underscore are allowed";
    private static final String INCORRECT_PAGE_SIZE = "The page size is not a number between 1 and " + MAX_PAGE_SIZE;

    private RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping(value = {"/set/{setId}.json", "/set/{setId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity recommendSet(
            @PathVariable (value = "setId") @Pattern(regexp = SET_ID_REGEX, message = INVALID_SETID_MESSAGE) String setId,
            @RequestParam (value = "pageSize" , required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization)
            throws RecommendException {

        Mono result = recommendService.getRecommendationsForSet(setId, pageSize, authorization);

        if (result == null) {
            return new ResponseEntity(new SearchAPIEmptyResponse(TokenUtils.getApiKey(authorization)), HttpStatus.OK);
        }

        return new ResponseEntity(result.block(), HttpStatus.OK);

    }

    @GetMapping(value = {"/record/{datasetId}/{localId}.json", "/record/{datasetId}/{localId}"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity recommendRecord(
            @PathVariable (value = "datasetId") @Pattern(regexp = EUROPEANA_ID_REGEX, message = INVALID_RECORDID_MESSAGE) String datasetId,
            @PathVariable (value = "localId")@Pattern(regexp = EUROPEANA_ID_REGEX, message = INVALID_RECORDID_MESSAGE) String localId,
            @RequestParam (value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization)
            throws RecommendException {

        String recordId = "/" + datasetId + "/" + localId;
        Mono result = recommendService.getRecommendationsForRecord(recordId, pageSize, authorization);

        if (result == null) {
            return new ResponseEntity(new SearchAPIEmptyResponse(TokenUtils.getApiKey(authorization)), HttpStatus.OK);
        }

        return new ResponseEntity(result.block(), HttpStatus.OK);

    }
}
