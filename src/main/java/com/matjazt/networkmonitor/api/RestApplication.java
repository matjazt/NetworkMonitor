package com.matjazt.networkmonitor.api;

import java.util.HashSet;
import java.util.Set;

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
