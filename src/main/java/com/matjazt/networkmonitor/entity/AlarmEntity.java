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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity representing an alarm (alert) in the system.
 * 
 * Alarms are triggered when networks or devices go down, or when
 * unauthorized devices are detected.
 */
@Entity
@Table(name = "alarm", indexes = {
        @Index(name = "idx_alarm_network", columnList = "network_id"),
        @Index(name = "idx_alarm_device", columnList = "device_id"),
        @Index(name = "idx_alarm_timestamp", columnList = "timestamp")
})
public class AlarmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * When this alarm was triggered.
     */
    @Column(name = "timestamp", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    /**
     * The network this alarm is associated with.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private NetworkEntity network;

    /**
     * The device this alarm is associated with (optional).
     * Null for network-level alarms.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = true)
    private DeviceEntity device;

    /**
     * The type of alarm.
     * Stored as integer matching alarm_type.id for referential integrity.
     */
    @Column(name = "alarm_type_id", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private AlarmType alarmType;

    /**
     * Reference to AlarmTypeEntity for OpenJPA foreign key validation only.
     * Not used in runtime code - insertable/updatable=false ensures enum field
     * controls the value.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_type_id", insertable = false, updatable = false)
    private AlarmTypeEntity alarmTypeRef;

    /**
     * Human-readable alarm message.
     */
    @Column(name = "message", nullable = true, length = 500)
    private String message;

    /**
     * When this alarm was closed/resolved (optional).
     */
    @Column(name = "closure_timestamp", nullable = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime closureTimestamp;

    // JPA requires no-arg constructor
    public AlarmEntity() {
    }

    public AlarmEntity(LocalDateTime timestamp, NetworkEntity network, DeviceEntity device,
            AlarmType alarmType, String message) {
        this.timestamp = timestamp;
        this.network = network;
        this.device = device;
        this.alarmType = alarmType;
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

    public AlarmType getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(AlarmType alarmType) {
        this.alarmType = alarmType;
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
