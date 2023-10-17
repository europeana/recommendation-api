package eu.europeana.api.recommend.util;

import org.codehaus.plexus.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Utility class for handling multiple languages
 */
public final class LangUtils {

    /**
     * Preferred order for selecting a value in a LanguageMap (most preferred language first)
     */
    public static final List<String> PREFERRED_LANGUAGES = List.of("en", "de", "fr", "pl", "it", "nl", "pt",
            "sp", "ru", "sv", "no", "fi", "ca", "ro", "cs", "hu", "sk", "da", "sl", "bg", "et", "hr", "el", "ga", "lv", "lt", "mt");

    private LangUtils() {
        // prevent constructor to prevent initialization
    }

    /**
     * Find the 'most' preferred language in the provided language map (map having single String values)
     * @param languageMap map to check
     * @return the most preferred language available, or null if nothing was found
     */
    public static String getMostPreferredLanguageString(Map<String, String> languageMap) {
        String result = null;
        if (languageMap.isEmpty()) {
            return result;
        }
        for (String lang : PREFERRED_LANGUAGES) {
            String value = languageMap.get(lang);
            if (!StringUtils.isEmpty(value)) {
                result = lang;
                break;
            }
        }
        return result;
    }

    /**
     * Find the 'most' preferred language in the provided language map (map having List<String> values)
     * @param languageMap map to check
     * @return the most preferred language available, or null if nothing was found
     */
    public static String getMostPreferredLanguageList(Map<String, List<String>> languageMap) {
        String result = null;
        if (languageMap.isEmpty()) {
            return result;
        }
        for (String lang : PREFERRED_LANGUAGES) {
            List<String> value = languageMap.get(lang);
            if (value != null && !value.isEmpty()) {
                result = lang;
                break;
            }
        }
        return result;
    }
}
