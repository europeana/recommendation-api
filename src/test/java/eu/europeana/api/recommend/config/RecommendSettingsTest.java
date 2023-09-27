package eu.europeana.api.recommend.config;


import eu.europeana.api.recommend.service.MilvusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations="classpath:recommend.test.properties")
public class RecommendSettingsTest {

    @Autowired
    RecommendSettings settings;

    @MockBean
    MilvusService milvusService; // to prevent connecting to Milvus

    @Test
    public void appendProtocolAndTrailingSlashTest() {
        assertEquals("https://api.my.org/test/record/", settings.getSearchApiEndpoint());
        assertEquals("https://api.my.org/test/set/", settings.getSetApiEndpoint());
        assertEquals("https://api.my.org/test/entity/", settings.getEntityApiEndpoint());
    }
}
