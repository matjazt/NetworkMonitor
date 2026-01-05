# Network Scanner for RouterOS 7

A RouterOS script that scans local network ranges, monitors critical nodes, and publishes device status to an MQTT broker for centralized monitoring.

## Overview

This script integrates with the Network Monitor application by:

1. Scanning configured IP ranges via ARP cache population
2. Performing targeted ping verification for critical nodes (24/7 devices)
3. Publishing online device lists (IP + MAC) to MQTT in JSON format

The backend application processes these messages to detect device state changes, trigger alerts for offline critical devices, and identify unauthorized devices.

## How It Works

**Two-stage detection approach:**

1. **ARP Scan**: Quickly pings configured IP ranges to populate the RouterOS ARP cache
2. **Targeted Verification**: For nodes listed in `pingableNodes`, performs additional ping verification with configurable packet count and timeout

This dual approach enables:

- **Uptime monitoring** for critical devices that must be online 24/7 (routers, servers, IoT controllers)
- **Presence detection** for all other devices via ARP cache

**Note**: The script scans only configured IP ranges but reports all valid entries from the ARP cache. Devices from other subnets may appear in the output. The backend application handles filtering based on network configuration.

## Prerequisites

### 1. IoT Package

Install the IoT package to enable MQTT functionality:

1. Navigate to **System → Packages**
2. Click **Check For Updates**
3. Find the **iot** package
4. Click **Enable**
5. Verify installation in **System → Log**

### 2. MQTT Broker Connection

#### For Self-Signed or Private CA Certificates

If using a broker with a self-signed certificate or private CA:

1. Export your CA certificate in PEM format
2. Upload to RouterOS via **Files** menu
3. Import: **System → Certificates** → **Import**
4. Verify the certificate is trusted

**Important**:
Unlike web browsers, RouterOS does **not** perform AIA fetching to download intermediate certificates. Ensure your MQTT broker is configured to send the full certificate chain:

- **Let's Encrypt/Certbot**: Use `fullchain.pem`, not `cert.pem`
- **Custom CA**: Concatenate leaf + intermediate(s) + root certificates

#### For Public CA Certificates

Enable built-in trust anchors:

```routeros
/certificate settings set builtin-trust-anchors=trusted
```

#### Create Broker Connection

```routeros
/iot/mqtt/brokers/add \
  name=mybroker \
  address=mqtt.example.com \
  port=8883 \
  ssl=yes \
  auto-connect=yes \
  client-id=router-scanner-01 \
  username=your-username \
  password=your-password
```

The broker `name` must match the value in `networkScan.json`.

**Test the connection:**

```routeros
/iot mqtt publish broker=mybroker topic=test/topic message="Hello MQTT" qos=1
```

### 3. MQTT Topic ACLs (Recommended)

Configure MQTT broker ACLs to restrict each device to its designated topic pattern. Since MQTT does not reveal message publishers to subscribers, topic-based access control is essential for security.

Example Mosquitto ACL:

```txt
user router-scanner-01
topic write network/Office/scan
```

## Installation

### 1. Upload Configuration File

1. Edit `networkScan.json` with your network configuration
2. Upload to RouterOS device via **Files** menu
3. Move/rename to `/etc/networkScan.json` or any preferred persistent location (you might need to put in `/flash` or `/disk` depending on your setup - in this case, adjust the script accordingly)

### 2. Install Script

#### Option A: Via WinBox/WebFig

1. Navigate to **System → Scripts**
2. Click **Add New**
3. Set **Name**: `NetworkScan`
4. Set **Policy**: `read`, `write`, `test`
5. Paste contents of `networkScan.rsc` into **Source**
6. Click **OK**

#### Option B: Via Terminal

```routeros
/system script add name=NetworkScan source=[paste script contents]
```

### 3. Test Execution

Run the script manually to verify configuration:

```routeros
/system/script/run NetworkScan
```

Check **System → Log** for output and any errors.

### 4. Schedule Automatic Execution

Configure the script to run periodically:

