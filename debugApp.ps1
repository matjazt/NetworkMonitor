# remove old deployment
if (Test-Path "S:\Razno\TomEE\webapps\network-monitor.war") {
    Remove-Item S:\Razno\TomEE\webapps\network-monitor.war -Force   
}
if (Test-Path "S:\Razno\TomEE\webapps\network-monitor") {
    Remove-Item S:\Razno\TomEE\webapps\network-monitor -Recurse -Force
}


# Build and deploy
mvn clean package
Copy-Item target\network-monitor.war S:\Razno\TomEE\webapps\

# Set debug port
$env:JPDA_ADDRESS = "8000"
$env:JPDA_TRANSPORT = "dt_socket"
$env:JPDA_SUSPEND = "y"

# Start TomEE in debug mode
$env:CATALINA_HOME = "S:\Razno\TomEE"
S:\Razno\TomEE\bin\catalina.bat jpda run

#Start-Process -FilePath "catalina.bat" -ArgumentList "jpda", "run" -WorkingDirectory "S:\Razno\TomEE\bin"
