package eu.europeana.api.recommend;

import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Main application. Allows deploying as a war and logs instance data when deployed in Cloud Foundry
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@SpringBootApplication
public class RecommendApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        // When deploying to Cloud Foundry, this will log the instance index number, IP and GUID
        LogManager.getLogger(RecommendApplication.class).
                info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                        System.getenv("CF_INSTANCE_INDEX"),
                        System.getenv("CF_INSTANCE_GUID"),
                        System.getenv("CF_INSTANCE_IP"));

        SpringApplication.run(RecommendApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RecommendApplication.class);
    }

}
