package eu.europeana.api.recommend.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class RecommendServiceUtilsTest {

    private final static String ENTITY_URI = "http://data.europeana.eu/agent/base/60753";
    private final static String ENTITY_API_RESPONSE_FILE = "/json/entity_api_response.json";
    private final static String SET_API_RESPONSE_FILE = "/json/set_api_response.json";

    private RecommendServiceUtils recommendServiceUtils ;

    @BeforeEach
    void setup(){
        recommendServiceUtils = new RecommendServiceUtils();
    }

    @Test
    public void entityApiSearchQueryTest() {
        String entityUrl = recommendServiceUtils.entityApiSearchQuery(ENTITY_URI, "test");
        assertNotNull(entityUrl);
        assertTrue(StringUtils.contains(entityUrl, ENTITY_URI));
        assertTrue(StringUtils.contains(entityUrl, "wskey=test"));
        assertTrue(StringUtils.contains(entityUrl, "fl=skos_prefLabel.*,skos_altLabel.*"));
    }

    @Test
    public void setApiSearchQueryTest(){
        String setApiUrl = recommendServiceUtils.setApiSearchQuery(ENTITY_URI, "20", "test");
        assertNotNull(setApiUrl);
        assertTrue(StringUtils.contains(setApiUrl, ENTITY_URI));
        assertTrue(StringUtils.contains(setApiUrl, "wskey=test"));
        assertTrue(StringUtils.contains(setApiUrl, "pageSize=20"));
        assertTrue(StringUtils.contains(setApiUrl, "type:EntityBestItemsSet"));
        assertTrue(StringUtils.contains(setApiUrl, "subject:"+ENTITY_URI));
    }

    @Test
    public void extractLabelTest() {
        JSONObject jsonObject = getJson(ENTITY_API_RESPONSE_FILE);
        List<String> labels = recommendServiceUtils.extractLabels(jsonObject);
        // perfLabels + altLabels = 8 + 4
        assertEquals(12, labels.size());
        assertTrue(labels.contains("Sandro Botticelli"));
        assertTrue(labels.contains("Сандра Бацічэлі"));
        assertTrue(labels.contains("Botticelli, Sandro"));
        assertTrue(labels.contains("বত্তিচেল্লি, সান্দ্রো"));
    }

    @Test
    public void extractItemsTest() {
        JSONObject jsonObject = getJson(SET_API_RESPONSE_FILE);
        List<String> items = recommendServiceUtils.extractItems(jsonObject);
        assertEquals(7, items.size());
        assertTrue(items.contains("/abcd/P755"));
        assertTrue(items.contains("/abcd/21_19"));
        assertTrue(items.contains("/abcd/Kunst__94"));
    }

    private JSONObject getJson(String file) {
        try (InputStream inputStreamObject = this.getClass().getResourceAsStream(file);
              BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStreamObject, "UTF-8"))){
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
            return jsonObject;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
