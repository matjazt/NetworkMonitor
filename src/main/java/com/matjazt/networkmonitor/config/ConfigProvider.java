package com.matjazt.networkmonitor.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.List;
import java.util.Optional;

/**
 * Central configuration provider using MicroProfile Config.
 * 
 * MicroProfile Config is a Jakarta EE standard for externalizing configuration.
 * It reads from multiple sources in this priority order:
 * 1. System properties (-Dproperty=value)
 * 2. Environment variables
 * 3. microprofile-config.properties file
 * 
 * This is similar to .NET's IConfiguration but follows Java EE conventions.
 */
@ApplicationScoped  // Single instance for entire application lifecycle
public class ConfigProvider {

    // @Inject with @ConfigProperty auto-loads values from config sources
    // Similar to .NET's [FromConfiguration] attribute
    
    @Inject
    @ConfigProperty(name = "mqtt.broker.url")
    private String mqttBrokerUrl;

    @Inject
    @ConfigProperty(name = "mqtt.client.id")
    private String mqttClientId;

    @Inject
    @ConfigProperty(name = "mqtt.username")
    private String mqttUsername;

    @Inject
    @ConfigProperty(name = "mqtt.password")
    private String mqttPassword;

    @Inject
    @ConfigProperty(name = "mqtt.topics")
    private String mqttTopics;  // Comma-separated string

    @Inject
    @ConfigProperty(name = "mqtt.connection.timeout", defaultValue = "30")
    private Integer mqttConnectionTimeout;

    @Inject
    @ConfigProperty(name = "mqtt.keepalive.interval", defaultValue = "60")
    private Integer mqttKeepaliveInterval;

    @Inject
    @ConfigProperty(name = "mqtt.clean.session", defaultValue = "false")
    private Boolean mqttCleanSession;

    @Inject
    @ConfigProperty(name = "mqtt.ssl.truststore.path")
    private Optional<String> mqttSslTruststorePath;

    @Inject
    @ConfigProperty(name = "mqtt.ssl.truststore.password")
    private Optional<String> mqttSslTruststorePassword;

    @Inject
    @ConfigProperty(name = "mqtt.ssl.verify.hostname", defaultValue = "true")
    private Boolean mqttSslVerifyHostname;

  
    @Inject
    @ConfigProperty(name = "app.log.level", defaultValue = "INFO")
    private String logLevel;

    // Inject the Config object for dynamic lookups
    @Inject
    private Config config;

    // Getters - standard Java bean pattern
    
    public String getMqttBrokerUrl() {
        return mqttBrokerUrl;
    }

    public String getMqttClientId() {
        return mqttClientId;
    }

    public String getMqttUsername() {
        return mqttUsername;
    }

    public String getMqttPassword() {
        return mqttPassword;
    }

    /**
     * Returns the list of MQTT topics to subscribe to.
     * Each topic typically represents a different network to monitor.
     */
    public List<String> getMqttTopics() {
        return List.of(mqttTopics.split(",\\s*"));  // Split by comma, trim whitespace
    }

    public Integer getMqttConnectionTimeout() {
        return mqttConnectionTimeout;
    }

    public Integer getMqttKeepaliveInterval() {
        return mqttKeepaliveInterval;
    }

    public Boolean getMqttCleanSession() {
        return mqttCleanSession;
    }

    public Optional<String> getMqttSslTruststorePath() {
        return mqttSslTruststorePath;
    }

    public Optional<String> getMqttSslTruststorePassword() {
        return mqttSslTruststorePassword;
    }

    public Boolean getMqttSslVerifyHostname() {
        return mqttSslVerifyHostname;
    }


    public String getLogLevel() {
        return logLevel;
    }
}
