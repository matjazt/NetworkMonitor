package com.matjazt.networkmonitor.api;

import java.io.InputStream;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * ServletContextListener to initialize SmallRye OpenAPI on application startup.
 * 
 * This listener scans the application classes and generates the OpenAPI
 * document.
 */
@WebListener
public class OpenAPIBootstrap implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(OpenAPIBootstrap.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing SmallRye OpenAPI...");

        try {
            // Create Jandex index for scanning
            Indexer indexer = new Indexer();

            // Index the API classes
            indexClass(indexer, "com.matjazt.networkmonitor.api.NetworkResource");
            indexClass(indexer, "com.matjazt.networkmonitor.api.OpenAPIConfig");
            indexClass(indexer, "com.matjazt.networkmonitor.entity.Network");
            indexClass(indexer, "com.matjazt.networkmonitor.entity.DeviceStatusHistory");

            Index index = indexer.complete();

            // Configure OpenAPI
            OpenApiConfig config = new OpenApiConfigImpl(
                    org.eclipse.microprofile.config.ConfigProvider.getConfig());

            // Build OpenAPI document
            OpenApiDocument openApiDocument = OpenApiDocument.INSTANCE;
            openApiDocument.reset();
            openApiDocument.config(config);
            openApiDocument.modelFromReader(OpenApiProcessor.modelFromReader(config, getClass().getClassLoader()));
            openApiDocument.modelFromAnnotations(OpenApiProcessor.modelFromAnnotations(config, index));
            openApiDocument.filter(OpenApiProcessor.getFilter(config, getClass().getClassLoader()));
            openApiDocument.initialize();

            logger.info("SmallRye OpenAPI initialized successfully");
            logger.info("OpenAPI spec available at: /api/openapi");
            logger.info("Swagger UI available at: /api/swagger-ui");

        } catch (Exception e) {
            logger.error("Failed to initialize SmallRye OpenAPI", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Shutting down SmallRye OpenAPI...");
        OpenApiDocument.INSTANCE.reset();
    }

    private void indexClass(Indexer indexer, String className) {
        try {
            String resourceName = className.replace('.', '/') + ".class";
            InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName);
            if (stream != null) {
                indexer.index(stream);
                stream.close();
            } else {
                logger.warn("Could not find class for indexing: {}", className);
            }
        } catch (Exception e) {
            logger.warn("Failed to index class: {}", className, e);
        }
    }
}
