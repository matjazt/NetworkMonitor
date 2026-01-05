# Example MQTT Payloads

This directory contains sample MQTT messages for testing the Network Monitor application.

## Message Format

Each message represents a network scan result containing currently online devices:

- `hostname`: Name of the device performing the scan
- `timestamp`: ISO 8601 timestamp when scan occurred (e.g., `2026-01-05T11:45:40+01:00`)
- `devices`: Array of online devices with:
  - `ip`: Device IP address
  - `mac`: Device MAC address (permanent identifier)

**Important**: Only currently online devices should be included. The application detects offline devices by comparing this list against the database.

## Topic Pattern

The MQTT topic follows the pattern defined in `mqtt.topic.template` configuration:

```
network/{networkName}/scan
```

For example, if you have a network named "TestNet", publish to: `network/TestNet/scan`

The `{networkName}` must match a network entry in the database.

## Publishing Test Messages

### Using mosquitto_pub (Windows PowerShell)

```powershell
# Publish from file
Get-Content example-message.json | mosquitto_pub `
  -h your-broker -p 8883 `
  -t network/TestNet/scan `
  -u username -P password `
  --cafile ca.crt -l
```

### Using mosquitto_pub (Linux/Mac)

```bash
# Publish from file
mosquitto_pub -h your-broker -p 8883 \
  -t network/TestNet/scan \
  -u username -P password \
  --cafile /path/to/ca.crt \
  -f example-message.json

# Publish inline message
mosquitto_pub -h your-broker -p 8883 \
  -t network/TestNet/scan \
  -u username -P password \
  --cafile /path/to/ca.crt \
  -m '{"hostname":"Scanner","timestamp":"2026-01-05T12:00:00+01:00","devices":[{"ip":"192.168.1.1","mac":"AA:BB:CC:DD:EE:FF"}]}'
```

## Tips

- **Topic must match**: Use exact network name in topic path
- **Timestamp format**: Use ISO 8601 with timezone
- **MAC format**: Use colon-separated format (e.g., `AA:BB:CC:DD:EE:FF`)
- **IP format**: Standard IPv4 dotted decimal notation
