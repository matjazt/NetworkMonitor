package com.matjazt.networkmonitor.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity representing a device on a network.
 * 
 * This entity stores the current state of each device,
 * while DeviceStatusHistory tracks historical changes.
 */
@Entity
@Table(name = "device", indexes = {
        @Index(name = "idx_device_mac", columnList = "mac_address"),
        @Index(name = "idx_device_network", columnList = "network_id")
})
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many devices belong to one network.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private NetworkEntity network;

    /**
     * Human-readable device name.
     */
    @Column(name = "name", length = 200)
    private String name;

    /**
     * Unique identifier for the device (MAC address).
     */
    @Column(name = "mac_address", nullable = false, length = 17)
    private String macAddress;

    /**
     * Current IP address of the device.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * The operational mode of this device (not allowed, allowed, always on).
     * Stored as integer matching operation_mode.id for referential integrity.
     */
    @Column(name = "device_operation_mode_id", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private DeviceOperationMode deviceOperationMode;

    /**
     * Reference to DeviceOperationModeEntity for OpenJPA foreign key validation
     * only.
     * Not used in runtime code - insertable/updatable=false ensures enum field
     * controls the value.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_operation_mode_id", insertable = false, updatable = false)
    private DeviceOperationModeEntity deviceOperationModeRef;

    /**
     * Current online status.
     */
    @Column(nullable = false)
    private Boolean online;

    /**
     * When this device was first seen.
     */
    @Column(name = "first_seen", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime firstSeen;

    /**
     * Last time we received data about this device.
     */
    @Column(name = "last_seen", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime lastSeen;

    /**
     * If there's an active alert for this device, references the alert ID.
     */
    @Column(name = "active_alert_id", nullable = true)
    private Long activeAlertId;

    // JPA requires no-arg constructor
    public DeviceEntity() {
    }

    public DeviceEntity(NetworkEntity network, String macAddress, String ipAddress, Boolean online) {
        this.network = network;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.online = online;
        this.firstSeen = LocalDateTime.now(ZoneOffset.UTC);
        this.lastSeen = LocalDateTime.now(ZoneOffset.UTC);
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NetworkEntity getNetwork() {
        return network;
    }

    public void setNetwork(NetworkEntity network) {
        this.network = network;
    }

    public String getName() {
        return name;
    }

    public String getNameOrUnknown() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return "unknown";
    }

    public String getNameOrMac() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return macAddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public DeviceOperationMode getDeviceOperationMode() {
        return deviceOperationMode;
    }

    public void setDeviceOperationMode(DeviceOperationMode operationMode) {
        this.deviceOperationMode = operationMode;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getActiveAlertId() {
        return activeAlertId;
    }

    public void setActiveAlertId(Long activeAlertId) {
        this.activeAlertId = activeAlertId;
    }

}