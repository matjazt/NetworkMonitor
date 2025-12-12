package com.matjazt.networkmonitor.api;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application configuration.
 * 
 * This class activates JAX-RS and sets the base path for all REST endpoints.
 * With @ApplicationPath("/"), all resources will be available under their @Path
 * annotations (e.g., /api/networks).
 * 
 * Similar to app.MapControllers() in .NET, this registers the REST API.
 */
@ApplicationPath("/")
/**
 * OpenAPI configuration and metadata for the Network Monitor API.
 */
@OpenAPIDefinition(info = @Info(title = "Network Monitor API", version = "1.0.0", description = "MQTT-based network device monitoring with REST API"), servers = {
        @Server(url = "/network-monitor")
})
public class RestApplication extends Application {
    /**
     * Returns the set of REST resource classes to register.
     * Without this, TomEE won't discover your @Path annotated classes.
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Register all REST endpoints
        classes.add(NetworkResource.class);
        return classes;
    }
}
