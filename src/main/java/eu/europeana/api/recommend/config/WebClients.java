package eu.europeana.api.recommend.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configures the various webclients we use to send/receive data.
 */
@Configuration
public class WebClients {

    private static final Logger LOG = LogManager.getLogger(WebClients.class);

    private static final int BYTES_PER_MB = 1024 * 1024;

    // TODO for now we set a timeout for all request of 10 seconds
    private static final int TIMEOUT = 10;

    private RecommendSettings config;
    private BuildInfo buildInfo;
    // defaults to 10; will be overwritten when defined in properties
    private int maxMemSizeMb = 10;


    public WebClients(RecommendSettings config, BuildInfo buildInfo) {
        this.config = config;
        this.buildInfo = buildInfo;
        if (null != config.getWebClientMaxMemMb()){
            maxMemSizeMb = config.getWebClientMaxMemMb();
        }
    }

    @Bean
    public WebClient getSearchApiClient() {
        return createWebClient(config.getSearchApiEndpoint());
    }

    @Bean
    public WebClient getEntityApiClient() {
        return createWebClient(config.getEntityApiEndpoint());
    }

    @Bean
    public WebClient getSetApiClient() {
        return createWebClient(config.getSetApiEndpoint());
    }

    @Bean
    public WebClient getEmbeddingsClient() {
        return createWebClient(config.getEmbeddingsApiEndpoint());
    }

    private WebClient createWebClient(String endpoint) {
        return getApiClient(endpoint, true, maxMemSizeMb, TIMEOUT);
    }

    private WebClient getApiClient(String apiEndpoint, boolean exchangeStrategy, int maxMemSizeMB, int timeoutInSec) {
        LOG.debug("Creating webclient for {}", apiEndpoint);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .compress(true)
                        .responseTimeout(Duration.ofSeconds(timeoutInSec))));
        if (exchangeStrategy) {
             webClientBuilder.exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(maxMemSizeMB * BYTES_PER_MB))
                    .build());
        }
        return webClientBuilder
                .baseUrl(apiEndpoint)
                .defaultHeader(HttpHeaders.USER_AGENT, generateUserAgentName())
                .filter(logRequest())
                .filter(logResponse())
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

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            LOG.trace("Response: {}", response.statusCode().value());
            return Mono.just(response);
        });
    }
}
