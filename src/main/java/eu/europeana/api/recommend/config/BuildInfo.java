package eu.europeana.api.recommend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Makes the information from the project's pom.xml available. While generating a war file this data is written
 * automatically to the build.properties file which is read here.
 * Note that this only works when deployed as a war
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@Configuration
@PropertySource("classpath:build.properties")
public class BuildInfo {

    @Value("${info.app.name:Recommend API}")
    private String appName;

    @Value("${info.app.version:unknown}")
    private String appVersion;

    @Value("${info.app.description:unknown}")
    private String appDescription;

    @Value("${info.build.number:unknown}")
    private String buildNumber;

    public String getAppName() {
        return appName;
    }

    public String getAppDescription() {
        return appDescription;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getBuildNumber() {
        return buildNumber;
    }
}
