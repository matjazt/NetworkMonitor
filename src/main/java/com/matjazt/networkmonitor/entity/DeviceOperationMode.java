package com.matjazt.networkmonitor.entity;

/**
 * Enum representing the operational mode of a device on a network.
 * Ordinal values match operation_mode.id in database for referential integrity.
 */
public enum DeviceOperationMode {
    UNAUTHORIZED, // ordinal = 0
    ALLOWED, // ordinal = 1
    ALWAYS_ON; // ordinal = 2
}
