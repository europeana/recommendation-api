package eu.europeana.api.recommend.config;

import eu.europeana.api.recommend.service.MilvusService;
import eu.europeana.api.recommend.service.RecommendService;
import eu.europeana.api.recommend.web.RecommendControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMVC test to check if CORS is configured as desired
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CorsConfigTest {

    @MockBean
    private RecommendService recommendService;

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    RecommendSettings settings; // to prevent loading non-existing properties
    @MockBean
    MilvusService milvusService; // to prevent connecting to Milvus

    /**
     * Test if CORS works for GET normal requests and error requests
     */
    @Test
    public void testCorsGet() throws Exception {
        // normal (200 response) request
        testNormalResponse(mockMvc.perform(get("/recommend/set/{setId}", "1")
                .header(HttpHeaders.AUTHORIZATION, RecommendControllerTest.TOKEN)
                .header(HttpHeaders.ORIGIN, "https://test.com")));

        // error request
       testErrorResponse(mockMvc.perform(get("/recommend/set/{setId}", "2-2")
                .header(HttpHeaders.AUTHORIZATION, RecommendControllerTest.TOKEN)
                .header(HttpHeaders.ORIGIN, "https://test.com")));
    }

    /**
     * Test if CORS works for HEAD normal requests and error requests
     */
    @Test
    public void testCorsHead() throws Exception {
        // normal (200 response) request
        testNormalResponse(mockMvc.perform(get("/recommend/record/{datasetId}/{localId}", "3", "3")
                .header(HttpHeaders.AUTHORIZATION, RecommendControllerTest.TOKEN)
                .header(HttpHeaders.ORIGIN, "https://test.com")));

        // error request
        testErrorResponse(mockMvc.perform(get("/recommend/record/{datasetId}/{localId}", "4-4", "4")
                .header(HttpHeaders.AUTHORIZATION, RecommendControllerTest.TOKEN)
                .header(HttpHeaders.ORIGIN, "https://test.com")));
    }

    /**
     * A pre-flight request is an OPTIONS request using three HTTP request headers:
     * Access-Control-Request-Method, Access-Control-Request-Headers, and the Origin header.
     */
    @Test
    public void testCorsPreFlight() throws Exception {
        mockMvc.perform(options("/recommend/set/{setId}", "5")
                .header(HttpHeaders.ORIGIN, "https://test.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

    private void testNormalResponse(ResultActions actions) throws Exception {
        actions.andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

    private void testErrorResponse(ResultActions actions) throws Exception {
        actions.andExpect(status().isBadRequest())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

}
