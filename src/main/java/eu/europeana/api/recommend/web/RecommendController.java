package eu.europeana.api.recommend.web;

import eu.europeana.api.recommend.service.RecommendService;
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
            @PathVariable @Pattern(regexp = SET_ID_REGEX, message = INVALID_SETID_MESSAGE) String setId,
            @RequestParam (required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestHeader(value = "Authorization") String authorization) {

        Mono result = recommendService.getRecommendationsForSet(setId, pageSize, authorization);

        // TODO how to respond if we get no recommendations?
        if (result == null) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity(result.block(), HttpStatus.OK);

    }

    @GetMapping(value = {"/record/{datasetId}/{localId}.json", "/record/{datasetId}/{localId}"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity recommendRecord(
            @PathVariable @Pattern(regexp = EUROPEANA_ID_REGEX, message = INVALID_RECORDID_MESSAGE) String datasetId,
            @PathVariable @Pattern(regexp = EUROPEANA_ID_REGEX, message = INVALID_RECORDID_MESSAGE) String localId,
            @RequestParam (required = false, defaultValue = DEFAULT_PAGE_SIZE)
                @Min(value = 1, message = INCORRECT_PAGE_SIZE)
                @Max(value = MAX_PAGE_SIZE, message = INCORRECT_PAGE_SIZE) int pageSize,
            @RequestHeader(value = "Authorization") String authorization) {

        String recordId = "/" + datasetId + "/" + localId;
        Mono result = recommendService.getRecommendationsForRecord(recordId, pageSize, authorization);

        // TODO how to respond if we get no recommendations?
        if (result == null) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity(result.block(), HttpStatus.OK);

    }
}
