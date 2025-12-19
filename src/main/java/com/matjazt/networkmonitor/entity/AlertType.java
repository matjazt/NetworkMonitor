package com.matjazt.networkmonitor.entity;

/**
 * Enum representing alert types.
 * Ordinal values match alert_type.id in database for referential integrity.
 */
public enum AlertType {
    NETWORK_DOWN, // ordinal = 0
    DEVICE_DOWN, // ordinal = 1
    DEVICE_UNAUTHORIZED; // ordinal = 2
}
