package eu.europeana.api.recommend.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Locale;

/**
 * Container for all settings that we load from the recommend.properties file and optionally override from
 * recommend.user.properties file
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@Configuration
@Component
@PropertySource("classpath:recommend.properties")
@PropertySource(value = "classpath:recommend.user.properties", ignoreResourceNotFound = true)
public class RecommendSettings {

    private static final Logger LOG = LogManager.getLogger(RecommendSettings.class);

    @Value("${search.api.host}")
    private String searchApiHost;
    @Value("${search.api.endpoint}")
    private String searchApiEndpoint;
    @Value("${recommend.engine.host}")
    private String rengineHost;
    @Value("${recommend.engine.recommend.path}")
    private String rengineRecommendPath;
    @Value("${recommend.engine.events.path}")
    private String rengineEventsPath;


    /**
     * @return the host name part of the configured Search API
     */
    public String getSearchApiHost() {
        return searchApiHost;
    }

    /**
     * @return the full endpoint (host name + path) of the configured Search API search endpoint
     */
    public String getSearchApiEndpoint() {
        return searchApiEndpoint;
    }

    public String getREngineHost() {
        return rengineHost;
    }

    public String getREngineRecommendPath() {
        return rengineRecommendPath;
    }

    public String getREngineEventsPath() {
        return rengineEventsPath;
    }

    @PostConstruct
    private void logImportantSettings() {
        searchApiEndpoint = addProtocolIfMissing(searchApiEndpoint);
        rengineHost = addProtocolIfMissing(rengineHost);

        LOG.info("Recommendation API settings:");
        LOG.info("  Search API endpoint: {}", searchApiEndpoint);
        LOG.info("  Recommender engine host: {}", rengineHost);
    }

    private String addProtocolIfMissing(String hostName) {
        String host = hostName.toLowerCase(Locale.GERMAN);
        if (!host.startsWith("https://") && !host.startsWith("http://")) {
            return "https://" + host;
        }
        return host;
    }

}
