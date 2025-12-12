package com.matjazt.networkmonitor.api;

import org.eclipse.microprofile.openapi.annotations.Operation;

import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * JAX-RS resource for serving the OpenAPI specification.
 * 
 * This endpoint provides the OpenAPI/Swagger specification in JSON or YAML
 * format.
 * Access at: http://localhost:8080/network-monitor/api/openapi
 */
@Path("/openapi")
public class OpenAPIResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(hidden = true) // Don't show this endpoint in the OpenAPI docs
    public Response getOpenApiJson() {
        try {
            // Get the OpenAPI document from SmallRye
            OpenApiDocument openApiDocument = OpenApiDocument.INSTANCE;

            if (openApiDocument.get() != null) {
                String json = OpenApiSerializer.serialize(openApiDocument.get(), Format.JSON);
                return Response.ok(json).build();
            }

            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"OpenAPI document not available\"}")
                    .build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/yaml")
    @Produces("application/yaml")
    @Operation(hidden = true)
    public Response getOpenApiYaml() {
        try {
            OpenApiDocument openApiDocument = OpenApiDocument.INSTANCE;

            if (openApiDocument.get() != null) {
                String yaml = OpenApiSerializer.serialize(openApiDocument.get(), Format.YAML);
                return Response.ok(yaml).build();
            }

            return Response.status(Response.Status.NOT_FOUND)
                    .entity("error: OpenAPI document not available")
                    .build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("error: " + e.getMessage())
                    .build();
        }
    }
}
