package eu.europeana.api.recommend.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EntityAPIUtilsTest {

    protected final static String ENTITY_URI = "http://data.europeana.eu/agent/base/60753";
    private final static String ENTITY_API_RESPONSE_FILE = "/json/entity_api_response.json";

    @Test
    public void entityApiSearchQueryTest() {
        String entityUrl = EntityAPIUtils.entityApiSearchQuery(ENTITY_URI, "test");
        assertNotNull(entityUrl);
        assertTrue(StringUtils.contains(entityUrl, ENTITY_URI));
        assertTrue(StringUtils.contains(entityUrl, "wskey=test"));
        assertTrue(StringUtils.contains(entityUrl, "fl=skos_prefLabel.*,skos_altLabel.*"));
    }

    @Test
    public void extractLabelTest() throws JSONException {
        JSONObject jsonObject = getJson(ENTITY_API_RESPONSE_FILE);
        List<String> labels = EntityAPIUtils.extractLabels(jsonObject);
        // perfLabels + altLabels = 8 + 4
        assertEquals(12, labels.size());
        assertTrue(labels.contains("Sandro Botticelli"));
        assertTrue(labels.contains("Сандра Бацічэлі"));
        assertTrue(labels.contains("Botticelli, Sandro"));
        assertTrue(labels.contains("বত্তিচেল্লি, সান্দ্রো"));
    }

    protected JSONObject getJson(String file) {
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
