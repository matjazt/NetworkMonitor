ne vsega hkrati poganjat!!!!

drop table device_status_history ;
drop table device ;
drop table network ;


SELECT
    *
FROM
    device_status_history
ORDER BY
    mac_address,
    timestamp DESC;

SELECT
    n.name AS network,
    d.mac_address,
    d.ip_address,
    COUNT(*) FILTER (
        WHERE
            d.online = false
    ) as offline_count,
    COUNT(*) FILTER (
        WHERE
            d.online = true
    ) as online_count,
    MAX(d.timestamp) AS last_seen
FROM
    device_status_history d
    JOIN network n ON d.network_id = n.id
GROUP BY
    n.name,
    d.mac_address,
    d.ip_address
ORDER BY
    offline_count DESC,
    n.name,
    d.mac_address;

select
    *
from
    device;

select
    *
from
    network;

alter TABLE network
drop column alertingdelay;

insert into account_type (id, name, description) values (1, 'admin', 'administrator'), (2, 'user', 'ordinary user'), (3, 'device', 'montoring device')



-- Create operation_mode reference table with zero-based IDs
CREATE TABLE device_operation_mode (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- Insert operation modes with zero-based IDs matching enum ordinals
INSERT INTO device_operation_mode (id, name, description) VALUES
    (0, 'NOT_ALLOWED', 'Device is not allowed on the network'),
    (1, 'ALLOWED', 'Device is allowed but not monitored'),
    (2, 'ALWAYS_ON', 'Device should always be online and is monitored');


update device set device_operation_mode_id = device_operation_mode_id -1

ALTER TABLE device 
ADD CONSTRAINT fk_device_device_operation_mode 
FOREIGN KEY (device_operation_mode_id) REFERENCES device_operation_mode(id);


ALTER TABLE account 
ADD CONSTRAINT fk_account_account_type 
FOREIGN KEY (account_type_id) REFERENCES account_type (id);


-- Step 1: Create alert_type reference table with zero-based IDs matching enum ordinals
CREATE TABLE alert_type (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- Step 2: Insert the three alert types with zero-based IDs matching enum
INSERT INTO alert_type (id, name, description) VALUES
    (0, 'NETWORK_DOWN', 'Network connectivity lost or network went offline'),
    (1, 'DEVICE_DOWN', 'Device that should always be online is not responding'),
    (2, 'DEVICE_NOT_ALLOWED', 'Unauthorized device detected on the network');

-- Step 3: Create alert table
CREATE TABLE alert (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    network_id BIGINT NOT NULL,
    device_id BIGINT,
    alert_type_id INTEGER NOT NULL,
    message VARCHAR(500) NOT NULL,
    CONSTRAINT fk_alert_network FOREIGN KEY (network_id) REFERENCES network(id),
    CONSTRAINT fk_alert_device FOREIGN KEY (device_id) REFERENCES device(id),
    CONSTRAINT fk_alert_type FOREIGN KEY (alert_type_id) REFERENCES alert_type(id)
);

-- Step 4: Create indexes for better query performance
CREATE INDEX idx_alert_network ON alert(network_id);
CREATE INDEX idx_alert_device ON alert(device_id);
CREATE INDEX idx_alert_timestamp ON alert(timestamp);


ALTER TABLE alert
  ADD CONSTRAINT fk_alert_alert_type
  FOREIGN KEY (alert_type_id) REFERENCES alert_type(id);

ALTER TABLE alert
  ADD CONSTRAINT fk_alert_device
  FOREIGN KEY (device_id) REFERENCES device(id);

ALTER TABLE device
  ADD CONSTRAINT fk_devicet_network
  FOREIGN KEY (network_id) REFERENCES network(id);

ALTER TABLE alert
  ADD CONSTRAINT fk_alert_network
  FOREIGN KEY (network_id) REFERENCES network(id);

ALTER TABLE account_network
  ADD CONSTRAINT fk_account_network_network
  FOREIGN KEY (network_id) REFERENCES network(id);

ALTER TABLE account_network
  ADD CONSTRAINT fk_account_network_account
  FOREIGN KEY (account_id) REFERENCES account(id);