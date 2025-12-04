package com.matjazt.networkmonitor.model;

import java.util.List;

/**
 * Data Transfer Object (DTO) representing the JSON structure of MQTT messages.
 * 
 * DTOs are simple data containers used for transferring data between layers.
 * This matches the MQTT payload structure exactly.
 * 
 * Jakarta JSON-B (JSON Binding) automatically maps JSON to these objects,
 * similar to System.Text.Json in .NET.
 */
public class NetworkStatusMessage {

    /**
     * Network hostname from MQTT message.
     */
    private String hostname;

    /**
     * Timestamp from the message (format: "2025-12-03 10:18:56").
     */
    private String timestamp;

    /**
     * List of currently online devices.
     */
    private List<DeviceInfo> devices;

    // No-arg constructor required for JSON-B deserialization
    public NetworkStatusMessage() {
    }

    // Getters and setters

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<DeviceInfo> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceInfo> devices) {
        this.devices = devices;
    }

    /**
     * Nested class for device information in the JSON.
     */
    public static class DeviceInfo {
        private String ip;
        private String mac;

        public DeviceInfo() {
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }
    }
}
