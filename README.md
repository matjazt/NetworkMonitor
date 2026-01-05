# Network Monitor

A Jakarta EE 10 application for monitoring network devices via MQTT with automated alerting, user authentication, and a REST API.

## Overview

This application subscribes to MQTT topics that publish network device lists, detects when devices go online or offline, stores state changes in a PostgreSQL database, triggers email alerts for critical events, and provides an authenticated REST API to query device status. It supports multi-user access with role-based account management and configurable device monitoring policies (unauthorized, authorized, always-on).

## Technology Stack

- **Jakarta EE 10**: Enterprise Java specification for building scalable applications
- **TomEE 10.x**: Application server that implements Jakarta EE
- **Maven**: Build tool and dependency management (like NuGet + MSBuild)
- **PostgreSQL**: Relational database for storing device history
- **Eclipse Paho**: MQTT client library

## Prerequisites

- JDK 17 (Adoptium)
- Apache Maven (latest)
- Apache TomEE 10.x
- PostgreSQL server

## Additional Software Recommended

### VS Code Extensions

Install these extensions for Java development:

1. **Extension Pack for Java** (vscjava.vscode-java-pack)
   - Includes language support, debugging, testing, Maven integration

2. **Tomcat for Java** (adashen.vscode-tomcat) or **Community Server Connectors** (redhat.vscode-community-server-connector)
   - For deploying and managing TomEE server from VS Code

3. **PostgreSQL** (ckolkman.vscode-postgres)
   - Database client for VS Code

4. **XML** (redhat.vscode-xml)
   - Syntax highlighting and validation for XML configuration files

5. **YAML** (redhat.vscode-yaml) - Optional
   - If you later decide to use YAML config

## Database Setup

### 1. Create Database

```sql
CREATE DATABASE network_monitor;
```

### 2. Run Schema Script

Execute the SQL script in `database/schema.sql` to create tables and seed reference data:

```powershell
psql -U postgres -d network_monitor -f database/schema.sql
```

This creates the following tables:

- **network**: Monitored networks
- **device**: Devices and their current state
- **device_status_history**: Historical state changes
- **alert**: Generated alerts (network down, device down, unauthorized devices)
- **account**: User accounts for API access
- **account_type**: Account role types (admin, user, device)
- **account_network**: User-network access mapping
- **alert_type**: Alert type reference data
- **device_operation_mode**: Device monitoring policy reference data

**Note**: `database/NetworkMonitor.sql` contains development queries and should NOT be executed.

## Configuration

### 1. Application Configuration

Edit `src/main/resources/META-INF/microprofile-config.properties`:

**MQTT Settings:**

- `mqtt.broker.url`: MQTT broker URL (e.g., `ssl://broker.example.com:8883`)
- `mqtt.client.id`: Unique client identifier
- `mqtt.username` / `mqtt.password`: MQTT authentication
- `mqtt.topic.template`: Topic pattern (e.g., `network/{networkName}/scan`)

**Email/SMTP Settings:**

- `smtp.host`, `smtp.port`: SMTP server details
- `smtp.username` / `smtp.password`: SMTP authentication
- `smtp.from.address`, `smtp.from.name`: Email sender identity
- `smtp.starttls.enable`, `smtp.auth.enable`: Security settings

**Alert Timing:**

- `alert.check.initial.delay`: Seconds before first alert check (default: 30)
- `alert.check.interval`: Seconds between alert checks (default: 60)

### 2. Database Connection

Configure in `src/main/resources/META-INF/persistence.xml`:

- Connection pool settings
- SQL logging options

For production deployments, define the DataSource in TomEE's `conf/tomee.xml` instead.

## Building

Compile and package the application:

```powershell
mvn clean package
```

This creates `target/network-monitor.war` - a Web Application Archive ready for deployment.

## Deployment

### Deploy to TomEE

1. Copy the WAR file to TomEE's webapps directory:

   ```powershell
   cp target/network-monitor.war <TOMEE_HOME>/webapps/
   ```

2. Start TomEE:

   ```powershell
   <TOMEE_HOME>/bin/catalina.bat run
   ```

3. TomEE automatically deploys the WAR. Watch logs for startup messages.

