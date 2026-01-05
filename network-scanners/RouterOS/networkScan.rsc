# Network Scanner Script for RouterOS 7
# Scans network ranges, monitors specific nodes, and reports to MQTT

# Read configuration from file
:local configFile "etc/networkScan.json";
:local configContent "";

:do {
    :set configContent [/file get $configFile contents];
    :put ("Configuration loaded from " . $configFile);
} on-error={
    :put ("Error: Could not read configuration file " . $configFile);
    :error ("Configuration file not found");
}

# Parse JSON configuration using RouterOS 7 built-in JSON parser
:local networkScanConfig;

# Parse the JSON configuration file
:do {
    :set networkScanConfig [:deserialize from=json value=$configContent];
    :put ("Configuration parsed successfully");
    :put ("Ranges: " . [:len ($networkScanConfig->"ranges")]);
    :put ("Pingable nodes: " . [:len ($networkScanConfig->"pingableNodes")]);
} on-error={
    :put "Error: Failed to parse JSON configuration";
    :error "Invalid JSON format in configuration file";
}

# Function to perform ping with multiple packets
:local pingWithCount do={
    :local ip $1;
    :local count $2;
    :local timeout $3;
    
    :local success false;
    
    :do {
        # TODO: suppress output? I've tried 'as-value' but couldn't parse result properly.
        :local result [:ping $ip count=$count interval=($timeout."ms")];
        # If any packets were received, consider it successful
        :if ($result > 0) do={
            :set success true;
        }
    } on-error={
        :set success false;
    }
    
    :return $success;
}

# Function to format timestamp
:local getTimestampOld do={
    :local date [/system clock get date];
    :local time [/system clock get time];
    :return ($date . " " . $time);
}

:local getTimestamp do={
    :local offset [/system clock get gmt-offset];
    :local date [/system clock get date];
    :local time [/system clock get time];
    :local sign ("+")
    if ($offset < 0) do={
        :set sign "-";
        :set offset (-1 * $offset);
    }

    # convert offset to hh:mm format
    :local offsetHoursStr "";
    :local offsetMinsStr "";
    :set offsetHoursStr [:tostr ($offset / 3600)];
    :set offsetMinsStr [:tostr (($offset % 3600) / 60)];
    # Pad with leading zeros if necessary
    :if ([:len $offsetHoursStr] = 1) do={
        :set offsetHoursStr ("0" . $offsetHoursStr);
    }
    :if ([:len $offsetMinsStr] = 1) do={
        :set offsetMinsStr ("0" . $offsetMinsStr);
    }

    :return ($date . "T" . $time . $sign . $offsetHoursStr . ":" . $offsetMinsStr);
}

:put "Network Scanner Starting...";
:put ("Timestamp: " . [$getTimestamp]);

# Step 1: Clear old ARP entries (optional - comment out if not desired)
# /ip arp remove [find dynamic=yes]

# Step 2: Ping all ranges to populate ARP cache
:put "Pinging network ranges to populate ARP cache...";

:foreach range in=($networkScanConfig->"ranges") do={
    :local subnet ($range->"subnet");
    :local startAddr ($range->"start");
    :local endAddr ($range->"end");
    
    # Extract the first three octets from the subnet address
    # Find dot positions
    :local p1 [:find $subnet "."]
    :local p2 [:find $subnet "." ($p1 + 1)]
    :local p3 [:find $subnet "." ($p2 + 1)]
    # Base = everything before the third dot
    :local base [:pick $subnet 0 $p3]

    :put ("Scanning range: " . $base . "." . $startAddr . " - " . $base . "." . $endAddr);
    
    :for addr from=$startAddr to=$endAddr do={
        :local targetIp ($base . "." . $addr);
        
        # Quick ping to populate ARP cache
        :do {
            :ping $targetIp count=1 interval=20ms as-value;
        } on-error={
            # Ignore ping failures at this stage
        }       
    }
}

# Step 3: Process ARP cache and build device list
:put "ARP cache population complete, processing it...";

:local devices [:toarray ""];

# Get all ARP entries directly, check them and compose JSON on the fly
:local identity [/system identity get name];
:local jsonOutput ("{ \"hostname\": \"" . $identity . "\", \"timestamp\": \"" . [$getTimestamp] . "\", \"devices\": [");
:local firstDevice true;

:foreach a in=[/ip arp print as-value] do={
    :local deviceIp ($a->"address");
    :local deviceMac ($a->"mac-address");
    :local status ($a->"status");
    
    # only process entries with valid IP and MAC
    :if ([:len $deviceMac] > 0 && $deviceMac != "00:00:00:00:00:00") do={
       
        :local isOnline false;
        :local isPingableNode false;
        :local nodeConfig;
        
        # Check if this IP is a pingable node
        :foreach node in=($networkScanConfig->"pingableNodes") do={
            :if (($node->"ip") = $deviceIp) do={
                :set isPingableNode true;
                :set nodeConfig $node;
            }
        }
        
        :if ($isPingableNode) do={
            # Ping with specific parameters for pingable nodes
            :local count ($nodeConfig->"count");
            :local timeout ($nodeConfig->"timeout");
            :set isOnline [$pingWithCount $deviceIp $count $timeout];
            :put ("Pingable node " . $deviceIp . ": " . $isOnline);
        } else={
            # For non-pingable nodes, consider them online if in ARP cache and have valid MAC
            # and IP address (there can be incomplete entries in arp output)
            :if ([:len $deviceIp] > 0 && [:len $deviceMac] > 0 && ($status != "failed") && ($status != "incomplete")) do={
                # For regular devices, assume online if in ARP cache
                :set isOnline true;
            }
        }
        
        # Only add online devices to list
        :if ($isOnline) do={

            :local jsonElement ("{ \"ip\":\"" . ($deviceIp) . "\", \"mac\":\"" . ($deviceMac) . "\" }");
            :put ("Found online device: " . $jsonElement);

            :if ($firstDevice = false) do={
                :set jsonOutput ($jsonOutput . ",");
            }
            :set firstDevice false;

            :set jsonOutput ($jsonOutput . $jsonElement);
        }
    }
}
# Close JSON array
:set jsonOutput ($jsonOutput . "]}");

# Step 5: Publish to MQTT
:local broker ($networkScanConfig->"mqtt"->"broker");
:local topic ($networkScanConfig->"mqtt"->"topic");

:put ("Publishing to MQTT broker: " . $broker . ", topic: " . $topic);

# Publish to MQTT
:do {
    /iot mqtt publish broker=$broker topic=$topic message=$jsonOutput qos=1;
    :put "Successfully published to MQTT.";
} on-error={
    :put "Error: Failed to publish to MQTT broker.";
}

:put "Scan complete.";

