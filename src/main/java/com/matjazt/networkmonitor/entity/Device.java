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
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many devices belong to one network.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    /**
     * Unique identifier for the device (MAC address).
     */
    @Column(name = "mac_address", nullable = false, unique = true, length = 17)
    private String macAddress;

    /**
     * Current IP address of the device.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Indicates if the device is allowed on the network.
     */
    @Column(nullable = false)
    private Boolean allowed;

    /**
     * Indicates if the device is expected to always be on.
     */
    @Column(name = "always_on", nullable = false)
    private Boolean alwaysOn;

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

    // JPA requires no-arg constructor
    public Device() {
    }

    public Device(Network network, String macAddress, String ipAddress, Boolean online) {
        this.network = network;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.online = online;
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
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

    public Boolean getAllowed() {
        return allowed;
    }

    public void setAllowed(Boolean allowed) {
        this.allowed = allowed;
    }

    public Boolean getAlwaysOn() {
        return alwaysOn;
    }

    public void setAlwaysOn(Boolean alwaysOn) {
        this.alwaysOn = alwaysOn;
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
        this.lastSeen = LocalDateTime.now();
    }
}