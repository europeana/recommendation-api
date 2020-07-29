package eu.europeana.api.recommend.config;

import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures the 2 webclients we use to send/receive data; 1 for the Search API and 1 for the Recommendation Engine
 */
@Configuration
public class WebClients {

    private static final Logger LOG = LogManager.getLogger(WebClients.class);

    private RecommendSettings config;
    private BuildInfo buildInfo;

    // TODO configure timeouts for webClients
    // TODO stress test to see how much traffic it can handle (with default settings)

    public WebClients(RecommendSettings config, BuildInfo buildInfo) {
        this.config = config;
        this.buildInfo = buildInfo;
    }

    @Bean
    public WebClient getSearchApiClient() {
        return WebClient.builder()
                .baseUrl(config.getSearchApiEndpoint())
                .defaultHeader(HttpHeaders.USER_AGENT, buildInfo.getAppName() + " v" + buildInfo.getAppVersion())
                .filter(logRequest())
                .build();
    }

    @Bean
    public WebClient getRecommendEngineClient() {
        return WebClient.builder()
                .baseUrl(config.getREngineHost())
                .defaultHeader(HttpHeaders.USER_AGENT, buildInfo.getAppName() + " v" + buildInfo.getAppVersion())
                .filter(logRequest())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            LOG.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            return next.exchange(clientRequest);
        };
    }
}
