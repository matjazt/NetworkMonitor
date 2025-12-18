package com.matjazt.networkmonitor.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matjazt.networkmonitor.entity.DeviceEntity;
import com.matjazt.networkmonitor.entity.DeviceOperationMode;
import com.matjazt.networkmonitor.entity.DeviceStatusHistoryEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;
import com.matjazt.networkmonitor.model.NetworkStatusMessage;
import com.matjazt.networkmonitor.repository.DeviceRepository;
import com.matjazt.networkmonitor.repository.DeviceStatusRepository;
import com.matjazt.networkmonitor.repository.NetworkRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.transaction.Transactional;

/**
 * Service that processes incoming MQTT messages and tracks device state
 * changes.
 * 
 * This is the core business logic of the application:
 * 1. Parse JSON from MQTT messages
 * 2. Track which devices are online
 * 3. Detect state changes (online -> offline, offline -> online)
 * 4. Store only the changes to database
 */
@ApplicationScoped
public class MessageProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessingService.class);

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private DeviceRepository deviceRepository;

    @Inject
    private DeviceStatusRepository deviceStatusRepository;

    @Inject
    private AlerterService alerterService;

    /**
     * Process an incoming MQTT message.
     * 
     * @param topic   The MQTT topic (e.g., "network/MaliGrdi")
     * @param payload The JSON payload as string
     */
    @Transactional // All database operations in one transaction
    public void processMessage(String topic, String payload) {
        try {
            // Extract network name from topic
            // For "network/MaliGrdi" -> "MaliGrdi"
            String networkName = extractNetworkName(topic);

            // Parse JSON payload to Java object using JSON-B
            NetworkStatusMessage message = parseMessage(payload);

            var messageTimestamp = LocalDateTime.ofInstant(message.getTimestamp(), ZoneOffset.UTC);

            // Get or create network record
            NetworkEntity network = getOrCreateNetwork(networkName);
            network.setLastSeen(messageTimestamp);
            networkRepository.save(network);

            // Get list of currently online MACs from message
            Set<String> currentlyOnlineMacs = new HashSet<>();
            for (NetworkStatusMessage.DeviceInfo device : message.getDevices()) {
                currentlyOnlineMacs.add(device.getMac());
            }

            // load all devices from the device repository for this network
            var knownDevices = deviceRepository.findAllForNetwork(network.getId());

            // load all previously online devices for this network
            var previouslyOnlineDevices = deviceStatusRepository.findCurrentlyOnline(network);

            List<Long> processedDevices = new ArrayList<>();

            // Process each device in the message (all are online)
            for (NetworkStatusMessage.DeviceInfo device : message.getDevices()) {

                // Determine if we need to record a state change
                boolean shouldRecord = false;

                // possible scenarios:
                // 1. device is known and was online -> no change
                // 2. device is known and was offline -> record online, log change if alwaysOn
                // is true
                // 3. device is unknown -> record online, add to device repository, log new
                // device

                var mac = device.getMac();
                var ip = device.getIp();

                // find the combination of mac and ip in the known devices list; keep in mind
                // that ip can be null
                var knownDeviceOpt = knownDevices.stream().filter(
                        d -> d.getMacAddress().equals(mac) && (d.getIpAddress() == null || d.getIpAddress().equals(ip)))
                        .findFirst();

                if (knownDeviceOpt.isEmpty()) {
                    // new device, add to repository
                    var newDevice = new DeviceEntity();
                    newDevice.setNetwork(network);
                    newDevice.setMacAddress(mac);
                    newDevice.setIpAddress(ip);
                    // default to NOT_ALLOWED for new devices
                    newDevice.setDeviceOperationMode(DeviceOperationMode.NOT_ALLOWED);
                    newDevice.setOnline(true); // currently online
                    newDevice.setFirstSeen(messageTimestamp);
                    newDevice.setLastSeen(messageTimestamp);
                    newDevice.setActiveAlarmTime(LocalDateTime.now(ZoneOffset.UTC)); // because we're going to alert
                                                                                     // about it
                    deviceRepository.save(newDevice);

                    alerterService.sendAlert("Unauthorized device detected on network for the first time.", network,
                            newDevice);

                    // also add to device history
                    shouldRecord = true;
                } else {
                    // known device
                    var knownDevice = knownDeviceOpt.get();
                    processedDevices.add(knownDevice.getId());

                    // see if alert needs to be sent for unauthorized device
                    if (knownDevice.getDeviceOperationMode() == DeviceOperationMode.NOT_ALLOWED
                            && knownDevice.getActiveAlarmTime() == null) {
                        // device is not allowed and no alert has been sent yet
                        alerterService.sendAlert("Unauthorized device detected on network.", network, knownDevice);
                        knownDevice.setActiveAlarmTime(LocalDateTime.now(ZoneOffset.UTC));
                    }

                    // in all cases, update device's current online status and last seen
                    knownDevice.setOnline(true);
                    knownDevice.setLastSeen(messageTimestamp);
                    deviceRepository.save(knownDevice);

                    // check last known status - search in previouslyOnlineDevices
                    var lastOnlineStatus = previouslyOnlineDevices.stream()
                            .filter(d -> d.getMacAddress().equals(mac))
                            .findFirst();

                    if (lastOnlineStatus.isPresent()) {
                        // device was already online, no change, don't record
                        LOGGER.info("Device is still online: " + mac + " (" + ip + ") on " + network.getName());
                    } else {
                        // The device was offline, now online
                        shouldRecord = true;
                        if (knownDevice.getDeviceOperationMode() == DeviceOperationMode.NOT_ALLOWED) {
                            LOGGER.info("Device " + mac + " (" + ip + ") is not allowed on network "
                                    + network.getName() + " but is online!");
                        } else {
                            LOGGER.info(String
                                    .format("Device came online: " + mac + " (" + ip + ") on " + network.getName()));
                        }
                    }
                }

                if (shouldRecord) {
                    DeviceStatusHistoryEntity status = new DeviceStatusHistoryEntity(
                            network, mac, ip, true, messageTimestamp);
                    deviceStatusRepository.save(status);
                }

            }

            // now process known devices that were not in the current message
            for (var knownDevice : knownDevices) {
                if (processedDevices.contains(knownDevice.getId())) {
                    continue; // already processed
                }

                var mac = knownDevice.getMacAddress();
                var ip = knownDevice.getIpAddress();

                // in all cases, update device's current online status and last seen
                knownDevice.setOnline(false);
                deviceRepository.save(knownDevice);

                // check if the device was previously online
                var lastOnlineStatus = previouslyOnlineDevices.stream()
                        .filter(d -> d.getMacAddress().equals(mac))
                        .findFirst();

                if (lastOnlineStatus.isPresent()) {
                    // device went offline
                    LOGGER.info("Device went offline: " + mac + " (" + ip + ") on " + network.getName());

                    // Record offline status with last known IP
                    var offlineStatus = new DeviceStatusHistoryEntity(
                            network, mac,
                            ip,
                            false, messageTimestamp);
                    deviceStatusRepository.save(offlineStatus);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error processing MQTT message from topic: {}", topic, e);
        }
    }

    /**
     * Extract network name from MQTT topic.
     * The topic is expected to be in format
     * "something/maybeSomethingElse/AndSoOn/NetworkName/operationName".
     */
    private String extractNetworkName(String topic) {

        int rightSlashIndex = topic.lastIndexOf('/');
        if (rightSlashIndex > 0) {
            int leftSlashIndex = topic.lastIndexOf('/', rightSlashIndex - 1);
            if (leftSlashIndex >= 0) {
                return topic.substring(leftSlashIndex + 1, rightSlashIndex);
            }
        }

        LOGGER.warn("Topic does not follow expected format, using entire topic as network name: {}", topic);
        return topic;
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
    private NetworkEntity getOrCreateNetwork(String networkName) {
        return networkRepository.findByName(networkName)
                .orElseGet(() -> {
                    NetworkEntity newNetwork = new NetworkEntity(networkName);
                    return networkRepository.save(newNetwork);
                });
    }

}
