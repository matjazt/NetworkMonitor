package com.matjazt.networkmonitor.api;

import java.io.InputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * JAX-RS resource for serving Swagger UI.
 * 
 * Access Swagger UI at: http://localhost:8080/network-monitor/api/swagger-ui
 */
@Path("/swagger-ui")
public class SwaggerUIServlet {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getSwaggerUI() {
        try {
            InputStream is = getClass().getResourceAsStream("/swagger-ui.html");
            if (is != null) {
                return Response.ok(is).build();
            }
            // Return a simple redirect page if the resource isn't found
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>Network Monitor API - OpenAPI Documentation</title>
                        <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui.css">
                        <style>
                            html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }
                            *, *:before, *:after { box-sizing: inherit; }
                            body { margin: 0; padding: 0; }
                        </style>
                    </head>
                    <body>
                    <div id="swagger-ui"></div>
                    <script src="https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui-bundle.js"></script>
                    <script src="https://unpkg.com/swagger-ui-dist@5.10.5/swagger-ui-standalone-preset.js"></script>
                    <script>
                    window.onload = function() {
                      const ui = SwaggerUIBundle({
                        url: "../openapi",
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        layout: "StandaloneLayout"
                      });
                    };
                    </script>
                    </body>
                    </html>
                    """;
            return Response.ok(html).build();
        } catch (Exception e) {
            return Response.serverError().entity("Error loading Swagger UI: " + e.getMessage()).build();
        }
    }
}
