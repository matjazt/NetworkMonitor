package com.matjazt.networkmonitor.api;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.matjazt.networkmonitor.dao.MonitoringDAO;
import com.matjazt.networkmonitor.entity.DeviceStatusHistoryEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;
import com.matjazt.networkmonitor.security.AccountPrincipal;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API endpoint for accessing network monitoring data.
 * 
 * JAX-RS (Jakarta RESTful Web Services) is the standard for building REST APIs
 * in Jakarta EE. Similar to ASP.NET Web API or Minimal APIs in .NET.
 * 
 * @Path defines the base URL path for this resource.
 *       All methods in this class will be under /api/networks
 */
@Path("/api/networks")
@Produces(MediaType.APPLICATION_JSON) // All responses are JSON by default
@Consumes(MediaType.APPLICATION_JSON) // All requests accept JSON by default
@Tag(name = "Networks", description = "Network and device monitoring operations")
public class NetworkResource {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Inject
    private MonitoringDAO monitoringDao;

    @jakarta.ws.rs.core.Context
    private jakarta.ws.rs.core.SecurityContext securityContext;

    /**
     * GET /api/networks
     * 
     * Returns a list of all monitored networks.
     * 
     * @GET indicates this handles HTTP GET requests.
     *      Return type is automatically serialized to JSON by JSON-B.
     */
    @GET
    @Operation(summary = "Get all networks", description = "Retrieves a list of all monitored networks with their details")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully retrieved networks", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response getNetworks() {
        List<NetworkEntity> networks = monitoringDao.findAll();

        // Convert entities to DTOs (Data Transfer Objects)
        // We don't expose entities directly to avoid over-fetching and
        // to keep API contract separate from database schema
        List<Map<String, Object>> networkDtos = networks.stream()
                .map(this::toNetworkDto)
                .toList();

        // Get account information from SecurityContext
        Map<String, Object> response = Map.of(
                "accountId", getAccountId(),
                "accountFullName", getAccountFullName(),
                "networks", networkDtos);

        return Response.ok(response).build();
    }

    /**
     * GET /api/networks/{networkName}/devices
     * 
     * Returns currently online devices for a specific network.
     * 
     * @PathParam extracts the {networkName} from the URL path.
     *            Example: GET /api/networks/MaliGrdi/devices
     */
    @GET
    @Path("/{networkName}/devices")
    @Operation(summary = "Get online devices for a network", description = "Retrieves all currently online devices for the specified network")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully retrieved online devices", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(responseCode = "404", description = "Network not found", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response getOnlineDevices(
            @Parameter(description = "Name of the network", required = true, example = "MaliGrdi") @PathParam("networkName") String networkName) {
        // Find the network
        Optional<NetworkEntity> networkOpt = monitoringDao.findByName(networkName);

        if (networkOpt.isEmpty()) {
            // Return 404 Not Found if network doesn't exist
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Network not found: " + networkName))
                    .build();
        }

        NetworkEntity network = networkOpt.get();

        // Get currently online devices
        List<DeviceStatusHistoryEntity> onlineDevices = monitoringDao.findCurrentlyOnline(network);

        // Convert to DTOs
        List<Map<String, Object>> deviceDtos = onlineDevices.stream()
                .map(this::toDeviceDto)
                .toList();

        // Get account information from SecurityContext
        Map<String, Object> response = Map.of(
                "accountId", getAccountId(),
                "accountFullName", getAccountFullName(),
                "networkName", networkName,
                "devices", deviceDtos);

        return Response.ok(response).build();
    }

    /**
     * Convert Network entity to a simple DTO map.
     * 
     * Using Map<String, Object> is a simple approach for DTOs.
     * In larger applications, you might create dedicated DTO classes.
     */
    private Map<String, Object> toNetworkDto(NetworkEntity network) {
        return Map.of(
                "id", network.getId(),
                "name", network.getName(),
                "firstSeen", network.getFirstSeen().format(ISO_FORMATTER),
                "lastSeen", network.getLastSeen().format(ISO_FORMATTER));
    }

    /**
     * Convert DeviceStatusHistory entity to DTO.
     */
    private Map<String, Object> toDeviceDto(DeviceStatusHistoryEntity historyEntry) {
        return Map.of(
                "macAddress", historyEntry.getDevice().getMacAddress(),
                "ipAddress", historyEntry.getIpAddress(),
                "online", historyEntry.getOnline(),
                "timestamp", historyEntry.getTimestamp().format(ISO_FORMATTER));
    }

    /**
     * Extract account ID from the SecurityContext.
     */
    private Long getAccountId() {
        if (securityContext != null && securityContext.getUserPrincipal() instanceof AccountPrincipal) {
            AccountPrincipal principal = (AccountPrincipal) securityContext.getUserPrincipal();
            return principal.getAccountId();
        }
        return null;
    }

    /**
     * Extract account full name from the SecurityContext.
     */
    private String getAccountFullName() {
        if (securityContext != null && securityContext.getUserPrincipal() instanceof AccountPrincipal) {
            AccountPrincipal principal = (AccountPrincipal) securityContext.getUserPrincipal();
            return principal.getFullName();
        }
        return "Anonymous";
    }
}
