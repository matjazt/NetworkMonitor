# Example MQTT Payloads

This directory contains sample MQTT messages for testing.

## Usage

Use an MQTT client like `mosquitto_pub` to publish these messages:

```bash
# For Linux/Mac
mosquitto_pub -h your-broker -p 8883 \
  -t network/MaliGrdi \
  -u username -P password \
  --cafile /path/to/ca.crt \
  -f example-message.json

# For Windows (PowerShell)
Get-Content example-message.json | mosquitto_pub -h your-broker -p 8883 -t network/MaliGrdi -u username -P password --cafile ca.crt -l
```

## Message Format

Each message contains:
- `hostname`: Network hostname
- `timestamp`: When the scan occurred (format: "YYYY-MM-DD HH:MM:SS")
- `devices`: Array of online devices, each with:
  - `ip`: Device IP address
  - `mac`: Device MAC address

Only online devices are included in the list.
