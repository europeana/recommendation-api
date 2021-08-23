package eu.europeana.api.recommend;

import eu.europeana.api.recommend.model.SearchAPIEmptyResponse;
import eu.europeana.api.recommend.service.RecommendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test for testing the RecommendController class
 */
@SpringBootTest
@AutoConfigureMockMvc
public class RecommendControllerTest {

    public static final String AUTH_HEADER     = "Authorization";
    public static final String TOKEN           = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYXpwIjoidGVzdF9rZXkiLCJqdGkiOiJiODM4MDM3Ni1mYThhLTQxN2ItODg0NC0xZTQ4ZjBlNDkyNjkiLCJpYXQiOjE1OTg0NTAwNzIsImV4cCI6MTU5ODQ1MzY3Mn0.in-NrpzLE4NVptQJUbFzEeWUDMpZShlad3GRIxgUlVk";
    public static final String TOKEN_API_KEY   = "test_key"; // this key is encoded in the token

    private static final String WSKEY_PARAM     = "wskey";
    private static final String WSKEY_VALUE     = "anotherTestKey";
    private static final String PAGE_PARAM     = "page";
    private static final String SEED_PARAM     = "seed";



    @MockBean
    private RecommendService recommendService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testInvalidSetId() throws Exception {
        mockMvc.perform(get("/recommend/set/{setId}",
                "SetIdShouldBeNumberic")
                .header(AUTH_HEADER, TOKEN))
                .andExpect(status().is(400));
    }

    @Test
    public void testInvalidRecordID() throws Exception {
        mockMvc.perform(get("/recommend/record/{datasetId}/{localId}.json",
                "ValidSetId", "But(Invalid)LocalId$")
                .header(AUTH_HEADER, TOKEN))
                .andExpect(status().is(400));
    }

    /**
     * Test if we handle invalid (unparseable) tokens properly
     */
    @Test
    public void testInvalidToken() throws Exception {
        mockMvc.perform(get("/recommend/record/{datasetId}/{localId}.json",
                "92092", "BibliographicResource_1000086018920")
                .header(AUTH_HEADER, "invalidToken"))
                .andExpect(status().is(400));
    }

    @Test
    public void testNoAuthorizationSet() throws Exception {
        mockMvc.perform(get("/recommend/set/{setId}", "2"))
                .andExpect(status().is(401));
    }

    @Test
    public void testNoAuthorizationRecord() throws Exception {
        mockMvc.perform(get("/recommend/record/{datasetId}/{localId}.json",
                "92092", "BibliographicResource_1000086018920"))
                .andExpect(status().is(401));
    }

    /**
     * Test the empty response for sets recommendation with valid input, as well as getting the apikey from a token
     */
    @Test
    public void testEmptyResponseSetTokenOnly() throws Exception {
        SearchAPIEmptyResponse expected = new SearchAPIEmptyResponse(TOKEN_API_KEY);

        ResultActions result = mockMvc.perform(get("/recommend/set/{setId}", "2")
                .header(AUTH_HEADER, TOKEN))
                .andDo(print());
        checkValidEmtpyResponse(expected, result);
    }

    /**
     * Test the empty response for sets recommendation with valid input, as well as getting the apikey from a token
     */
    @Test
    public void testEmptyResponseSetWithPageSeedTokenOnly() throws Exception {
        SearchAPIEmptyResponse expected = new SearchAPIEmptyResponse(TOKEN_API_KEY);

        ResultActions result = mockMvc.perform(get("/recommend/set/{setId}", "2")
                        .header(AUTH_HEADER, TOKEN)
                        .param(PAGE_PARAM, "5")
                        .param(SEED_PARAM, String.valueOf(new Random().nextInt())))
                .andDo(print());
        checkValidEmtpyResponse(expected, result);
    }

    /**
     * Test the invalid seed value
     */
    @Test
    public void testSetRecommendWithInvalidSeed() throws Exception {
         ResultActions result = mockMvc.perform(get("/recommend/set/{setId}", "2")
                        .header(AUTH_HEADER, TOKEN)
                        .param(PAGE_PARAM, "5")
                        .param(SEED_PARAM, "test1234"))
                .andExpect(status().is(400));
    }


    /**
     * Test the empty response for record recommendation with valid input, as well as getting the apikey from a token (when both
     * token and wskey are provided)
     */
    @Test
    public void testEmptyResponseRecordTokenAndKey() throws Exception {
        SearchAPIEmptyResponse expected = new SearchAPIEmptyResponse(TOKEN_API_KEY);

        ResultActions result = mockMvc.perform(get("/recommend/set/{setId}", "2")
                .header(AUTH_HEADER, TOKEN)
                .param(WSKEY_PARAM, WSKEY_VALUE))
                .andDo(print());
        checkValidEmtpyResponse(expected, result);
    }

    /**
     * Test the empty response for record recommendation, using an apikey only
     */
    @Test
    public void testEmptyResponseRecordKeyOnly() throws Exception {
        SearchAPIEmptyResponse expected = new SearchAPIEmptyResponse(WSKEY_VALUE );

        ResultActions result = mockMvc.perform(get("/recommend/record/{datasetId}/{localId}.json",
                "92092", "BibliographicResource_1000086018920")
                .param(WSKEY_PARAM, WSKEY_VALUE))
                .andDo(print());
        checkValidEmtpyResponse(expected, result);
    }

    /**
     * Test the empty response for record recommendation, using an apikey only
     */
    @Test
    public void testEmptyResponseWithPageSeedRecordKeyOnly() throws Exception {
        SearchAPIEmptyResponse expected = new SearchAPIEmptyResponse(WSKEY_VALUE );

        ResultActions result = mockMvc.perform(get("/recommend/record/{datasetId}/{localId}.json",
                        "92092", "BibliographicResource_1000086018920")
                        .param(WSKEY_PARAM, WSKEY_VALUE)
                        .param(PAGE_PARAM, "10")
                        .param(SEED_PARAM, String.valueOf(new Random().nextInt())))
                .andDo(print());
        checkValidEmtpyResponse(expected, result);
    }

    /**
     * Test the empty response for record recommendation, using an apikey only
     */
    @Test
    public void testRecordRecommendInvalidSeed() throws Exception {
        ResultActions result = mockMvc.perform(get("/recommend/record/{datasetId}/{localId}.json",
                        "92092", "BibliographicResource_1000086018920")
                        .param(WSKEY_PARAM, WSKEY_VALUE)
                        .param(PAGE_PARAM, "10")
                        .param(SEED_PARAM, "test456"))
                .andExpect(status().is(400));
    }

    private ResultActions checkValidEmtpyResponse(SearchAPIEmptyResponse expected, ResultActions response) throws Exception {
        return response
            .andExpect(status().is(200))
            .andExpect(jsonPath("apiKey").value(expected.getApiKey()))
            .andExpect(jsonPath("success").value(expected.isSuccess()))
            .andExpect(jsonPath("requestNumber").value(expected.getRequestNumber()))
            .andExpect(jsonPath("itemsCount").value(expected.getItemsCount()))
            .andExpect(jsonPath("totalResults").value(expected.getTotalResults()))
            .andExpect(jsonPath("items").isEmpty());
    }


}
