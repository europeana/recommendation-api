package eu.europeana.api.recommend.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Container class for holding relevant entity data returned by Entity Management API
 */
public class Entity {

    private static final String BASE_URL_ID = "http://data.europeana.eu";
    private static final String TYPE_AGENT    = "agent";
    private static final String TYPE_CONCEPT  = "concept";
    private static final String TYPE_PLACE    = "place";
    private static final String TYPE_TIMESPAN = "timespan";

    String id;
    String type;
    Map<String, String> prefLabel;
    Map<String, List<String>> altLabel;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getPrefLabel() {
        if (prefLabel == null) {
            return Collections.emptyMap();
        }
        return prefLabel;
    }

    public Map<String, List<String>> getAltLabel() {
        if (altLabel == null) {
            return Collections.emptyMap();
        }
        return altLabel;
    }

    /**
     * @return true if the entity is an agent, otherwise false
     */
    public boolean isAgentType() {
        return TYPE_AGENT.equalsIgnoreCase(this.type);
    }

    /**
     * @return true if the entity is a concept, otherwise false
     */
    public boolean isConceptType() {
        return TYPE_CONCEPT.equalsIgnoreCase(this.type);
    }

    /**
     * @return true if the entity is a place, otherwise false
     */
    public boolean isPlaceType() {
        return TYPE_PLACE.equalsIgnoreCase(this.type);
    }

    /**
     * @return true if the entity is a timespan, otherwise false
     */
    public boolean isTimespanType() {
        return TYPE_TIMESPAN.equalsIgnoreCase(this.type);
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", prefLabel=" + prefLabel +
                ", altLabel=" + altLabel +
                '}';
    }

    /**
     * Generates a string that is the uri of an entity
     * @param type type of the entity
     * @param id id number of the entity
     * @return generates a string that is the uri of an entity
     */
    public static String generateUri(String type, int id) {
        StringBuilder entityId = new StringBuilder(BASE_URL_ID);
        entityId.append("/").append(type);
        entityId.append("/").append(id);
        return entityId.toString();
    }

    /**
     * Check if we support generating recommendations for the provided entity type
     * @param type the agent type to check
     * @return true if the provided entity type is supported for generating recommendations, otherwise false
     */
    public static boolean isSupportedType(String type) {
        return TYPE_AGENT.equalsIgnoreCase(type) ||
                TYPE_CONCEPT.equalsIgnoreCase(type) ||
                TYPE_PLACE.equalsIgnoreCase(type) ||
                TYPE_TIMESPAN.equalsIgnoreCase(type);

    }
}
