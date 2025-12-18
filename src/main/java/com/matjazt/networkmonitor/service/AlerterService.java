package com.matjazt.networkmonitor.service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matjazt.networkmonitor.config.ConfigProvider;
import com.matjazt.networkmonitor.entity.DeviceEntity;
import com.matjazt.networkmonitor.entity.DeviceOperationMode;
import com.matjazt.networkmonitor.entity.NetworkEntity;
import com.matjazt.networkmonitor.repository.DeviceRepository;
import com.matjazt.networkmonitor.repository.DeviceStatusRepository;
import com.matjazt.networkmonitor.repository.NetworkRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.inject.Inject;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@Singleton
@Startup
public class AlerterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlerterService.class);

    @Resource
    private TimerService timerService;

    @Inject
    private ConfigProvider config;

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private DeviceRepository deviceRepository;

    @Inject
    private DeviceStatusRepository deviceStatusRepository;

    /**
     * Called automatically after dependency injection completes.
     * Creates a programmatic timer with configurable delay and interval.
     */
    @PostConstruct
    public void initialize() {
        LOGGER.info("starting up...");

        // Read configuration
        var initialDelaySeconds = config.getAlertCheckInitialDelay();
        var intervalSeconds = config.getAlertCheckInterval();

        // Create repeating timer
        TimerConfig timerConfig = new TimerConfig("AlertChecker", false);
        timerService.createIntervalTimer(
                initialDelaySeconds * 1000, // Initial delay in milliseconds
                intervalSeconds * 1000, // Interval in milliseconds
                timerConfig);

        LOGGER.info("Alert check timer created - initial delay: {}s, interval: {}s",
                initialDelaySeconds, intervalSeconds);
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

    public void sendAlert(String message, NetworkEntity network, DeviceEntity device) {
        if (network != null) {
            message += "\nNetwork: " + network.getName();
        }

        if (device != null) {
            message += "\nDevice: " + device.getName() + " (mac:" + device.getMacAddress() + ", ip:"
                    + device.getIpAddress() + ")";
        }

        LOGGER.warn("ALERT: {}", message);

        // Send email if network has an email address configured
        if (network != null && network.getEmailAddress() != null && !network.getEmailAddress().trim().isEmpty()) {
            try {
                // TODO: subject could be a parameter
                sendEmail(network.getEmailAddress(), "[" + network.getName() + "] network alert", message.toString());
                LOGGER.info("Alert email sent to: {}", network.getEmailAddress());
            } catch (Exception e) {
                LOGGER.error("Failed to send alert email", e);
                throw new RuntimeException("Failed to send alert email to " + network.getEmailAddress(), e);
            }
        }
    }

    /**
     * Sends an email using Jakarta Mail API.
     * 
     * @param to      recipient email address
     * @param subject email subject
     * @param body    email body
     * @throws MessagingException           if email could not be sent
     * @throws UnsupportedEncodingException
     */
    private void sendEmail(String to, String subject, String body)
            throws MessagingException, UnsupportedEncodingException {
        // Configure SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort().toString());
        props.put("mail.smtp.starttls.enable", config.getSmtpStartTlsEnable().toString());
        props.put("mail.smtp.auth", config.getSmtpAuthEnable().toString());
        props.put("mail.smtp.timeout", config.getSmtpTimeout().toString());
        props.put("mail.smtp.connectiontimeout", config.getSmtpConnectionTimeout().toString());

        // Create session with authentication if credentials provided
        Session session;
        if (config.getSmtpAuthEnable() && config.getSmtpUsername().isPresent()) {
            final String username = config.getSmtpUsername().get();
            final String password = config.getSmtpPassword().orElse("");

            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        // Create and send message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getSmtpFromAddress(), config.getSmtpFromName()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    /**
     * Scheduled alert checking triggered by programmatic timer.
     * Replaces @Schedule annotation for runtime configuration.
     */
    @Timeout
    public void checkForAlerts() {
        LOGGER.info("Running scheduled alert task...");

        // process networks one by one
        for (NetworkEntity network : networkRepository.findAll()) {
            processNetworkAlerts(network);
        }

    }

    @Transactional
    private void processNetworkAlerts(NetworkEntity network) {

        // see if the entire network is down or up

        var now = LocalDateTime.now(ZoneOffset.UTC);
        var alertingThreshold = now.minusSeconds(network.getAlertingDelay());

        if (network.getLastSeen().isBefore(alertingThreshold)) {
            // network is down
            if (network.getActiveAlarmTime() == null || network.getActiveAlarmTime().isBefore(network.getLastSeen())) {
                // network is down, alert hasn't been sent yet
                sendAlert("Network appears to be down!", network, null);
                network.setActiveAlarmTime(now);
                networkRepository.save(network);
            }
            // there's nothing else to do if the entire network is down
            return;
        }

        // network is up
        if (network.getActiveAlarmTime() != null) {
            // network was down, now it's back up - send recovery alert
            sendAlert("Network is back online.", network, null);
            network.setActiveAlarmTime(null);
            networkRepository.save(network);
        }

        // now check individual devices
        for (DeviceEntity device : deviceRepository.findAllForNetwork(network.getId())) {

            if (device.getDeviceOperationMode() == DeviceOperationMode.NOT_ALLOWED) {
                // the device is not allowed on the network
                // alerts for such cases are sent when the device first appears, so here we can
                // just check if it's gone
                if (device.getActiveAlarmTime() != null && device.getLastSeen().isBefore(alertingThreshold)) {
                    // device is gone, clear alarm
                    sendAlert("Unauthorized device is no longer detected on the network.", network, device);
                    device.setActiveAlarmTime(null);
                    deviceRepository.save(device);
                }
            } else if (device.getDeviceOperationMode() == DeviceOperationMode.ALLOWED) {
                // the device is allowed, no alerts needed, but we can clear any active alarms
                // in case they were set before (e.g., if the device was previously NOT_ALLOWED)
                if (device.getActiveAlarmTime() != null) {
                    sendAlert(
                            "Allowed device alarm cleared (it was previously not allowed or always on, hence the alarm).",
                            network, device);
                    device.setActiveAlarmTime(null);
                    deviceRepository.save(device);
                }
            } else if (device.getDeviceOperationMode() == DeviceOperationMode.ALWAYS_ON) {
                // the device should always be online, check its status
                if (device.getLastSeen().isBefore(alertingThreshold)) {
                    // device is down
                    if (device.getActiveAlarmTime() == null
                            || device.getActiveAlarmTime().isBefore(device.getLastSeen())) {
                        // device is down, alert hasn't been sent yet
                        sendAlert("Device appears to be down!", network, device);
                        device.setActiveAlarmTime(now);
                        deviceRepository.save(device);
                    }
                } else {
                    // device is up
                    if (device.getActiveAlarmTime() != null
                            && deviceStatusRepository.findLatestByMacAddress(network, device.getMacAddress())
                                    .getTimestamp().isBefore(alertingThreshold)) {
                        // device was down, now it's back up and has been up for long enough - send
                        // recovery alert

                        sendAlert("Device is back online.", network, device);
                        device.setActiveAlarmTime(null);
                        deviceRepository.save(device);
                    }
                }
            }
        }

    }

}
