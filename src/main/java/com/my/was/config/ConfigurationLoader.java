package com.my.was.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class ConfigurationLoader {

    private static Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);

    public static Configuration loadConfig(String configFileName) {

        logger.info("[ConfigurationLoader] configFileName=[{}]", configFileName);

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = ConfigurationLoader.class.getClassLoader().getResourceAsStream(configFileName)) {
            if (inputStream == null) {
                throw new RuntimeException("Configuration file not found: " + configFileName);
            }
            return mapper.readValue(inputStream, Configuration.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
}
