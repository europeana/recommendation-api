package eu.europeana.api.recommend;

import eu.europeana.api.recommend.exception.RecommendException;
import eu.europeana.api.recommend.web.RecommendController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test to see if we can send and retrieve requests to the recommendation engine properly
 * Note that this will only work if an recommendation engine endpoint is defined in the properties
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RecommendEngineIT {

    //TODO At the moment only record recommendation tests will work as that will not check the token or key
    // For sets the engine sends a request to Search API, so it should have a proper key or token
    // So we should actually inject a proper key and token here
    private static final String TEST_API_KEY = RecommendControllerTest.TOKEN_API_KEY;
    private static final String TEST_TOKEN = RecommendControllerTest.TOKEN;

    @Autowired
    RecommendController recommendController;

    @Test
    public void testGetSetRecommendationsWithToken() throws RecommendException {
        // we use setId 0 so we expect 0 results (and no request to Search API should be fired)
        ResponseEntity response = recommendController.recommendSet("0", 4, 0, null,null, TEST_TOKEN);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetSetRecommendationsWithPageSeed() throws RecommendException {
        // we use setId 0 so we expect 0 results (and no request to Search API should be fired)
        ResponseEntity response = recommendController.recommendSet("0", 4, 3, "-35425",null, TEST_TOKEN);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetSetRecommendationsWithApiKey() throws RecommendException {
        // we use setId 0 so we expect 0 results (and no request to Search API should be fired)
        ResponseEntity response = recommendController.recommendSet("0", 4, 0,null,TEST_API_KEY, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetRecordRecommendationsWithToken() throws RecommendException {
        // we use a non-existing record so we expect 0 results (and no request to Search API should be fired)
        ResponseEntity response = recommendController.recommendRecord("x", "y", 4, 0,null,null, TEST_TOKEN);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetRecordRecommendationsWithPageSeed() throws RecommendException {
        // we use a non-existing record so we expect 0 results (and no request to Search API should be fired)
        ResponseEntity response = recommendController.recommendRecord("x", "y", 4, 3, "3563",null, TEST_TOKEN);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetRecordRecommendationsWithApiKey() throws RecommendException {
        // we use a non-existing record so we expect 0 results (and no request to Search API should be fired)
        ResponseEntity response = recommendController.recommendRecord("x", "y", 4, 0,null,TEST_API_KEY, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


}
