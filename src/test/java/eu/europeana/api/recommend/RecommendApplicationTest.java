package eu.europeana.api.recommend;

import eu.europeana.api.recommend.config.RecommendSettings;
import eu.europeana.api.recommend.service.MilvusService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class RecommendApplicationTest {

    @MockBean
    RecommendSettings settings; // to prevent loading non-existing properties
    @MockBean
    MilvusService milvusService; // to prevent connecting to Milvus

    @SuppressWarnings("squid:S2699") // we are aware that this test doesn't have any assertion
    @Test
    void contextLoads() {
    }

}
