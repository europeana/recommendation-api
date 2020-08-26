package eu.europeana.api.recommend;

import eu.europeana.api.recommend.model.SearchAPIEmptyResponse;
import eu.europeana.api.recommend.service.RecommendService;
import eu.europeana.api.recommend.web.RecommendController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class RecommendControllerTest {

    private static final String AUTH_HEADER     = "Authorization";
    private static final String TOKEN           = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYXpwIjoidGVzdF9rZXkiLCJqdGkiOiJiODM4MDM3Ni1mYThhLTQxN2ItODg0NC0xZTQ4ZjBlNDkyNjkiLCJpYXQiOjE1OTg0NTAwNzIsImV4cCI6MTU5ODQ1MzY3Mn0.in-NrpzLE4NVptQJUbFzEeWUDMpZShlad3GRIxgUlVk";
    private static final String TOKEN_API_KEY   = "test_key"; // this key is encoded in the token

    private static final String WSKEY_PARAM     = "wskey";
    private static final String WSKEY_VALUE     = "anotherTestKey";

    private static RecommendController recommendController;
    private static RecommendService recommendService;

    private static MockMvc recommendControllerMock;

    @Before
    public void setup() {
        recommendService = mock(RecommendService.class);
        recommendController = spy(new RecommendController(recommendService));
        recommendControllerMock = MockMvcBuilders
                .standaloneSetup(recommendController)
                .build();
    }

    @Test
    public void testNoAuthorizationSet() throws Exception {
        recommendControllerMock.perform(get("/recommend/set/{setId}", "2"))
                .andExpect(status().is(401));
    }

    @Test
    public void testNoAuthorizationRecord() throws Exception {
        recommendControllerMock.perform(get("/recommend/record/{datasetId}/{localId}.json",
                "92092", "BibliographicResource_1000086018920"))
                .andExpect(status().is(401));
    }

    /**
     * Test the empty response for sets recommendation, as well as getting the apikey from a token
     */
    @Test
    public void testEmptyResponseSetTokenOnly() throws Exception {
        SearchAPIEmptyResponse response = new SearchAPIEmptyResponse(TOKEN_API_KEY);

        recommendControllerMock.perform(get("/recommend/set/{setId}", "2")
                .header(AUTH_HEADER, TOKEN))

                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(content().json("{\n" +
                        "    \"apiKey\": \"" + response.getApiKey() + "\",\n" +
                        "    \"success\": " + response.isSuccess() + ",\n" +
                        "    \"requestNumber\": " + response.getRequestNumber() + ",\n" +
                        "    \"itemsCount\": " + response.getItemsCount() + ",\n" +
                        "    \"totalResults\": " + response.getTotalResults() + ",\n" +
                        "    \"items\": []\n" +
                        "}"));
    }

    /**
     * Test the empty response for record recommendation, as well as getting the apikey from a token (when both
     * token and wskey are provided)
     */
    @Test
    public void testEmptyResponseRecordTokenAndKey() throws Exception {
        SearchAPIEmptyResponse response = new SearchAPIEmptyResponse(TOKEN_API_KEY);

        recommendControllerMock.perform(get("/recommend/set/{setId}", "2")
                .header(AUTH_HEADER, TOKEN)
                .param(WSKEY_PARAM, WSKEY_VALUE))

                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(content().json(expectedEmtpyResponse(response)));
    }

    /**
     * Test the empty response for record recommendation, using an apikey only
     */
    @Test
    public void testEmptyResponseRecordKeyOnly() throws Exception {
        SearchAPIEmptyResponse response = new SearchAPIEmptyResponse(WSKEY_VALUE );

        recommendControllerMock.perform(get("/recommend/record/{datasetId}/{localId}.json",
                "92092", "BibliographicResource_1000086018920")
                .param(WSKEY_PARAM, WSKEY_VALUE))

                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(content().json(expectedEmtpyResponse(response)));
    }

    private String expectedEmtpyResponse(SearchAPIEmptyResponse response) {
        return  "{\n" +
                "    \"apiKey\": \"" + response.getApiKey() + "\",\n" +
                "    \"success\": " + response.isSuccess() + ",\n" +
                "    \"requestNumber\": " + response.getRequestNumber() + ",\n" +
                "    \"itemsCount\": " + response.getItemsCount() + ",\n" +
                "    \"totalResults\": " + response.getTotalResults() + ",\n" +
                "    \"items\": []\n" +
                "}";
    }

}
