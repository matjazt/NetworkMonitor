package com.matjazt.networkmonitor.service;

import com.matjazt.networkmonitor.config.ConfigProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that manages MQTT connection and subscriptions.
 * 
 * @Singleton: Only one instance exists in the application.
 * @Startup: Instance is created when application starts (eager initialization).
 * 
 * This is similar to a hosted service in .NET Core that runs for the
 * application lifetime.
 */
@Singleton
@Startup
public class MqttService {

    private static final Logger LOGGER = Logger.getLogger(MqttService.class.getName());

    @Inject
    private ConfigProvider config;

    @Inject
    private MessageProcessingService messageProcessor;

    private MqttClient mqttClient;

    /**
     * Called automatically after dependency injection completes.
     * Similar to .NET's IHostedService.StartAsync().
     * 
     * @PostConstruct is a lifecycle callback - runs once during initialization.
     */
    @PostConstruct
    public void initialize() {
        try {
            LOGGER.info("Initializing MQTT connection...");
            
            // Create MQTT client
            // MemoryPersistence: messages stored in memory (lost on restart)
            mqttClient = new MqttClient(
                config.getMqttBrokerUrl(),
                config.getMqttClientId(),
                new MemoryPersistence()
            );

            // Configure connection options
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(config.getMqttUsername());
            options.setPassword(config.getMqttPassword().toCharArray());
            options.setConnectionTimeout(config.getMqttConnectionTimeout());
            options.setKeepAliveInterval(config.getMqttKeepaliveInterval());
            options.setCleanSession(config.getMqttCleanSession());
            options.setAutomaticReconnect(true);  // Auto-reconnect on connection loss

            // Configure TLS/SSL if using ssl:// protocol
            if (config.getMqttBrokerUrl().startsWith("ssl://")) {
                configureSsl(options);
            }

            // Set callback for incoming messages
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    LOGGER.log(Level.WARNING, "MQTT connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Delegate message processing to dedicated service
                    String payload = new String(message.getPayload());
                    LOGGER.fine(() -> String.format("Received message on topic %s: %s", topic, payload));
                    messageProcessor.processMessage(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used - we only subscribe, don't publish
                }
            });

            // Connect to broker
            LOGGER.info("Connecting to MQTT broker: " + config.getMqttBrokerUrl());
            mqttClient.connect(options);
            LOGGER.info("Connected to MQTT broker");

            // Subscribe to all configured topics
            List<String> topics = config.getMqttTopics();
            for (String topic : topics) {
                mqttClient.subscribe(topic, 1);  // QoS 1: at least once delivery
                LOGGER.info("Subscribed to topic: " + topic);
            }

        } catch (MqttException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize MQTT connection", e);
            throw new RuntimeException("MQTT initialization failed", e);
        }
    }

    /**
     * Configure SSL/TLS for secure MQTT connection.
     * 
     * If a custom truststore is configured, it's loaded and used.
     * Otherwise, the JVM's default truststore is used (contains common CA certs).
     */
    private void configureSsl(MqttConnectOptions options) throws MqttException {
        try {
            // If custom truststore specified, load it
            if (config.getMqttSslTruststorePath().isPresent()) {
                String truststorePath = config.getMqttSslTruststorePath().get();
                String truststorePassword = config.getMqttSslTruststorePassword().orElse("");

                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                try (FileInputStream fis = new FileInputStream(truststorePath)) {
                    trustStore.load(fis, truststorePassword.toCharArray());
                }

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);

                SSLSocketFactory socketFactory = sslContext.getSocketFactory();
                options.setSocketFactory(socketFactory);
            } else {
                // Use JVM's default truststore
                options.setSocketFactory(SSLSocketFactory.getDefault());
            }

            // Optionally disable hostname verification (not recommended for production)
            if (!config.getMqttSslVerifyHostname()) {
                options.setSSLHostnameVerifier((hostname, session) -> true);
            }

        } catch (Exception e) {
            throw new MqttException(e);
        }
    }

    /**
     * Called automatically when application is shutting down.
     * Similar to .NET's IHostedService.StopAsync().
     * 
     * @PreDestroy is a lifecycle callback for cleanup.
     */
    @PreDestroy
    public void cleanup() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                LOGGER.info("Disconnecting from MQTT broker...");
                mqttClient.disconnect();
                mqttClient.close();
                LOGGER.info("MQTT connection closed");
            } catch (MqttException e) {
                LOGGER.log(Level.WARNING, "Error during MQTT cleanup", e);
            }
        }
    }
}
