package eu.europeana.api.recommend;

import eu.europeana.api.recommend.model.SearchAPIEmptyResponse;
import eu.europeana.api.recommend.service.RecommendService;
import eu.europeana.api.recommend.web.RecommendController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RecommendControllerTest {

    private static final String TOKEN         = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3Y1N6TDZ0a3RCNFhHcUtjbEZncnVaaHQtX3d5MkZUV0FlWUtaYWNSOTNnIn0.eyJqdGkiOiI5MWI1YTk2OC1mZTViLTQzM2MtYmFiZi03OTE2ZmFlZDYzZjYiLCJleHAiOjE1OTIxNDgzMjMsIm5iZiI6MCwiaWF0IjoxNTc2NTk2MzIzLCJpc3MiOiJodHRwczovL2tleWNsb2FrLXNlcnZlci10ZXN0LmVhbmFkZXYub3JnL2F1dGgvcmVhbG1zL2V1cm9wZWFuYSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIxZGMyNzk5Yy1iMjM2LTQyODUtYWI3NC03MWVmZDkyZDhiMjQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJjbGllbnRfdGVzdGVyXzEiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiI4ZjUzZmNlMy1jNmE5LTQzNmItYTY3MC1lNjc4NDIzMDVmZWQiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6Imh1Z28gbWFuZ3VpbmhhcyIsInByZWZlcnJlZF91c2VybmFtZSI6Imh1Z28ubWFuZ3Vpbmhhc0BnbWFpbC5jb20iLCJnaXZlbl9uYW1lIjoiaHVnbyIsImZhbWlseV9uYW1lIjoibWFuZ3VpbmhhcyIsImVtYWlsIjoiaHVnby5tYW5ndWluaGFzQGdtYWlsLmNvbSJ9.BVgMH-8a062UHb_KP3DNsJl-FP_kALEiAUBepBDEn4iwX9drIkn-N4EuiM2EOxvC0QgH1z07nuCJRZrZT7Py4wBjDKaHIB9gmo0_89QxPMxgNk5CMr37oE6zEeTg4nglbIB-V3_cqSFSWCvIljAGb-fp3QDQsyIHOQ2dHgCi3XuFq17xCJI8J1mkGNZD0BlvTagOo8ARfdtlJXjAWamu5u7sCV5GREKq__HphfsIu9Li0U2QpV6hvOWM_v8Kh8eS-x6kObGeSay78Hk07sL8gOXe9QFFgS8PcocwWVT_aXlcxxiZhnjJ9qnHMbAlFuMMH65Ra73hSV1pBsiLjBmHRA";
    private static final String HEADER_STRING = "Authorization";
    private static final String PAGE_SIZE     = "pageSize";
    private static final String API_KEY_TEST  = "client_tester_1";

    private static RecommendController recommendController;
    private static RecommendService recommendService;

    private static MockMvc recommendControllerMock;

    @Before
    public void setup() throws Exception {
        recommendService = mock(RecommendService.class);
        recommendController = spy(new RecommendController(recommendService));

        recommendControllerMock = MockMvcBuilders
                .standaloneSetup(recommendController)
                .build();
    }

    /**
     * Test the empty response for sets recommendation
     * @throws Exception
     */
    @Test
    public void testEmptyResponseForSet() throws Exception {
        SearchAPIEmptyResponse response = new SearchAPIEmptyResponse(API_KEY_TEST);

        recommendControllerMock.perform(get("/recommend/set/{setId}", "2")
                .param(PAGE_SIZE, "2")
                .header(HEADER_STRING, TOKEN))
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
     * Test the empty response for record recommendation
     * @throws Exception
     */
    @Test
    public void testEmptyResponseForRecord() throws Exception {
        SearchAPIEmptyResponse response = new SearchAPIEmptyResponse(API_KEY_TEST);

        recommendControllerMock.perform(get("/recommend/record/{datasetId}/{localId}.json",
                "92092", "BibliographicResource_1000086018920")
                .param(PAGE_SIZE, "2")
                .header(HEADER_STRING, TOKEN))
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

}
