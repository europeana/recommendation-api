package eu.europeana.api.recommend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.*;

public class RecommendServiceUtils {

    private static final Logger LOG = LogManager.getLogger(RecommendServiceUtils.class);

    private static final String SOLR_ID_FIELD = "europeana_id";
    private static final String ITEMS_FIELD = "items";
    private static final String PREF_LABEL_FIELD = "prefLabel";
    private static final String ALT_LABEL_FIELD = "altLabel";


    /**
     * Constructs a Search API query in the form
     * <pre>query=europeana_id:("/x1/y1" OR "/x2/y2 OR "/x3/y3")&pageSize=3&profile=minimal&wskey=[wskey]</pre>
     */
    public String generateSearchQuery(String[] recordIds, int maxResults, String wskey) {
        StringBuilder s = new StringBuilder(50).append("?query=");
        for  (int i = 0; i < maxResults && i < recordIds.length; i++) {
            if (i > 0) {
                s.append(" OR ");
            }
            s.append(SOLR_ID_FIELD).append(":\"").append(recordIds[i]).append('"');
        }
        s.append("&rows=").append(recordIds.length)
                .append("&profile=minimal")
                .append("&wskey=").append(wskey);
        return s.toString();
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
    public String entityApiSearchQuery(String entityUri, String wskey) {
        StringBuilder query = new StringBuilder("?query=entity_uri:");
        query.append("\"" + entityUri + "\"");
        query.append("&fl=skos_prefLabel.*,skos_altLabel.*");
        query.append("&").append("wskey=").append(wskey);
        return query.toString();
    }

    /**
     * Generates set Api search query to fetch entity set items
     * for the given entity uri.
     *
     * example : ?query=type:EntityBestItemsSet&f&qf=subject:<entityUri>&pageSize=<pageSize>&profile=standard&wskey=
     * @param entityUri
     * @param pageSize
     * @param wskey
     * @return
     */
    public String setApiSearchQuery(String entityUri, String pageSize, String wskey) {
        StringBuilder query = new StringBuilder("?query=type:EntityBestItemsSet");
        query.append("&qf=subject:").append(entityUri);
        query.append("&pageSize=").append(pageSize);
        query.append("&profile=standard");
        query.append("&").append("wskey=").append(wskey);
        return query.toString();
    }

    // ---------------------- Temporary functionality below to mock entity recommendations
    private String generateEntitySearchQuery(String entityFullId, int maxResults, String wskey) {
        StringBuilder s = new StringBuilder(50)
                .append("?query=text:\"").append(entityFullId).append("\"")
                .append("&rows=").append(maxResults)
                .append("&sort=random")
                .append("&profile=minimal")
                .append("&wskey=").append(wskey);
        return s.toString();
    }

    /**
     * extracts items from set api response
     * @param jsonObject
     * @return
     */
    public List<String> extractItems(JSONObject jsonObject) {
        List<String> items = new ArrayList<>();
        try {
            JSONArray setsList = jsonObject.getJSONArray(ITEMS_FIELD);
            for(int i =0; i < setsList.length(); i++) {
                JSONObject set = (JSONObject) setsList.get(i);
                JSONArray itemList =  set.getJSONArray(ITEMS_FIELD);
                for(int j =0; j < itemList.length(); j++) {
                 items.add(StringUtils.substringAfter((String) itemList.get(j), "http://data.europeana.eu/item"));
                }
            }
        } catch (JSONException e) {
            LOG.error("Error fetching the objects from set api response. {}", e.getMessage());
        }
        return items;
    }

    /**
     * Extracts skos:prefLabel(s) and skos:altLabel(s)
     *
     * @param jsonObject
     * @return
     */
    public List<String> extractLabels(JSONObject jsonObject) {
        List<String> labels = new ArrayList<>();
        try {
            JSONArray items =  jsonObject.getJSONArray(ITEMS_FIELD);
            for(int i =0; i < items.length(); i++) {
                JSONObject item = (JSONObject) items.get(i);
                JSONObject prefLabel = item.getJSONObject(PREF_LABEL_FIELD);
                JSONObject altLabel = item.getJSONObject(ALT_LABEL_FIELD);
                labels.addAll(getPreflabels(prefLabel.toString()));
                labels.addAll(getAltLabels(altLabel.toString()));
            }
        } catch (JSONException e) {
            LOG.error("Error fetching the objects from entity api response. {}", e.getMessage());
        }
        return labels;
    }

    /**
     * Returns all the prefLabel values of all languages
     * @param prefLabels
     * @return
     */
    private Collection<String> getPreflabels(String prefLabels) {
       try {
           ObjectMapper mapper = new ObjectMapper();
           return mapper.readValue(prefLabels, new TypeReference<HashMap>(){}).values();
        } catch (JsonProcessingException e) {
           LOG.error("Error reading the prefLabels. {}", e.getMessage());
        }
        return new ArrayList();
    }

    /**
     * Returns all the altLabel values of all languages
     * @param altLabels
     * @return
     */
    private List<String> getAltLabels(String altLabels) {
       Map<String, List<String>> map ;
       List<String> altLabel = new ArrayList<>();
       try {
           ObjectMapper mapper = new ObjectMapper();
           map = mapper.readValue(altLabels, new TypeReference<HashMap>(){});
           map.entrySet().stream().forEach((entry) -> {
                for(String value : entry.getValue()) {
                    altLabel.add(value);
                }});
        } catch (JsonProcessingException e) {
           LOG.error("Error reading the altLabels. {}", e.getMessage());
        }
        return altLabel;
    }
}
