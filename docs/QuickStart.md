# Quick Start Guide

This guide walks you through setting up and running the Network Monitor application.

## Step 1: Database Setup

Create the database and schema:

```powershell
# Create database
psql -U postgres -c "CREATE DATABASE network_monitor;"

# Create tables
psql -U postgres -d network_monitor -f database/schema.sql
```

Verify tables were created:

```powershell
psql -U postgres -d network_monitor -c "\dt"
```

## Step 2: Configure Application

Edit `src/main/resources/META-INF/microprofile-config.properties`:

```properties
# MQTT Settings
mqtt.broker.url=ssl://your-mqtt-broker:8883
mqtt.client.id=network-monitor-01
mqtt.username=your-username
mqtt.password=your-password
mqtt.topics=network/MaliGrdi,network/Office

# Database Settings (also update in resources.xml)
db.url=jdbc:postgresql://localhost:5432/network_monitor
db.username=postgres
db.password=postgres
```

Also update `src/main/webapp/WEB-INF/resources.xml` with your database credentials.

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

## Step 5: Verify Deployment

Check if application started:

```powershell
# Test REST API
Invoke-RestMethod http://localhost:8080/network-monitor/api/networks
```

Should return an empty array `[]` initially.

## Step 6: Test MQTT Integration

Publish a test message:

```powershell
mosquitto_pub -h your-broker -p 8883 `
  -t network/TestNet `
  -u username -P password `
  --cafile ca.crt `
  -f docs/mqtt-examples/example-message.json
```

Check database:

```powershell
psql -U postgres -d network_monitor -c "SELECT * FROM networks;"
psql -U postgres -d network_monitor -c "SELECT * FROM device_status_history;"
```

## Step 7: Query via REST API

List networks:

```powershell
Invoke-RestMethod http://localhost:8080/network-monitor/api/networks
```

Get online devices:

```powershell
Invoke-RestMethod http://localhost:8080/network-monitor/api/networks/TestNet/devices
```

## Troubleshooting

**MQTT not connecting**: Check TomEE logs in `logs/catalina.out`. Verify broker URL and credentials.

**Database errors**: Ensure PostgreSQL is running and credentials are correct in both config files.

**WAR not deploying**: Check TomEE logs for errors. Verify Java 17 is being used.

**404 on REST endpoints**: Verify application deployed successfully. URL should be `/network-monitor/api/networks`.

## Development Workflow

1. Make code changes
2. Stop TomEE (`Ctrl+C`)
3. Rebuild: `mvn clean package`
4. Redeploy WAR
5. Restart TomEE

For faster iteration, consider setting up hot reload or using the TomEE Maven plugin.
