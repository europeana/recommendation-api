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


    @Value("${search.api.endpoint}")
    private String searchApiEndpoint;

    @Value("${set.api.endpoint}")
    private String setApiEndpoint;

    @Value("${entity.api.endpoint}")
    private String entityApiEndpoint;

    @Value("${embeddings.endpoint}")
    private String embeddingsApiEndpoint;

    @Value("${milvus.hostname}")
    private String milvusHostName;

    @Value("${milvus.port}")
    private Integer milvusPort;

    @Value("${milvus.collection}")
    private String milvusCollection;

    @Value("${webclient.max.memsizemb:10}")
    private Integer webClientMaxMemMb;


    public String getSearchApiEndpoint() {
        return this.searchApiEndpoint;
    }

    public String getSetApiEndpoint() {
        return this.setApiEndpoint;
    }

    public String getEntityApiEndpoint() {
        return this.entityApiEndpoint;
    }

    public String getEmbeddingsApiEndpoint() {
        return this.embeddingsApiEndpoint;
    }
    /**
     * @return the configured Milvus hostname or ip address
     */
    public String getMilvusHostName() {
        return milvusHostName;
    }

    /**
     * @return the configured Milvus port number
     */
    public int getMilvusPort() {
        return milvusPort;
    }

    /**
     * @return the configured Milvus collection to use
     */
    public String getMilvusCollection() {
        return milvusCollection;
    }

    public Integer getWebClientMaxMemMb() {
        return webClientMaxMemMb;
    }

    @PostConstruct
    private void validateAndLogSettings() {
        searchApiEndpoint = addProtocolIfMissing(addTrailingSlashIfMissing(searchApiEndpoint));
        setApiEndpoint = addProtocolIfMissing(addTrailingSlashIfMissing(setApiEndpoint));
        entityApiEndpoint = addProtocolIfMissing(addTrailingSlashIfMissing(entityApiEndpoint));
        embeddingsApiEndpoint = addProtocolIfMissing(embeddingsApiEndpoint); // don't add trailing slash!

        LOG.info("Recommendation API settings:");
        LOG.info("  Milvus {}:{}, collection {}", milvusHostName, milvusPort, milvusCollection);
        LOG.info("  Embeddings endpoint: {}", embeddingsApiEndpoint);
        LOG.info("  Search API endpoint: {}", searchApiEndpoint);
        LOG.info("  Set    API endpoint: {}", setApiEndpoint);
        LOG.info("  Entity API endpoint: {}", entityApiEndpoint);
    }

    private String addProtocolIfMissing(String hostName) {
        String host = hostName.toLowerCase(Locale.GERMAN).trim();
        if (!host.startsWith("https://") && !host.startsWith("http://")) {
            host = "https://" + host;
        }
        return host;
    }

    private String addTrailingSlashIfMissing(String hostName) {
        String host = hostName.toLowerCase(Locale.GERMAN).trim();
        if (!host.endsWith("/")) {
            host = host + "/";
        }
        return host;
    }

}
