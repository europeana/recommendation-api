package eu.europeana.api.recommend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.util.Collections;

/**
 * Configures swagger on all requests. Swagger Json file is availabe at <hostname>/v2/api-docs
 * Note that this only works when deployed as a war
 *
 * @author Patrick Ehlert
 * Created on 22 Jul 2020
 */
@Configuration
@EnableSwagger2WebMvc
public class SwaggerConfig {

    private BuildInfo buildInfo;

    public SwaggerConfig(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("eu.europeana.api"))
                .paths(PathSelectors.any())
                .build();
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private ApiInfo apiInfo() {
        return new ApiInfo(
                buildInfo.getAppName(),
                buildInfo.getAppDescription(),
                buildInfo.getAppVersion(),
                null,
                new Contact("APIs team", "www.europeana.eu", "api@europeana.eu"),
                "EUPL 1.2", "API license URL", Collections.emptyList());
    }
}
