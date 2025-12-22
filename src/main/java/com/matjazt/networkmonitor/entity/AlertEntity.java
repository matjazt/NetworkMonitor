package com.matjazt.networkmonitor.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity representing an alert (alert) in the system.
 * 
 * Alerts are triggered when networks or devices go down, or when
 * unauthorized devices are detected.
 */
@Entity
@Table(name = "alert")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * When this alert was triggered.
     */
    @Column(name = "timestamp", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    /**
     * The network this alert is associated with.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private NetworkEntity network;

    /**
     * The device this alert is associated with (optional).
     * Null for network-level alerts.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = true)
    private DeviceEntity device;

    /**
     * The type of alert.
     * Stored as integer matching alert_type.id for referential integrity.
     */
    @Column(name = "alert_type_id", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private AlertType alertType;

    /**
     * Reference to AlertTypeEntity for OpenJPA foreign key validation only.
     * Not used in runtime code - insertable/updatable=false ensures enum field
     * controls the value.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_type_id", insertable = false, updatable = false)
    private AlertTypeEntity alertTypeRef;

    /**
     * Human-readable alert message.
     */
    @Column(name = "message", nullable = true, length = 500)
    private String message;

    /**
     * When this alert was closed/resolved (optional).
     */
    @Column(name = "closure_timestamp", nullable = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime closureTimestamp;

    // JPA requires no-arg constructor
    public AlertEntity() {
    }

    public AlertEntity(LocalDateTime timestamp, NetworkEntity network, DeviceEntity device,
            AlertType alertType, String message) {
        this.timestamp = timestamp;
        this.network = network;
        this.device = device;
        this.alertType = alertType;
        this.message = message;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getClosureTimestamp() {
        return closureTimestamp;
    }

    public void setClosureTimestamp(LocalDateTime closureTimestamp) {
        this.closureTimestamp = closureTimestamp;
    }
}
