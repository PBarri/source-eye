package com.pbarrientos.sourceeye.config.properties;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class YamlPropertiesPostProcessor implements EnvironmentPostProcessor {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        Resource secrets = new ClassPathResource("application-secrets.yml");
        
        if (secrets.exists()) {
        	PropertySource<?> propertySource = this.loadYaml(secrets);
            environment.getPropertySources().addFirst(propertySource);
        }
    }

    private PropertySource<?> loadYaml(final Resource path) {
        if (!path.exists()) {
            throw new IllegalArgumentException("Resource " + path + " does not exist");
        }
        try {
            return this.loader.load("custom-resource", path).get(0);
        } catch (IOException ex) {
            throw new IllegalStateException(
                "Failed to load yaml configuration from " + path, ex);
        }
    }

}