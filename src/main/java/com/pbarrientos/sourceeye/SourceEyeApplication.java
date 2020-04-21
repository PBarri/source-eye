package com.pbarrientos.sourceeye;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;
import com.pbarrientos.sourceeye.config.properties.SourceEyePropertiesValidator;

@EnableConfigurationProperties(SourceEyeProperties.class)
@SpringBootApplication
public class SourceEyeApplication {

    public static void main(final String[] args) {
        SpringApplication.run(SourceEyeApplication.class, args);
    }

    /**
     * Validates the properties for invalid configuration
     *
     * @return the Validator instance
     */
    @Bean
    public static Validator configurationPropertiesValidator() {
        return new SourceEyePropertiesValidator();
    }
}
