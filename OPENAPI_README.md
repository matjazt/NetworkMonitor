# OpenAPI / Swagger Documentation

## Overview

Your Network Monitor application now includes **MicroProfile OpenAPI** with **SmallRye OpenAPI** implementation, providing interactive API documentation similar to ASP.NET's Swagger integration.

## Accessing the API Documentation

Once the application is deployed and running on TomEE, you can access:

### 1. Swagger UI (Interactive Documentation)

```
http://localhost:8080/network-monitor/api/swagger-ui
```

- Interactive web interface to test your API endpoints
- Similar to the Swagger UI you see in ASP.NET projects
- Try out API calls directly from the browser

### 2. OpenAPI Specification (JSON)

```
http://localhost:8080/network-monitor/api/openapi
```

- Raw OpenAPI 3.x specification in JSON format
- Can be imported into Postman, Insomnia, or other API clients

### 3. OpenAPI Specification (YAML)

```
http://localhost:8080/network-monitor/api/openapi/yaml
```

- Raw OpenAPI 3.x specification in YAML format

## Available Endpoints

The following REST API endpoints are documented:

### Networks API

- **GET** `/api/networks` - Get all monitored networks
- **GET** `/api/networks/{networkName}/devices` - Get online devices for a network

## Features

### What's Included

1. **@Tag** - Groups endpoints in the UI (e.g., "Networks")
2. **@Operation** - Describes what each endpoint does
3. **@APIResponse** - Documents response codes (200, 404, etc.)
4. **@Parameter** - Describes path parameters with examples
5. **OpenAPIDefinition** - Application-level metadata (title, version, description)

### Example Annotations in Code

```java
@Tag(name = "Networks", description = "Network and device monitoring operations")
@Path("/api/networks")
public class NetworkResource {

    @GET
    @Operation(
        summary = "Get all networks",
        description = "Retrieves a list of all monitored networks with their details"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Successfully retrieved networks",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
        )
    })
    public Response getNetworks() {
        // ...
    }
}
```

## Dependencies Added

The following dependencies were added to `pom.xml`:

```xml
<!-- MicroProfile OpenAPI API -->
<dependency>
    <groupId>org.eclipse.microprofile.openapi</groupId>
    <artifactId>microprofile-openapi-api</artifactId>
    <version>3.1.1</version>
    <scope>provided</scope>
</dependency>

<!-- SmallRye OpenAPI Implementation -->
<dependency>
    <groupId>io.smallrye</groupId>
    <artifactId>smallrye-open-api-core</artifactId>
    <version>3.10.0</version>
</dependency>
```

## Components

### Core Files

1. **OpenAPIConfig.java** - Application-level OpenAPI configuration with metadata
2. **NetworkResource.java** - REST endpoints with OpenAPI annotations
3. **OpenAPIBootstrap.java** - ServletContextListener that initializes SmallRye on startup
4. **OpenAPIResource.java** - Serves the OpenAPI spec in JSON/YAML
5. **SwaggerUIServlet.java** - Serves the Swagger UI HTML interface

### Configuration

**src/main/webapp/META-INF/microprofile-config.properties**:

```properties
mp.openapi.scan.disable=false
mp.openapi.extensions.smallrye.openapi=3.1.0
mp.openapi.extensions.smallrye.info.title=Network Monitor API
mp.openapi.extensions.smallrye.info.version=1.0.0
```

## How It Works

1. **On Application Startup**: `OpenAPIBootstrap` listener runs
2. **Class Scanning**: Uses Jandex to index your REST resource classes
3. **Annotation Processing**: SmallRye reads OpenAPI annotations
4. **Document Generation**: Creates OpenAPI 3.x specification
5. **Runtime Serving**: OpenAPI spec and Swagger UI available via JAX-RS endpoints

## Comparison to ASP.NET Swagger

| Feature | ASP.NET | Jakarta EE |
|---------|---------|------------|
| Annotations | `[SwaggerOperation]` | `@Operation` |
| Response Docs | `[ProducesResponseType]` | `@APIResponse` |
| Groups/Tags | `[ApiExplorerSettings]` | `@Tag` |
| Parameters | `[FromRoute]` docs | `@Parameter` |
| Configuration | `builder.Services.AddSwaggerGen()` | `@OpenAPIDefinition` |
| UI Access | `/swagger/index.html` | `/api/swagger-ui` |
| Spec Access | `/swagger/v1/swagger.json` | `/api/openapi` |

## Testing the Setup

1. **Build and deploy**:

   ```powershell
   mvn clean package
   .\runApp.ps1
   ```

2. **Wait for TomEE to start** (watch for "Server startup" in console)

3. **Open Swagger UI**:

   ```
   http://localhost:8080/network-monitor/api/swagger-ui
   ```

4. **Try an endpoint**:
   - Click on "GET /api/networks"
   - Click "Try it out"
   - Click "Execute"
   - See the response with live data from your database

## Troubleshooting

### Swagger UI not loading?

- Check that TomEE is fully started
- Verify application deployed: `http://localhost:8080/manager/html`
- Check TomEE logs in `apache-tomee-webprofile-10.0.0-M3\logs\catalina.out`

### OpenAPI spec returning 404?

- Ensure `OpenAPIBootstrap` ran (check logs for "Initializing SmallRye OpenAPI")
- Verify SmallRye dependencies are in the WAR file
- Check `target/network-monitor/WEB-INF/lib` for `smallrye-open-api-core-*.jar`

### Endpoints not appearing in Swagger?

- Ensure your REST resources have `@Tag`, `@Operation` annotations
- Check that classes are being indexed in `OpenAPIBootstrap.indexClass()`
- Add more classes to indexing if you add new REST resources

## Next Steps

To document additional endpoints:

1. Add `@Operation`, `@APIResponse` to your methods
2. Use `@Parameter` for path/query params
3. Create DTO schema classes with `@Schema` for complex types
4. Add authentication docs with `@SecurityScheme` if needed

## References

- [MicroProfile OpenAPI Spec](https://download.eclipse.org/microprofile/microprofile-open-api-3.1/microprofile-openapi-spec-3.1.html)
- [SmallRye OpenAPI Documentation](https://github.com/smallrye/smallrye-open-api)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
