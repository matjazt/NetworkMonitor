package com.matjazt.networkmonitor.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matjazt.networkmonitor.dao.MonitoringDAO;
import com.matjazt.networkmonitor.entity.AlertType;
import com.matjazt.networkmonitor.entity.DeviceEntity;
import com.matjazt.networkmonitor.entity.DeviceOperationMode;
import com.matjazt.networkmonitor.entity.DeviceStatusHistoryEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;
import com.matjazt.networkmonitor.model.NetworkStatusMessage;

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
    private MonitoringDAO monitoringDao;

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
        LOGGER.debug("Processing MQTT message from topic: {}:\n{}", topic, payload);

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
            monitoringDao.save(network);

            /*
             * // Get list of currently online MACs from message
             * Set<String> currentlyOnlineMacs = new HashSet<>();
             * for (NetworkStatusMessage.DeviceInfo deviceStatus : message.getDevices()) {
             * currentlyOnlineMacs.add(deviceStatus.getMac());
             * }
             */

            // load all devices from the device repository for this network
            var knownDevices = monitoringDao.findAllDevicesForNetwork(network.getId());

            // load all previously online devices for this network
            var previouslyOnlineDevices = monitoringDao.findCurrentlyOnlineDevices(network);

            List<Long> processedDevices = new ArrayList<>();

            // Process each device in the message (all are online)
            for (NetworkStatusMessage.DeviceInfo deviceStatus : message.getDevices()) {

                // Determine if we need to record a state change
                boolean shouldRecord = false;

                // possible scenarios:
                // 1. device is known and was online -> no change
                // 2. device is known and was offline -> record online, log change if alwaysOn
                // is true
                // 3. device is unknown -> record online, add to device repository, log new
                // device

                var mac = deviceStatus.getMac();
                if (mac == null || mac.isBlank()) {
                    LOGGER.warn("Device with missing or empty MAC address reported on network: " + network.getName());
                    continue; // skip devices with missing MAC
                }

                var ip = deviceStatus.getIp();

                // find the mac in the known devices list
                var knownDeviceOpt = knownDevices.stream().filter(
                        d -> d.getMacAddress().equals(mac))
                        .findFirst();

                DeviceEntity device = null;

                if (knownDeviceOpt.isEmpty()) {
                    // new device, add to repository
                    device = new DeviceEntity();
                    device.setNetwork(network);
                    device.setMacAddress(mac);
                    device.setIpAddress(ip);
                    device.setDeviceOperationMode(DeviceOperationMode.UNAUTHORIZED); // default for new devices
                    device.setOnline(true); // currently online, obviously
                    device.setFirstSeen(messageTimestamp);
                    device.setLastSeen(messageTimestamp);
                    // persist the new device before using it in the alert
                    monitoringDao.save(device);

                    alerterService.openAlert(AlertType.DEVICE_UNAUTHORIZED, network, device,
                            "device detected for the first time");

                    // also add to device history
                    shouldRecord = true;
                } else {
                    // known device
                    device = knownDeviceOpt.get();
                    processedDevices.add(device.getId());

                    // in all cases, update device's current online status and last seen
                    device.setOnline(true);
                    device.setLastSeen(messageTimestamp);
                    device.setIpAddress(ip);

                    // see if alert needs to be sent for unauthorized device
                    if (device.getDeviceOperationMode() == DeviceOperationMode.UNAUTHORIZED
                            && device.getActiveAlertId() == null) {
                        // device is not allowed and no alert has been sent yet
                        alerterService.openAlert(AlertType.DEVICE_UNAUTHORIZED, network, device,
                                "device was seen before");
                    } else {
                        // openAlert saves the device, so only save if no alert was opened
                        monitoringDao.save(device);
                    }

                    // check last known status - search in previouslyOnlineDevices
                    var deviceId = device.getId();
                    var lastOnlineStatus = previouslyOnlineDevices.stream()
                            .filter(d -> d.getDevice().getId() == deviceId)
                            .findFirst();

                    if (lastOnlineStatus.isPresent()) {
                        // device was already online, no change, don't record
                        LOGGER.info("Device is still online: " + mac + " (" + ip + ") on " + network.getName());
                    } else {
                        // The device was offline, now online
                        shouldRecord = true;
                        if (device.getDeviceOperationMode() == DeviceOperationMode.UNAUTHORIZED) {
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
                            network, device, ip, true, messageTimestamp);
                    monitoringDao.save(status);
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
                monitoringDao.save(knownDevice);

                // check if the device was previously online
                var lastOnlineStatus = previouslyOnlineDevices.stream()
                        .filter(d -> d.getDevice().getId() == knownDevice.getId())
                        .findFirst();

                if (lastOnlineStatus.isPresent()) {
                    // device went offline
                    LOGGER.info("Device went offline: " + mac + " (" + ip + ") on " + network.getName());

                    // Record offline status with last known IP
                    var offlineStatus = new DeviceStatusHistoryEntity(
                            network, knownDevice,
                            ip,
                            false, messageTimestamp);
                    monitoringDao.save(offlineStatus);
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
        return monitoringDao.findNetworkByName(networkName)
                .orElseGet(() -> {
                    NetworkEntity newNetwork = new NetworkEntity(networkName);
                    return monitoringDao.save(newNetwork);
                });
    }

}
