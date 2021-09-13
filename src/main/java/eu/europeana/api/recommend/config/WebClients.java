package eu.europeana.api.recommend.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures the 2 webclients we use to send/receive data; 1 for the Search API and 1 for the Recommendation Engine
 */
@Configuration
public class WebClients {

    private static final Logger LOG = LogManager.getLogger(WebClients.class);

    private RecommendSettings config;
    private BuildInfo buildInfo;
    // defaults to 10; will be overwritten when defined in properties
    private int maxInMemSizeMb = 10;

    // TODO configure timeouts for webClients
    // TODO stress test to see how much traffic it can handle (with default settings)

    public WebClients(RecommendSettings config, BuildInfo buildInfo) {
        this.config = config;
        this.buildInfo = buildInfo;
        if (null != config.getMaxInMemSizeMb()){
            maxInMemSizeMb = config.getMaxInMemSizeMb();
        }
    }

    @Bean
    public WebClient getSearchApiClient() {
        return getApiClient(config.getSearchApiEndpoint(), true);
    }

    @Bean
    public WebClient getRecommendEngineClient() {
        return getApiClient(config.getREngineHost(), false);
    }

    @Bean
    public WebClient getEntityApiClient() {
        return getApiClient(config.getEntityApiEndpoint(), true);
    }

    @Bean
    public WebClient getSetApiClient() {
        return getApiClient(config.getSetApiEndpoint(), true);
    }

    private WebClient getApiClient(String apiEndpoint, boolean exchangeStrategy) {
        WebClient.Builder webClientBuilder = WebClient.builder();
        if (exchangeStrategy) {
             webClientBuilder.exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(maxInMemSizeMb * 1024 * 1024))
                    .build());
        }
        return webClientBuilder
                .baseUrl(apiEndpoint)
                .defaultHeader(HttpHeaders.USER_AGENT, generateUserAgentName())
                .filter(logRequest())
                .build();
    }

    private String generateUserAgentName() {
        return buildInfo.getAppName() + " v" + buildInfo.getAppVersion();
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            LOG.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            return next.exchange(clientRequest);
        };
    }
}
