-- Network Monitor Database Schema
-- PostgreSQL DDL for creating tables and indexes
-- Run this with: psql -U postgres -d network_monitor -f schema.sql

-- Drop tables if they exist (for clean reinstall)
DROP TABLE IF EXISTS device_status_history CASCADE;
DROP TABLE IF EXISTS networks CASCADE;

-- Networks table: stores information about monitored networks
CREATE TABLE networks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    first_seen TIMESTAMP NOT NULL,
    last_seen TIMESTAMP NOT NULL
);

-- Add index for faster lookups by name
CREATE INDEX idx_networks_name ON networks(name);

-- Device status history: historical record of device state changes
-- Each row represents when a device went online or offline
CREATE TABLE device_status_history (
    id BIGSERIAL PRIMARY KEY,
    network_id BIGINT NOT NULL,
    mac_address VARCHAR(17) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    online BOOLEAN NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    
    -- Foreign key to networks table
    CONSTRAINT fk_network
        FOREIGN KEY (network_id)
        REFERENCES networks(id)
        ON DELETE CASCADE
);

-- Index for finding all devices in a network
CREATE INDEX idx_network_id ON device_status_history(network_id);

-- Index for finding current status of a specific device
-- DESC on timestamp for efficient "latest status" queries
CREATE INDEX idx_mac_timestamp ON device_status_history(mac_address, timestamp DESC);

-- Composite index for the most common query pattern
CREATE INDEX idx_network_mac_timestamp 
    ON device_status_history(network_id, mac_address, timestamp DESC);

-- Comments for documentation
COMMENT ON TABLE networks IS 'Stores monitored networks extracted from MQTT topics';
COMMENT ON TABLE device_status_history IS 'Historical record of device online/offline state changes';

COMMENT ON COLUMN device_status_history.mac_address IS 'Device MAC address - permanent identifier';
COMMENT ON COLUMN device_status_history.ip_address IS 'Device IP at time of status change';
COMMENT ON COLUMN device_status_history.online IS 'true = device came online, false = went offline';
COMMENT ON COLUMN device_status_history.timestamp IS 'When the status change occurred (from MQTT message)';
