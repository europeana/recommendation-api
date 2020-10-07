package eu.europeana.api.recommend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;

/**
 * Setup CORS for all requests
 *
 * @author Patrick Ehlert
 * Created on 23 Jul 2020
 */
@Configuration
public class WebRequestConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * Enable content negotiation via path extension.
             */
            @Override
            public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
                //Note that favorPathExtension() is deprecated and will be phased out by Spring. This means that when we upgrade
                // Spring later it may stop working, see also
                // https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-requestmapping-suffix-pattern-match
                configurer.favorPathExtension(true);
            }
        };
    }


    /**
     * For some reason the default Spring-Boot way of configuring Cors using the CorsFilter in WebMvcConfigurer class doesn't
     * work for Swagger, so we configure it here (solution copied from https://stackoverflow.com/a/45685909)
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowedOrigins(Collections.singletonList("*"));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setMaxAge(1000L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
