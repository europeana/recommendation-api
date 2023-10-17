package eu.europeana.api.recommend.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LangUtilsTest {

    @Test
    public void getMostPreferredLanguageTest() {
        Map<String, String> langMap = new HashMap<>();
        langMap.put("hu", "Ez egy magyar mondat");
        langMap.put("de", "Dies ist ein deutscher Satz");
        langMap.put("nl", "Dit is een Nederlandse zin");
        assertEquals("de", LangUtils.getMostPreferredLanguageString(langMap));

        langMap.put("en", "This is an English sentence");
        assertEquals("en", LangUtils.getMostPreferredLanguageString(langMap));
    }

    @Test
    public void getMostPreferredLanguageListTest() {
        Map<String, List<String>> langMap = new HashMap<>();
        langMap.put("mt", List.of("Din hija frażi bil-Malti"));
        langMap.put("lv", List.of("Tas ir latviešu teikums"));
        assertEquals("lv", LangUtils.getMostPreferredLanguageList(langMap));

        langMap.put("it", List.of("Questa è una frase italiana"));
        assertEquals("it", LangUtils.getMostPreferredLanguageList(langMap));
    }
}
