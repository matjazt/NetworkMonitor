package com.matjazt.networkmonitor.api;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * OpenAPI configuration for the Network Monitor REST API.
 * 
 * MicroProfile OpenAPI provides OpenAPI/Swagger documentation similar to
 * ASP.NET's Swagger integration.
 * 
 * The OpenAPI UI will be available at:
 * - OpenAPI spec (JSON): http://localhost:8080/openapi
 * - Swagger UI: http://localhost:8080/openapi-ui/
 */
@ApplicationPath("/")
@OpenAPIDefinition(info = @Info(title = "Network Monitor API", version = "1.0.0", description = "REST API for monitoring network devices via MQTT", contact = @Contact(name = "Network Monitor Team", email = "support@example.com"), license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")), servers = {
        @Server(url = "http://localhost:8080", description = "Development Server")
})
public class OpenAPIConfig extends Application {
    // JAX-RS application configuration
    // By extending Application, we can customize the REST application
}
