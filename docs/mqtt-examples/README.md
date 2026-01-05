# Example MQTT Payloads

This directory contains sample MQTT messages for testing.

## Usage

Use an MQTT client like `mosquitto_pub` to publish these messages:

```bash
# For Linux/Mac
mosquitto_pub -h your-broker -p 8883 \
  -t networks/YourNetwork/scan \
  -u username -P password \
  --cafile /path/to/ca.crt \
  -f example-message.json

# For Windows (PowerShell)
Get-Content example-message.json | mosquitto_pub -h your-broker -p 8883 -t networks/YourNetwork/scan -u username -P password --cafile ca.crt -l
```

## Message Format

Each message contains:

- `hostname`: Hostname of the scanning device
- `timestamp`: When the scan occurred, for example 2026-01-05T11:45:40+01:00 (ISO 8601 format)
- `devices`: Array of online devices, each with:
  - `ip`: Device IP address
  - `mac`: Device MAC address

Only online devices are included in the list.
