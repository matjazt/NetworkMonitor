package com.matjazt.networkmonitor.entity;

/**
 * Enum representing the operational mode of a device on a network.
 * Determines monitoring behavior and alerting policy.
 * 
 * Ordinal values match device_operation_mode.id in database for referential
 * integrity.
 */
public enum DeviceOperationMode {
    /**
     * Device is not allowed on the network - triggers DEVICE_UNAUTHORIZED alerts
     */
    UNAUTHORIZED, // ordinal = 0

    /** Device is allowed but not actively monitored for uptime */
    AUTHORIZED, // ordinal = 1

    /** Device should always be online - triggers DEVICE_DOWN alerts when offline */
    ALWAYS_ON; // ordinal = 2
}
