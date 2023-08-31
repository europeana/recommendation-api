package eu.europeana.api.recommend.config;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations="classpath:recommend.test.properties")
public class RecommendSettingsTest {

    @Autowired
    RecommendSettings settings;

    @Test
    public void appendProtocolAndTrailingSlashTest() {
        assertEquals("https://api.europeana.eu/test/record/", settings.getSearchApiEndpoint());
        assertEquals("https://api.europeana.eu/test/set/", settings.getSetApiEndpoint());
        assertEquals("https://api.europeana.eu/test/entity/", settings.getEntityApiEndpoint());
    }
}
