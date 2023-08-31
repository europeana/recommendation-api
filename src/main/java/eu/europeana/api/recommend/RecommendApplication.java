package eu.europeana.api.recommend;

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
        SpringApplication.run(RecommendApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RecommendApplication.class);
    }

}
