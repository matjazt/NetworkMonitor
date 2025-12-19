package com.matjazt.networkmonitor.entity;

/**
 * Enum representing alarm types.
 * Ordinal values match alarm_type.id in database for referential integrity.
 */
public enum AlarmType {
    NETWORK_DOWN, // ordinal = 0
    DEVICE_DOWN, // ordinal = 1
    UNAUTHORIZED_DEVICE; // ordinal = 2
}
