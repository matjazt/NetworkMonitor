# Quick Start Guide

This guide walks you through setting up and running the Network Monitor application.

## Prerequisites

- PostgreSQL server running
- Apache TomEE 10.x installed
- JDK 17
- Apache Maven

## Step 1: Database Setup

Create the database and run the schema script:

```powershell
# Create database
psql -U postgres -c "CREATE DATABASE network_monitor;"

# Create tables and seed reference data
psql -U postgres -d network_monitor -f database/schema.sql
```

Verify tables were created:

```powershell
psql -U postgres -d network_monitor -c "\dt"
```

You should see: `account`, `account_network`, `account_type`, `alert`, `alert_type`, `device`, `device_operation_mode`, `device_status_history`, `network`.

## Step 2: Configure Application

Edit `src/main/resources/META-INF/microprofile-config.properties`:

```properties
# MQTT Settings
mqtt.broker.url=ssl://your-mqtt-broker:8883
mqtt.client.id=network-monitor-01
mqtt.username=your-username
mqtt.password=your-password
mqtt.topic.template=network/{networkName}/scan

# SMTP Settings (for alerts)
smtp.host=smtp.gmail.com
smtp.port=587
smtp.username=your-email@gmail.com
smtp.password=your-app-password
smtp.from.address=your-email@gmail.com
smtp.from.name=Network Monitor

# Alert Timing
alert.check.initial.delay=30
alert.check.interval=60
```

Also update database connection in `src/main/resources/META-INF/persistence.xml` if needed.

## Step 3: Build

Compile and package the application:

```powershell
mvn clean package
```

Output: `target/network-monitor.war`

## Step 4: Deploy to TomEE

Copy WAR to TomEE:

```powershell
# Adjust path to your TomEE installation
Copy-Item target/network-monitor.war C:\path\to\tomee\webapps\
```

Start TomEE:

```powershell
cd C:\path\to\tomee\bin
.\catalina.bat run
```

Watch the logs for successful deployment.

## Step 5: Create User Account

Create a user account for API access:

```powershell
# Generate BCrypt hash for password (use online tool or bcrypt library)
# Example password hash for "password123"

psql -U postgres -d network_monitor
```

```sql
INSERT INTO account (username, email, password_hash, full_name, account_type_id, created_at)
VALUES ('admin', 'admin@example.com', 
        '$2a$10$N9qo8uLOickgx2ZMRZoMye0IJxD.eONKXKQQMrZQFQTlXOvHs3Bam', 
        'Administrator', 1, NOW());
```

**Note**: The hash above is for password "password123". Use a proper BCrypt tool to generate secure hashes.

## Step 6: Create Network Entry

Add a network to monitor:

```sql
INSERT INTO network (name, first_seen, last_seen, alerting_delay, email_address)
VALUES ('TestNet', NOW(), NOW(), 300, 'alerts@example.com');
```

The application will automatically subscribe to the MQTT topic based on the template: `network/TestNet/scan`.

## Step 7: Verify Deployment

Check if application started and test REST API:

```powershell
# Test REST API (requires authentication)
Invoke-RestMethod -Uri http://localhost:8080/network-monitor/api/networks `
  -Headers @{Authorization=("Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:password123")))}
```

Response should include account info and networks array:

```json
{
  "accountId": 1,
  "accountFullName": "Administrator",
  "networks": [...]
}
```

## Step 8: Test MQTT Integration

Publish a test message to the MQTT broker:

```powershell
# For Windows PowerShell
Get-Content docs/mqtt-examples/example-message.json | mosquitto_pub `
  -h your-broker -p 8883 `
  -t network/TestNet/scan `
  -u username -P password `
  --cafile ca.crt -l

# For Linux/Mac
mosquitto_pub -h your-broker -p 8883 \
  -t network/TestNet/scan \
  -u username -P password \
  --cafile ca.crt \
  -f docs/mqtt-examples/example-message.json
```

Check database for new entries:

```powershell
psql -U postgres -d network_monitor -c "SELECT * FROM device;"
psql -U postgres -d network_monitor -c "SELECT * FROM device_status_history ORDER BY timestamp DESC LIMIT 10;"
```

## Step 9: Query via REST API

List networks:

```powershell
$cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:password123"))
Invoke-RestMethod -Uri http://localhost:8080/network-monitor/api/networks `
  -Headers @{Authorization="Basic $cred"}
```

Get online devices:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/network-monitor/api/networks/TestNet/devices `
  -Headers @{Authorization="Basic $cred"}
```

## Troubleshooting

**MQTT not connecting**: Check TomEE logs in `logs/catalina.out`. Verify broker URL, credentials, and TLS certificate path.

**Database errors**: Ensure PostgreSQL is running and credentials in `persistence.xml` are correct. Verify schema was loaded successfully.

**Authentication fails**: Verify account exists and password hash is correct. BCrypt hash must match password.

**Alert emails not sending**: Check SMTP configuration in `microprofile-config.properties`. Test SMTP connectivity separately. Review logs for email errors.

**WAR not deploying**: Check TomEE logs for deployment errors. Verify Java 17 is being used: `java -version`.

**404 on REST endpoints**: Verify application deployed successfully. Check URL includes context path: `/network-monitor/api/networks`.

## Development Workflow

1. Make code changes
2. Stop TomEE (`Ctrl+C`)
3. Rebuild: `mvn clean package`
4. Redeploy WAR
5. Restart TomEE

For faster iteration, consider setting up hot reload or using the TomEE Maven plugin.
