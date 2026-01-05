package com.matjazt.networkmonitor.entity;

/**
 * Enum representing alert types in the system.
 * 
 * Ordinal values match alert_type.id in database for referential integrity.
 */
public enum AlertType {
    /** Network has not reported within configured alerting_delay period */
    NETWORK_DOWN, // ordinal = 0

    /** An ALWAYS_ON device has gone offline */
    DEVICE_DOWN, // ordinal = 1

    /** An UNAUTHORIZED device has been detected on the network */
    DEVICE_UNAUTHORIZED; // ordinal = 2
}
