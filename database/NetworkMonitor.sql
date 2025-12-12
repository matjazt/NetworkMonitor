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