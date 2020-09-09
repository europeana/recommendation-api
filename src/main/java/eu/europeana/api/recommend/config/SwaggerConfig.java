package eu.europeana.api.recommend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

/**
 * Configures swagger on all requests. Swagger Json file is availabe at <hostname>/v2/api-docs
 * Note that this only works when deployed as a war
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private BuildInfo buildInfo;

    public SwaggerConfig(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("eu.europeana.api"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                buildInfo.getAppName(),
                buildInfo.getAppDescription(),
                buildInfo.getAppVersion(),
                null,
                new Contact("API team", "https://api.europeana.eu", "api@europeana.eu"),
                "EUPL 1.2", "https://www.eupl.eu", Collections.emptyList());
    }

    /**
     * For some reason the default Spring-Boot way of configuring Cors using the CorsFilter in WebMvcConfig class doesn't
     * work, so we configure it separately for Swagger here (solution copied from https://stackoverflow.com/a/45685909)
     */
    @Bean
    public CorsFilter corsFilterSwagger() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.setMaxAge(1000L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/v2/api-docs", config);
        return new CorsFilter(source);
    }
}
