package eu.europeana.api.recommend.util;

public class SearchAPIUtils {

    private static final String SOLR_ID_FIELD = "europeana_id";

    private SearchAPIUtils() {
        // to hide implicit public one
    }

    /**
     * Constructs a Search API query in the form
     * <pre>query=europeana_id:("/x1/y1" OR "/x2/y2 OR "/x3/y3")&pageSize=3&profile=minimal&wskey=[wskey]</pre>
     */
    public static String generateSearchQuery(String[] recordIds, int maxResults, String wskey) {
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
}
