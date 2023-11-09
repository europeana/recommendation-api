package eu.europeana.api.recommend.web;

import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.service.RecommendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendController.class)
public class RecommendControllerTest {

    public static final String AUTH_HEADER     = "Authorization";
    public static final String TOKEN           = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYXpwIjoidGVzdF9rZXkiLCJqdGkiOiJiODM4MDM3Ni1mYThhLTQxN2ItODg0NC0xZTQ4ZjBlNDkyNjkiLCJpYXQiOjE1OTg0NTAwNzIsImV4cCI6MTU5ODQ1MzY3Mn0.in-NrpzLE4NVptQJUbFzEeWUDMpZShlad3GRIxgUlVk";
     private static final String PAGE_PARAM     = "page";
    private static final String SEED_PARAM     = "seed";

    public static final String X_API_KEY_HEADER     = "X-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendSettings recommendSettings; // to prevent loading non-existing properties
    @MockBean
    private RecommendService recommendService;


    @Test
    public void testRecordOkApiKey() throws Exception {
        this.mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/", "a", "1")
                        .param("wskey", "test"))
                .andExpect(status().isOk());
    }

    @Test
    public void testRecordOkToken() throws Exception {
        this.mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/","a", "1")
                        .header(AUTH_HEADER, TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    public void testRecordMissingCredentials() throws Exception {
        this.mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/","a", "1"))
                .andExpect(status().is(401));
        this.mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/","a", "1")
                        .param("wskey", ""))
                .andExpect(status().is(401));
        this.mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/","a", "1"))
                .andExpect(status().is(401));
        this.mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/","a", "1")
                        .header(AUTH_HEADER, ""))
                .andExpect(status().is(401));
    }

    @Test
    public void testRecordInvalidId() throws Exception {
        mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/","ValidSetId", "But(Invalid)LocalId$")
                        .header(AUTH_HEADER, TOKEN))
                .andExpect(status().is(400));
    }

    @Test
    public void testRecordInvalidToken() throws Exception {
        this.mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/","a", "1")
                        .header(AUTH_HEADER, "invalidToken"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testSetOkApiKey() throws Exception {
        this.mockMvc.perform(get("/recommend/set/{setId}/", 2)
                        .param("wskey", "test"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSetOkApiToken() throws Exception {
        this.mockMvc.perform(get("/recommend/set/{setId}/", 2)
                        .header(AUTH_HEADER, TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    public void testSetInvalidId() throws Exception {
        this.mockMvc.perform(get("/recommend/set/{setId}/", "invalid")
                        .header(AUTH_HEADER, TOKEN))
                .andExpect(status().is(400));
    }

    @Test
    public void testSetInvalidSeed() throws Exception {
        ResultActions result = mockMvc.perform(get("/recommend/set/{setId}", "2")
                        .header(AUTH_HEADER, TOKEN)
                        .param(PAGE_PARAM, "5")
                        .param(SEED_PARAM, "test1234"))
                .andExpect(status().is(400));
    }

    @Test
    public void testEntityOkApiKey() throws Exception {
        this.mockMvc.perform(get("/recommend/entity/{type}/{id}", "agent", "1")
                        .param("wskey", "test"))
                .andExpect(status().isOk());
    }

    @Test
    public void testEntityOkApiToken() throws Exception {
        this.mockMvc.perform(get("/recommend/entity/{type}/{id}", "concept", "2")
                        .header(AUTH_HEADER, TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    public void testEntityInvalidType() throws Exception {
        this.mockMvc.perform(get("/recommend/entity/{type}/{id}", "invalid", 3)
                        .param("wskey", "test"))
                .andExpect(status().is(400));
    }

    @Test
    public void testEntityInvalidId() throws Exception {
        mockMvc.perform(get("/recommend/entity/{type}/{id}.json","agent", "x")
                        .header(AUTH_HEADER, TOKEN))
                .andExpect(status().is(400));
    }


    /* Tests for New Header xApiKey */

    @Test
    public void testRecordOkWithApiKeyOnlyInHeader() throws Exception {
        this.mockMvc.perform(get("/recommend/record/{datasetId}/{localId}/", "a", "1")
                .header(X_API_KEY_HEADER, "test"))
            .andExpect(status().isOk());

    }

    @Test
    public void testEntityOkWithApiKeyOnlyInHeader() throws Exception {

        this.mockMvc.perform(get("/recommend/entity/{type}/{id}", "concept", "2")
                .header(X_API_KEY_HEADER, "test"))
            .andExpect(status().isOk());
    }

    @Test
    public void testSetOkWithApiKeyOnlyInHeader() throws Exception {
        this.mockMvc.perform(get("/recommend/set/{setId}/", 2)
                .header(X_API_KEY_HEADER, "test"))
            .andExpect(status().isOk());
    }



}
