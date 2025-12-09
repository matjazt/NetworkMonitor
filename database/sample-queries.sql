-- Sample queries for exploring the data
-- View all networks with activity timestamps
SELECT
    id,
    name,
    first_seen,
    last_seen,
    EXTRACT(
        EPOCH
        FROM
            (last_seen - first_seen)
    ) / 3600 as hours_monitored
FROM
    network
ORDER BY
    name;

-- Get current online devices across all networks
SELECT
    n.name as network,
    d.mac_address,
    d.ip_address,
    d.timestamp as last_seen
FROM
    device_status_history d
    JOIN network n ON d.network_id = n.id
WHERE
    d.online = true
    AND d.timestamp = (
        SELECT
            MAX(timestamp)
        FROM
            device_status_history
        WHERE
            mac_address = d.mac_address
            AND network_id = d.network_id
    )
ORDER BY
    n.name,
    d.mac_address;

-- Device status change history for a specific device
SELECT
    n.name as network,
    mac_address,
    ip_address,
    online,
    timestamp
FROM
    device_status_history d
    JOIN network n ON d.network_id = n.id
WHERE
    mac_address = 'D8:B6:B7:F1:F8:E4' -- Replace with actual MAC
ORDER BY
    timestamp DESC;

-- Count online vs offline devices per network
SELECT
    n.name,
    SUM(
        CASE
            WHEN d.online THEN 1
            ELSE 0
        END
    ) as currently_online,
    COUNT(DISTINCT d.mac_address) as total_devices
FROM
    network n
    LEFT JOIN device_status_history d ON d.network_id = n.id
WHERE
    d.timestamp = (
        SELECT
            MAX(timestamp)
        FROM
            device_status_history
        WHERE
            mac_address = d.mac_address
            AND network_id = d.network_id
    )
GROUP BY
    n.name;

-- Find devices with frequent state changes (flapping)
SELECT
    n.name,
    mac_address,
    COUNT(*) as state_changes,
    MIN(timestamp) as first_change,
    MAX(timestamp) as last_change
FROM
    device_status_history d
    JOIN network n ON d.network_id = n.id
WHERE
    timestamp > NOW () - INTERVAL '24 hours'
GROUP BY
    n.name,
    mac_address
HAVING
    COUNT(*) > 5
ORDER BY
    state_changes DESC;

-- Timeline of device activity for a network
SELECT
    mac_address,
    ip_address,
    CASE
        WHEN online THEN 'ONLINE'
        ELSE 'OFFLINE'
    END as status,
    timestamp
FROM
    device_status_history
WHERE
    network_id = (
        SELECT
            id
        FROM
            network
        WHERE
            name = 'MaliGrdi'
    )
ORDER BY
    timestamp DESC
LIMIT
    50;