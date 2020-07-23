package eu.europeana.api.recommend.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Container for all settings that we load from the recommend.properties file and optionally override from
 * recommend.user.properties file
 *
 * @author Patrick Ehlert
 * Created on 22 Jul-2020
 */
@Configuration
@Component
@PropertySource("classpath:recommend.properties")
@PropertySource(value = "classpath:recommend.user.properties", ignoreResourceNotFound = true)
public class RecommendSettings {

    private static final Logger LOG = LogManager.getLogger(RecommendSettings.class);



    @PostConstruct
    private void logImportantSettings() {
        LOG.info("Recommendation API settings:");

    }
}
