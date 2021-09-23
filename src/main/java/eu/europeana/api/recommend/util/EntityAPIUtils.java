package eu.europeana.api.recommend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.*;

public class EntityAPIUtils {

    private static final Logger LOG = LogManager.getLogger(EntityAPIUtils.class);

    private static final String ITEMS_FIELD = "items";
    private static final String PREF_LABEL_FIELD = "prefLabel";
    private static final String ALT_LABEL_FIELD = "altLabel";
    public static final String TOTAL = "total";

    private EntityAPIUtils() {
        // to hide implicit public one
    }


    /**
     * Build the entity id/url
     *
     * @param type
     * @param id
     * @return
     */
    public static String buildEntityId(String type, String id) {
        StringBuilder entityId = new StringBuilder("http://data.europeana.eu");
        entityId.append("/").append(type);
        entityId.append("/base/").append(id);
        return entityId.toString();
    }

    /**
     * Generates Entity Api search query to fetch all skos:preflabel and skos:altlabel
     * for the given entity uri.
     *
     * example : ?query=entity_uri:%22<entityUri>%22&fl=skos_prefLabel.*,skos_altLabel.*&wskey=
     * @param entityUri
     * @param wskey
     * @return
     */
    public static String entityApiSearchQuery(String entityUri, String wskey) {
        StringBuilder query = new StringBuilder("?query=entity_uri:");
        query.append("\"").append(entityUri).append("\"");
        query.append("&fl=skos_prefLabel.*,skos_altLabel.*");
        query.append("&").append("wskey=").append(wskey);
        return query.toString();
    }

    /**
     * Extracts skos:prefLabel(s) and skos:altLabel(s)
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    public static List<String> extractLabels(JSONObject jsonObject) throws JSONException {
        List<String> labels = new ArrayList<>();
        JSONArray items = jsonObject.getJSONArray(ITEMS_FIELD);
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = (JSONObject) items.get(i);
            if (item.has(PREF_LABEL_FIELD)) {
                JSONObject prefLabel = item.getJSONObject(PREF_LABEL_FIELD);
                labels.addAll(getPreflabels(prefLabel.toString()));
            }
            if (item.has(ALT_LABEL_FIELD)) {
                JSONObject altLabel = item.getJSONObject(ALT_LABEL_FIELD);
                labels.addAll(getAltLabels(altLabel.toString()));
            }
        }
        return labels;
    }

    /**
     * Returns all the prefLabel values of all languages
     * @param prefLabels
     * @return
     */
    private static Collection<String> getPreflabels(String prefLabels) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(prefLabels, new TypeReference<HashMap>(){}).values();
        } catch (JsonProcessingException e) {
            LOG.error("Error reading the prefLabels.", e);
        }
        return new ArrayList();
    }

    /**
     * Returns all the altLabel values of all languages
     * @param altLabels
     * @return
     */
    private static List<String> getAltLabels(String altLabels) {
        Map<String, List<String>> map ;
        List<String> altLabel = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            map = mapper.readValue(altLabels, new TypeReference<HashMap>(){});
            map.entrySet().stream().forEach((entry) -> {
                for(String value : entry.getValue()) {
                    altLabel.add(value);
                }
            });
        } catch (JsonProcessingException e) {
            LOG.error("Error reading the altLabels.", e);
        }
        return altLabel;
    }
}
