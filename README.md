# Network Monitor

A Jakarta EE 10 application for monitoring network devices via MQTT and exposing their status through a REST API.

## Overview

This application subscribes to MQTT topics that publish network device lists, detects when devices go online or offline, stores the state changes in a PostgreSQL database, and provides a REST API to query the current device status.

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

Execute the SQL script in `database/schema.sql` to create tables:

```powershell
psql -U postgres -d network_monitor -f database/schema.sql
```

Or connect with your PostgreSQL client and run the schema manually.

## Configuration

### 1. Application Configuration

Edit `src/main/resources/META-INF/microprofile-config.properties`:

- Set your MQTT broker URL, credentials, and topics
- Configure database connection details
- Adjust TLS/SSL settings if needed

### 2. Database Connection

The database connection is configured in two places:

**Development** (WEB-INF/resources.xml): Used when running in TomEE. Edit connection details here for local development.

**Production** (TomEE server config): For deployment, define the DataSource in TomEE's `conf/tomee.xml` instead.

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

Returns list of all monitored networks.

**Response:**

```json
[
  {
    "id": 1,
    "name": "MaliGrdi",
    "firstSeen": "2025-12-03T10:15:00",
    "lastSeen": "2025-12-04T14:30:00"
  }
]
```

### Get Online Devices for Network

```text
GET /api/networks/{networkName}/devices
```

Returns currently online devices for the specified network.

**Response:**

```json
[
  {
    "macAddress": "D8:B6:B7:F1:F8:E4",
    "ipAddress": "10.255.254.1",
    "online": true,
    "timestamp": "2025-12-04T14:30:00"
  }
]
```

## How It Works

### MQTT Message Processing

1. Application starts and connects to MQTT broker
2. Subscribes to configured topics (e.g., `network/MaliGrdi`)
3. Receives JSON messages with device lists
4. For each device in the message (online devices):
   - Checks previous status in database
   - If new or was offline: records "online" event
5. For devices in database but not in message:
   - Records "offline" event
6. Only state changes are stored, not every message

### Database Schema

**networks**: Stores monitored networks

- `id`: Primary key
- `name`: Network name (from MQTT topic)
- `first_seen`, `last_seen`: Tracking timestamps

**device_status_history**: Historical record of device state changes

- `id`: Primary key
- `network_id`: Foreign key to networks
- `mac_address`: Device MAC address (permanent identifier)
- `ip_address`: Device IP at time of event
- `online`: Boolean (true = came online, false = went offline)
- `timestamp`: When the change occurred

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

**MQTT Connection Fails**: Check broker URL, credentials, and TLS settings. Verify firewall rules.

**Database Connection Fails**: Ensure PostgreSQL is running and credentials in `resources.xml` are correct.

**ClassNotFoundException**: Usually means a dependency is missing from `pom.xml` or wrong scope.

**No endpoints found**: Verify `RestApplication.java` exists and has `@ApplicationPath` annotation.

## Next Steps

Potential enhancements for learning:

- Add authentication/authorization to REST API (Jakarta Security)
- Implement WebSocket for real-time device status updates
- Add metrics and health checks (MicroProfile Metrics/Health)
- Create a simple web UI for visualization
- Add unit tests (JUnit 5, Mockito)
- Implement caching for frequently accessed data

## Learning Resources

- **Jakarta EE Tutorial**: <https://eclipse-ee4j.github.io/jakartaee-tutorial/>
- **TomEE Documentation**: <https://tomee.apache.org/documentation.html>
- **Maven Guide**: <https://maven.apache.org/guides/>
- **JPA/Hibernate**: <https://hibernate.org/orm/documentation/>