### Application URLs

- Application context: `http://localhost:8080/network-monitor/`
- REST API base: `http://localhost:8080/network-monitor/api/networks`

## REST API Endpoints

### Get All Networks

```text
GET /api/networks
```

Returns list of all monitored networks with account context.

**Response:**

```json
{
  "accountId": 1,
  "accountFullName": "John Doe",
  "networks": [
    {
      "id": 1,
      "name": "MaliGrdi",
      "firstSeen": "2025-12-03T10:15:00",
      "lastSeen": "2025-12-04T14:30:00"
    }
  ]
}
```

### Get Online Devices for Network

```text
GET /api/networks/{networkName}/devices
```

Returns currently online devices for the specified network.

**Response:**

```json
{
  "accountId": 1,
  "accountFullName": "John Doe",
  "networkName": "MaliGrdi",
  "devices": [
    {
      "macAddress": "D8:B6:B7:F1:F8:E4",
      "ipAddress": "10.255.254.1",
      "online": true,
      "timestamp": "2025-12-04T14:30:00"
    }
  ]
}
```

**Authentication**: API uses Jakarta Security with Basic Authentication. User credentials are validated against the `account` table with BCrypt password hashing.

## How It Works

### MQTT Message Processing

1. Application starts and connects to MQTT broker
2. Subscribes to configured topics based on topic template and networks in database
3. Receives JSON messages with device lists:

   ```json
   {
     "hostname": "Scanner",
     "timestamp": "2026-01-05T11:45:40+01:00",
     "devices": [
       {"ip": "192.168.1.1", "mac": "AA:BB:CC:DD:EE:FF"}
     ]
   }
   ```

4. For each device in message (online devices):
   - Checks previous status in database
   - If new or was offline: records "online" event
   - Creates/updates device record
5. For known devices not in message:
   - Records "offline" event
6. Only state changes are stored

### Device Operation Modes

Devices can be configured with three operation modes:

- **UNAUTHORIZED** (0): Device is not allowed on the network - triggers alerts
- **AUTHORIZED** (1): Device is allowed but not actively monitored
- **ALWAYS_ON** (2): Device should always be online - triggers alerts when offline

### Alert System

The `AlerterService` runs on a configurable schedule (default: every 60 seconds) and checks for:

1. **NETWORK_DOWN**: Network hasn't sent data within configured `alerting_delay` period
2. **DEVICE_DOWN**: An ALWAYS_ON device is offline
3. **DEVICE_UNAUTHORIZED**: An UNAUTHORIZED device appears online

When triggered:

- Alert record created in database
- Email notification sent to configured address (if set on network)
- Alert remains active until condition clears
- Closure email sent when alert resolves

### Account Management

Users authenticate via Jakarta Security:

- Passwords stored as BCrypt hashes
- Account types: admin, user, device (for MQTT publishers)
- User-network access control via `account_network` junction table
- Security context available throughout application

### Database Schema

**network**: Monitored networks with alerting configuration

- `id`: Primary key
- `name`: Network name (matches MQTT topic)
- `first_seen`, `last_seen`: Activity timestamps
- `alerting_delay`: Seconds before triggering NETWORK_DOWN alert
- `email_address`: Email for alert notifications
- `active_alert_id`: Reference to active alert (if any)

**device**: Current state of devices

- `id`: Primary key
- `network_id`: Foreign key to network
- `mac_address`: Unique device identifier
- `ip_address`: Current IP address
- `name`: Human-readable device name
- `device_operation_mode_id`: Monitoring policy (0=UNAUTHORIZED, 1=AUTHORIZED, 2=ALWAYS_ON)
- `online`: Current online status
- `first_seen`, `last_seen`: Activity timestamps
- `active_alert_id`: Reference to active alert (if any)

**device_status_history**: Historical state changes

- `id`: Primary key
- `network_id`: Foreign key to network
- `device_id`: Foreign key to device
- `ip_address`: IP at time of event
- `online`: State (true=came online, false=went offline)
- `timestamp`: When change occurred

**alert**: Generated alerts

