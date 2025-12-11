package com.matjazt.networkmonitor.service;

import java.util.logging.Logger;

import com.matjazt.networkmonitor.config.ConfigProvider;
import com.matjazt.networkmonitor.entity.Device;
import com.matjazt.networkmonitor.entity.Network;
import com.matjazt.networkmonitor.repository.DeviceRepository;
import com.matjazt.networkmonitor.repository.NetworkRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Singleton
@Startup
public class AlerterService {

    private static final Logger LOGGER = Logger.getLogger(AlerterService.class.getName());

    @Inject
    private ConfigProvider config;

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private DeviceRepository deviceRepository;

    /**
     * Called automatically after dependency injection completes.
     * Similar to .NET's IHostedService.StartAsync().
     * 
     * @PostConstruct is a lifecycle callback - runs once during initialization.
     */
    @PostConstruct
    public void initialize() {
        LOGGER.info("starting up...");
    }

    /**
     * Called automatically when application is shutting down.
     * Similar to .NET's IHostedService.StopAsync().
     * 
     * @PreDestroy is a lifecycle callback for cleanup.
     */
    @PreDestroy
    public void cleanup() {
        LOGGER.info("shutting down...");
    }

    void sendAlert(String message, Network network, Device device) {
        if (network != null) {
            message += "\nNetwork: " + network.getName();
        }

        if (device != null) {
            message += "\nDevice: " + device.getName() + " (mac:" + device.getMacAddress() + ", ip:"
                    + device.getIpAddress() + ")";
        }

        LOGGER.warning("ALERT: " + message);
    }

    // Runs every 60 seconds automatically
    @Schedule(second = "0,10,20,30,40,50", minute = "*", hour = "*")
    public void checkForAlerts() {
        LOGGER.info("Running scheduled alert task...");

        // process networks one by one
        for (Network network : networkRepository.findAll()) {
            processNetworkAlerts(network);
        }

    }

    @Transactional
    private void processNetworkAlerts(Network network) {
        // see if the entire network is down for long enough

        // var now = LocalDateTime.now();

    }
}
