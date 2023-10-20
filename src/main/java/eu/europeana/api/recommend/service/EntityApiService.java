package eu.europeana.api.recommend.service;

import eu.europeana.api.recommend.config.WebClients;
import eu.europeana.api.recommend.model.Entity;
import eu.europeana.api.recommend.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for interacting with Entity API
 */
@Service
public class EntityApiService {

    private WebClient webClient;

    @Autowired
    public EntityApiService(WebClients webclients) {
        this.webClient = webclients.getEntityApiClient();
    }


    /**
     * Return the relevant entity data (prefLabels and altLabels for doing an entity recommendation based on its metadata
     * @param type type of entity, e.g. agent, concept, place
     * @param id id number of the entity
     * @param apikey optional, if empty apikey header is not included (authToken should be provided)
     * @param token optional, if empty the apikey parameter is used
     * @return labels of the requested entity if available, otherwise null
     */
    public Mono<Entity> getEntity(String type, int id, String apikey ,String token) {

        return this.webClient.get()
                .uri(type + '/' + id)
                .headers(RequestUtils.generateHeaders(token,apikey))
                .retrieve()
                .bodyToMono(Entity.class);
    }





}