```routeros
/system scheduler add \
  name=NetworkScan \
  interval=1m \
  on-event=NetworkScan \
  policy=read,write,test
```

Adjust `interval` as needed (e.g., `30s`, `2m`).

## Configuration File

`networkScan.json` structure:

```json
{
  "ranges": [
    {
      "subnet": "192.168.1.0",
      "start": 1,
      "end": 254
    },
    {
      "subnet": "10.0.10.0",
      "start": 1,
      "end": 100
    }
  ],
  "pingableNodes": [
    {
      "ip": "192.168.1.1",
      "name": "gateway",
      "count": 3,
      "timeout": 100
    },
    {
      "ip": "192.168.1.10",
      "name": "nas-server",
      "count": 2,
      "timeout": 200
    }
  ],
  "mqtt": {
    "broker": "mybroker",
    "topic": "network/Office/scan"
  }
}
```

### Configuration Fields

**ranges**: Array of IP ranges to scan

- `subnet`: Network address (must be complete IP, e.g., `192.168.1.0`)
- `start`: First host number to scan (1-254)
- `end`: Last host number to scan (1-254)

**pingableNodes**: Critical devices requiring explicit ping verification

- `ip`: Device IP address (must match exactly)
- `name`: Human-readable identifier (for logging only)
- `count`: Number of ping packets (2-5 recommended)
- `timeout`: Ping timeout in milliseconds (100-500 recommended)

**mqtt**: MQTT broker settings

- `broker`: Broker name (must match `/iot/mqtt/brokers` entry)
- `topic`: MQTT topic for publishing (must match backend network name pattern)

### Topic Naming Convention

The MQTT topic should follow the pattern configured in the backend application (`mqtt.topic.template`):

```network/{networkName}/scan```

Example: If your backend has a network named "Office", use topic `network/Office/scan`.

## Output Format

The script publishes JSON messages matching the Network Monitor expected format:

```json
{
  "hostname": "RouterName",
  "timestamp": "2026-01-05T14:30:45+01:00",
  "devices": [
    {
      "ip": "192.168.1.100",
      "mac": "AA:BB:CC:DD:EE:FF"
    },
    {
      "ip": "192.168.1.101",
      "mac": "11:22:33:44:55:66"
    }
  ]
}
```

Only **online devices** are included. The backend detects offline devices by comparing this list with its database.

## Important Notes

### JSON IP Address Parsing

**Warning**: RouterOS JSON parser has a quirk when handling IP-like strings. It auto-completes partial IP addresses:

- `"10.255.254"` becomes `"10.255.0.254"`
- `"192.168.1"` becomes `"192.168.0.1"`

**Always use complete IP addresses** in the JSON configuration (all four octets).

### ARP Cache Behavior

The script reports all valid ARP entries, not just devices from configured ranges. This is intentional—it allows detection of devices that obtained IPs outside the scanned ranges (e.g., static IPs, DHCP reservations).

The backend application handles network-specific filtering.

### Scheduler Policy

Ensure the scheduled script has sufficient permissions. Recommended policies: `read`, `write`, `test`.

## Troubleshooting

**MQTT publish fails**:

- Check broker connection: `/iot mqtt brokers print`
- Verify certificate import: `/certificate print`
- Check logs: `/log print where topics~"iot"`

**No devices detected**:

- Verify IP ranges in configuration
- Check ARP cache manually: `/ip arp print`
- Test ping manually: `/ping 192.168.1.1 count=2`

**JSON parse errors**:

- Validate JSON syntax (use online validator)
- Ensure all IP addresses have four octets
- Check file encoding (UTF-8 without BOM)

**Script not running on schedule**:

- Verify scheduler entry: `/system scheduler print`
- Check script policy matches scheduler policy
- Review system log for errors

## Integration with Network Monitor

This script integrates with the [Network Monitor](../../README.md) backend application:

1. Create a network entry in the backend database with matching name
2. The backend subscribes automatically based on database networks
3. Device states are tracked and alerts triggered based on operation mode

See the main [README](../../README.md) for backend setup and configuration.
