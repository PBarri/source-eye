package com.pbarrientos.sourceeye.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Configuration class that configures the Swagger implementation of the project. This configuration can be aborted if
 * the user sets the property 'sourceeye.api.enabled' to 'false'.
 *
 * @author Pablo Barrientos
 */
@Configuration
@EnableSwagger2
@ConditionalOnProperty(name = "sourceeye.api.expose", matchIfMissing = true)
public class SwaggerConfiguration {

    /**
     * Generates the API of Swagger from the base package 'com.pbarrientos.sourceeye.api' and the information provided.
     *
     * @return the API
     * @since 0.1.0
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("com.pbarrientos.sourceeye.api")).paths(PathSelectors.any())
                .build().apiInfo(this.apiInfo());
    }

    /**
     * Generates information for the API to show.
     *
     * @return the information
     * @since 0.1.0
     */
    private ApiInfo apiInfo() {
        ApiInfoBuilder builder = new ApiInfoBuilder();
        builder.title("Source Eye API").description("https://gitlab.com/SourceEye")
                .contact(new Contact("Pablo Barrientos", "linkedin", "pablo.barrientos.13@gmail.com"))
                .license("GNU General Public License v3.0")
                .licenseUrl("https://gitlab.com/SourceEye/source-eye/raw/master/LICENSE");

        return builder.build();
    }

}
