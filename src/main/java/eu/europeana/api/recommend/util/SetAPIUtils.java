package eu.europeana.api.recommend.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.*;

public final class SetAPIUtils {

    private static final String ITEMS_FIELD = "items";

    private SetAPIUtils() {
        // to hide implicit public one
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
    public static String setApiSearchQuery(String entityUri, String pageSize, String wskey) {
        StringBuilder query = new StringBuilder("?query=type:EntityBestItemsSet");
        query.append("&qf=subject:").append(entityUri);
        query.append("&pageSize=").append(pageSize);
        query.append("&profile=standard");
        query.append("&").append("wskey=").append(wskey);
        return query.toString();
    }

    /**
     * extracts items from set api response
     * @param jsonObject
     * @return
     */
    public static List<String> extractItems(JSONObject jsonObject) throws JSONException {
        List<String> items = new ArrayList<>();
        JSONArray setsList = jsonObject.getJSONArray(ITEMS_FIELD);
        for (int i = 0; i < setsList.length(); i++) {
            JSONObject set = (JSONObject) setsList.get(i);
            // if items are available for that entity set
            if (set.has(ITEMS_FIELD)) {
                JSONArray itemList =  set.getJSONArray(ITEMS_FIELD);
                for (int j = 0; j < itemList.length(); j++) {
                    items.add(StringUtils.substringAfter((String) itemList.get(j), "http://data.europeana.eu/item"));
                }
            }
        }
        return items;
    }
}
