package eu.europeana.api.recommend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Setup CORS for all requests
 *
 * @author Patrick Ehlert
 * Created on 23 Jul 2020
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Setup CORS for all requests.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*").maxAge(1000);
    }

    /**
     * Enable content negotiation via path extension.
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        //Note that favorPathExtension() is deprecated and will be phased out by Spring. This means that when we upgrade
        // Spring later it may stop working (see also https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-requestmapping-suffix-pattern-match
        configurer.favorPathExtension(true);
    }

}
