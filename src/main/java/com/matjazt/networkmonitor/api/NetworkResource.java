package com.matjazt.networkmonitor.api;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.matjazt.networkmonitor.entity.DeviceStatusHistory;
import com.matjazt.networkmonitor.entity.Network;
import com.matjazt.networkmonitor.repository.DeviceStatusRepository;
import com.matjazt.networkmonitor.repository.NetworkRepository;

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
public class NetworkResource {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private DeviceStatusRepository deviceStatusRepository;

    /**
     * GET /api/networks
     * 
     * Returns a list of all monitored networks.
     * 
     * @GET indicates this handles HTTP GET requests.
     *      Return type is automatically serialized to JSON by JSON-B.
     */
    @GET
    public Response getNetworks() {
        List<Network> networks = networkRepository.findAll();

        // Convert entities to DTOs (Data Transfer Objects)
        // We don't expose entities directly to avoid over-fetching and
        // to keep API contract separate from database schema
        List<Map<String, Object>> networkDtos = networks.stream()
                .map(this::toNetworkDto)
                .toList();
        return Response.ok(networkDtos).build();
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
    public Response getOnlineDevices(@PathParam("networkName") String networkName) {
        // Find the network
        Optional<Network> networkOpt = networkRepository.findByName(networkName);

        if (networkOpt.isEmpty()) {
            // Return 404 Not Found if network doesn't exist
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Network not found: " + networkName))
                    .build();
        }

        Network network = networkOpt.get();

        // Get currently online devices
        List<DeviceStatusHistory> onlineDevices = deviceStatusRepository.findCurrentlyOnline(network);

        // Convert to DTOs
        List<Map<String, Object>> deviceDtos = onlineDevices.stream()
                .map(this::toDeviceDto)
                .toList();
        return Response.ok(deviceDtos).build();
    }

    /**
     * Convert Network entity to a simple DTO map.
     * 
     * Using Map<String, Object> is a simple approach for DTOs.
     * In larger applications, you might create dedicated DTO classes.
     */
    private Map<String, Object> toNetworkDto(Network network) {
        return Map.of(
                "id", network.getId(),
                "name", network.getName(),
                "firstSeen", network.getFirstSeen().format(ISO_FORMATTER),
                "lastSeen", network.getLastSeen().format(ISO_FORMATTER));
    }

    /**
     * Convert DeviceStatusHistory entity to DTO.
     */
    private Map<String, Object> toDeviceDto(DeviceStatusHistory device) {
        return Map.of(
                "macAddress", device.getMacAddress(),
                "ipAddress", device.getIpAddress(),
                "online", device.getOnline(),
                "timestamp", device.getTimestamp().format(ISO_FORMATTER));
    }
}
