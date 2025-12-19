package com.matjazt.networkmonitor.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity representing a device status change event.
 * 
 * This is a historical record - each row represents when a device
 * changed state (went online or offline).
 * We only insert new rows when the status actually changes,
 * not on every MQTT message.
 */
@Entity
@Table(name = "device_status_history", indexes = {
        // Index for fast lookups by network
        @Index(name = "idx_network_id", columnList = "network_id")
})
public class DeviceStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many-to-One relationship: many status records belong to one network.
     * Similar to a foreign key in SQL, but JPA manages the relationship.
     * 
     * @ManyToOne tells JPA this is a relationship field.
     * @JoinColumn specifies the foreign key column name.
     */
    @ManyToOne(fetch = FetchType.LAZY) // LAZY = don't load network unless accessed
    @JoinColumn(name = "network_id", nullable = false)
    private NetworkEntity network;

    /**
     * The device this history entry is associated with.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    /**
     * Device IP address at the time of this status change.
     * Can change for the same device (DHCP), so we store it historically.
     */
    @Column(name = "ip_address", nullable = false, length = 45) // 45 chars for IPv6
    private String ipAddress;

    /**
     * True if device went online, false if it went offline.
     */
    @Column(nullable = false)
    private Boolean online;

    /**
     * When this status change occurred.
     * Uses the timestamp from the MQTT message.
     */
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    // JPA requires no-arg constructor
    public DeviceStatusHistoryEntity() {
    }

    public DeviceStatusHistoryEntity(NetworkEntity network, DeviceEntity device, String ipAddress,
            Boolean online, LocalDateTime timestamp) {
        this.network = network;
        this.device = device;
        this.ipAddress = ipAddress;
        this.online = online;
        this.timestamp = timestamp;
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

    public DeviceEntity getDevice() {
        return device;
    }

    public void setDevice(DeviceEntity device) {
        this.device = device;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