- `id`: Primary key
- `timestamp`: When alert triggered
- `network_id`: Foreign key to network
- `device_id`: Foreign key to device (null for network alerts)
- `alert_type_id`: Type (0=NETWORK_DOWN, 1=DEVICE_DOWN, 2=DEVICE_UNAUTHORIZED)
- `message`: Human-readable message
- `closure_timestamp`: When alert resolved (null if active)

**account**: User accounts

- `id`: Primary key
- `username`, `email`: Unique identifiers
- `password_hash`: BCrypt hashed password
- `full_name`: Display name
- `account_type_id`: Role (1=admin, 2=user, 3=device)
- `created_at`, `last_seen`: Activity timestamps

**account_network**: User access to networks (many-to-many)

**account_type**, **alert_type**, **device_operation_mode**: Reference tables

## Project Structure

See `ProjectStructure.md` for detailed explanation of folders and files.

## Development Tips

### Viewing Logs

TomEE logs are in `<TOMEE_HOME>/logs/catalina.out`. Watch for:

- Application startup messages
- MQTT connection status
- SQL queries (if SQL logging enabled)
- Any errors or warnings

### Testing MQTT Connection

You can test by publishing to your MQTT topics using an MQTT client like `mosquitto_pub`:

```bash
mosquitto_pub -h your-broker -p 8883 -t network/TestNetwork \
  -u username -P password --cafile ca.crt \
  -m '{"hostname":"Test","timestamp":"2025-12-04 15:00:00","devices":[{"ip":"192.168.1.1","mac":"AA:BB:CC:DD:EE:FF"}]}'
```

### Rebuilding After Changes

1. Stop TomEE
2. Run `mvn clean package`
3. Redeploy WAR file
4. Start TomEE

For faster development, consider using TomEE Maven plugin for hot reload.

## Common Issues

**MQTT Connection Fails**: Check broker URL, credentials, and TLS settings in `microprofile-config.properties`. Verify firewall rules. Check TomEE logs for connection errors.

**Database Connection Fails**: Ensure PostgreSQL is running and credentials in `persistence.xml` are correct. Verify database exists and schema is loaded.

**Alert Emails Not Sending**: Verify SMTP configuration. Check `smtp.host`, `smtp.port`, credentials, and network firewall rules. Review TomEE logs for email errors.

**ClassNotFoundException**: Usually means a dependency is missing from `pom.xml` or has wrong scope. Run `mvn clean package` to rebuild.

**No endpoints found / 404 errors**: Verify `RestApplication.java` exists with `@ApplicationPath` annotation. Check application deployed successfully. URL should be `/network-monitor/api/networks`.

**Authentication fails**: Ensure accounts exist in database with correct BCrypt hashed passwords. Check network access in `account_network` table.

## Features

✅ **MQTT Integration**: Subscribe to device scan results from network scanners  
✅ **State Change Detection**: Track devices going online/offline  
✅ **Historical Tracking**: Store all state changes with timestamps  
✅ **Multi-Network Support**: Monitor multiple networks simultaneously  
✅ **Device Management**: Classify devices as unauthorized, authorized, or always-on  
✅ **Automated Alerting**: Email notifications for network/device issues  
✅ **User Authentication**: Secure API with Jakarta Security and BCrypt  
✅ **Role-Based Access**: Admin, user, and device account types  
✅ **REST API**: Query current status and historical data  
✅ **OpenAPI Documentation**: Auto-generated API docs via MicroProfile OpenAPI

## Next Steps

Potential enhancements:

- Add web UI for device/network management dashboard
- Implement WebSocket for real-time device status updates
- Add metrics and health checks (MicroProfile Metrics/Health)
- Enhanced reporting and analytics
- Multi-language support for alert messages
- SMS/Slack/Teams alert integrations
- Device grouping and tagging
- Custom alert rules and thresholds

## Learning Resources

- **Jakarta EE Tutorial**: <https://eclipse-ee4j.github.io/jakartaee-tutorial/>
- **TomEE Documentation**: <https://tomee.apache.org/documentation.html>
- **Maven Guide**: <https://maven.apache.org/guides/>
- **JPA/Hibernate**: <https://hibernate.org/orm/documentation/>
