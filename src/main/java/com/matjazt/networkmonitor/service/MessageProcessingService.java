package com.matjazt.networkmonitor.service;

import com.matjazt.networkmonitor.entity.DeviceStatusHistory;
import com.matjazt.networkmonitor.entity.Network;
import com.matjazt.networkmonitor.model.NetworkStatusMessage;
import com.matjazt.networkmonitor.repository.DeviceStatusRepository;
import com.matjazt.networkmonitor.repository.NetworkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that processes incoming MQTT messages and tracks device state changes.
 * 
 * This is the core business logic of the application:
 * 1. Parse JSON from MQTT messages
 * 2. Track which devices are online
 * 3. Detect state changes (online -> offline, offline -> online)
 * 4. Store only the changes to database
 */
@ApplicationScoped
public class MessageProcessingService {

    private static final Logger LOGGER = Logger.getLogger(MessageProcessingService.class.getName());
    
    // DateTimeFormatter for parsing timestamps from MQTT messages
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private DeviceStatusRepository deviceStatusRepository;

    /**
     * Process an incoming MQTT message.
     * 
     * @param topic The MQTT topic (e.g., "network/MaliGrdi")
     * @param payload The JSON payload as string
     */
    @Transactional  // All database operations in one transaction
    public void processMessage(String topic, String payload) {
        try {
            // Extract network name from topic
            // For "network/MaliGrdi" -> "MaliGrdi"
            String networkName = extractNetworkName(topic);
            
            // Parse JSON payload to Java object using JSON-B
            NetworkStatusMessage message = parseMessage(payload);
            
            // Get or create network record
            Network network = getOrCreateNetwork(networkName);
            network.updateLastSeen();
            networkRepository.save(network);
            
            // Get list of currently online MACs from message
            Set<String> currentlyOnlineMacs = new HashSet<>();
            for (NetworkStatusMessage.DeviceInfo device : message.getDevices()) {
                currentlyOnlineMacs.add(device.getMac());
            }
            
            // Parse message timestamp
            LocalDateTime messageTimestamp = LocalDateTime.parse(
                message.getTimestamp(), TIMESTAMP_FORMATTER);
            
            // Process each device in the message (all are online)
            for (NetworkStatusMessage.DeviceInfo device : message.getDevices()) {
                processDeviceOnline(network, device, messageTimestamp);
            }
            
            // Check for devices that went offline
            // (were online before but not in current message)
            processDevicesOffline(network, currentlyOnlineMacs, messageTimestamp);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing MQTT message from topic: " + topic, e);
        }
    }

    /**
     * Extract network name from MQTT topic.
     * Takes the last part after the last '/'.
     */
    private String extractNetworkName(String topic) {
        int lastSlash = topic.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < topic.length() - 1) {
            return topic.substring(lastSlash + 1);
        }
        return topic;  // Fallback if no slash found
    }

    /**
     * Parse JSON string to NetworkStatusMessage object.
     * 
     * JSON-B (Jakarta JSON Binding) is the standard JSON library in Jakarta EE.
     * Similar to System.Text.Json in .NET.
     */
    private NetworkStatusMessage parseMessage(String payload) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(payload, NetworkStatusMessage.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON message", e);
        }
    }

    /**
     * Get existing network or create a new one.
     */
    private Network getOrCreateNetwork(String networkName) {
        return networkRepository.findByName(networkName)
            .orElseGet(() -> {
                Network newNetwork = new Network(networkName);
                return networkRepository.save(newNetwork);
            });
    }

    /**
     * Process a device that is currently online.
     * Only creates a record if the device was previously offline or never seen.
     */
    private void processDeviceOnline(Network network, NetworkStatusMessage.DeviceInfo device,
                                     LocalDateTime timestamp) {
        String mac = device.getMac();
        String ip = device.getIp();
        
        // Check the device's last known status
        Optional<DeviceStatusHistory> lastStatus = 
            deviceStatusRepository.findLatestStatus(network, mac);
        
        // Determine if we need to record a state change
        boolean shouldRecord = false;
        
        if (lastStatus.isEmpty()) {
            // First time seeing this device
            shouldRecord = true;
            LOGGER.fine(() -> String.format("New device: %s (%s) on %s", 
                mac, ip, network.getName()));
        } else if (!lastStatus.get().getOnline()) {
            // Device was offline, now online
            shouldRecord = true;
            LOGGER.info(String.format("Device came online: %s (%s) on %s", 
                mac, ip, network.getName()));
        }
        // else: device was already online, no change, don't record
        
        if (shouldRecord) {
            DeviceStatusHistory status = new DeviceStatusHistory(
                network, mac, ip, true, timestamp);
            deviceStatusRepository.save(status);
        }
    }

    /**
     * Check for devices that went offline.
     * 
     * This finds devices that have an "online" status in the database
     * but are not in the current message's device list.
     */
    private void processDevicesOffline(Network network, Set<String> currentlyOnlineMacs,
                                       LocalDateTime timestamp) {
        // Get all devices that were online
        List<DeviceStatusHistory> previouslyOnline = 
            deviceStatusRepository.findCurrentlyOnline(network);
        
        for (DeviceStatusHistory prevStatus : previouslyOnline) {
            String mac = prevStatus.getMacAddress();
            
            // If device is not in current message, it went offline
            if (!currentlyOnlineMacs.contains(mac)) {
                LOGGER.info(String.format("Device went offline: %s (%s) on %s",
                    mac, prevStatus.getIpAddress(), network.getName()));
                
                // Record offline status with last known IP
                DeviceStatusHistory offlineStatus = new DeviceStatusHistory(
                    network, mac, prevStatus.getIpAddress(), false, timestamp);
                deviceStatusRepository.save(offlineStatus);
            }
        }
    }
}
