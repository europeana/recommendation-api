package eu.europeana.api.recommend.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class SetAPIUtilsTest {

    private final static String SET_API_RESPONSE_FILE = "/json/set_api_response.json";

    @Test
    public void setApiSearchQueryTest(){
        String setApiUrl = SetAPIUtils.setApiSearchQuery(EntityAPIUtilsTest.ENTITY_URI, "20", "test");
        assertNotNull(setApiUrl);
        assertTrue(StringUtils.contains(setApiUrl, EntityAPIUtilsTest.ENTITY_URI));
        assertTrue(StringUtils.contains(setApiUrl, "wskey=test"));
        assertTrue(StringUtils.contains(setApiUrl, "pageSize=20"));
        assertTrue(StringUtils.contains(setApiUrl, "type:EntityBestItemsSet"));
        assertTrue(StringUtils.contains(setApiUrl, "subject:"+EntityAPIUtilsTest.ENTITY_URI));
    }

    @Test
    public void extractItemsTest() throws JSONException {
        EntityAPIUtilsTest entityAPIUtilsTest = new EntityAPIUtilsTest();
        JSONObject jsonObject = entityAPIUtilsTest.getJson(SET_API_RESPONSE_FILE);
        List<String> items = SetAPIUtils.extractItems(jsonObject);
        assertEquals(7, items.size());
        assertTrue(items.contains("/abcd/P755"));
        assertTrue(items.contains("/abcd/21_19"));
        assertTrue(items.contains("/abcd/Kunst__94"));
    }
}
