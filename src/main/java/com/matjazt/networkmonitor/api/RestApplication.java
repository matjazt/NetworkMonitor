package com.matjazt.networkmonitor.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application configuration.
 * 
 * This class activates JAX-RS and sets the base path for all REST endpoints.
 * With @ApplicationPath("/"), all resources will be available under their
 * @Path annotations (e.g., /api/networks).
 * 
 * Similar to app.MapControllers() in .NET, this registers the REST API.
 */
@ApplicationPath("/")
public class RestApplication extends Application {
    // No methods needed - the class itself activates JAX-RS
    // TomEE automatically discovers all @Path annotated classes
}
