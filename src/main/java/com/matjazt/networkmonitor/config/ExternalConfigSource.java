package com.matjazt.networkmonitor.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom MicroProfile ConfigSource that reads from an external properties file
 * in the TomEE conf directory.
 * 
 * File location: ${catalina.base}/conf/network-monitor.properties
 * 
 * This allows environment-specific configuration (passwords, URLs, etc.) to be
 * stored outside the WAR file and not in source control.
 * 
 * Priority: 500 (higher than default 100, so it overrides bundled properties)
 */
public class ExternalConfigSource implements ConfigSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalConfigSource.class);

    private static final String CONFIG_FILE_NAME = "network-monitor.properties";
    private static final int ORDINAL = 500; // Higher than default sources

    private final Map<String, String> properties = new HashMap<>();
    private final String configFilePath;

    public ExternalConfigSource() {
        // Determine config file path using catalina.base
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase == null) {
            catalinaBase = System.getProperty("catalina.home");
        }

        if (catalinaBase != null) {
            configFilePath = Paths.get(catalinaBase, "conf", CONFIG_FILE_NAME).toString();
            loadProperties();
        } else {
            configFilePath = null;
            LOGGER.warn("catalina.base not set, external config will not be loaded");
        }
    }

    private void loadProperties() {
        Path path = Paths.get(configFilePath);

        if (!Files.exists(path)) {
            LOGGER.info("External config file not found: {} (this is optional)", configFilePath);
            return;
        }

        try (InputStream input = new FileInputStream(path.toFile())) {
            Properties props = new Properties();
            props.load(input);

            // Convert Properties to Map<String, String>
            for (String key : props.stringPropertyNames()) {
                properties.put(key, props.getProperty(key));
            }

            LOGGER.info("Loaded {} properties from: {}", properties.size(), configFilePath);

        } catch (IOException e) {
            LOGGER.warn("Failed to load external config from {}: {}", configFilePath, e.getMessage());
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>(properties);
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "ExternalConfigSource[" + configFilePath + "]";
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }
}
