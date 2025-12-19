package com.matjazt.networkmonitor.service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matjazt.networkmonitor.config.ConfigProvider;
import com.matjazt.networkmonitor.entity.AlarmEntity;
import com.matjazt.networkmonitor.entity.AlarmType;
import com.matjazt.networkmonitor.entity.DeviceEntity;
import com.matjazt.networkmonitor.entity.DeviceOperationMode;
import com.matjazt.networkmonitor.entity.NetworkEntity;
import com.matjazt.networkmonitor.repository.AlarmRepository;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Singleton
@Startup
public class AlerterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlerterService.class);

    @Resource
    private TimerService timerService;

    @Inject
    private ConfigProvider config;

    @PersistenceContext(unitName = "NetworkMonitorPU")
    private EntityManager entityManager;

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private DeviceRepository deviceRepository;

    @Inject
    private AlarmRepository alarmRepository;

    @Inject
    private DeviceStatusRepository deviceStatusRepository;

    private static final Map<AlarmType, String> ALARM_TYPE_MESSAGES = Map.ofEntries(
            Map.entry(AlarmType.NETWORK_DOWN, "Network is unavailable"),
            Map.entry(AlarmType.DEVICE_DOWN, "Device is offline"),
            Map.entry(AlarmType.UNAUTHORIZED_DEVICE, "Unauthorized device detected"));

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

    public void sendAlert(AlarmType alarmType, boolean closure, NetworkEntity network, DeviceEntity device,
            String message) {

        String baseMessage = ALARM_TYPE_MESSAGES.get(alarmType);
        if (baseMessage == null) {
            throw new IllegalArgumentException("Unsupported alarm type: " + alarmType);
        }

        var subject = "[" + network.getName() + "] ";

        var fullMessageEntries = new ArrayList<String>();

        if (closure) {
            fullMessageEntries.add("ALERT CLOSED");
            subject += " alert closure";
        } else {
            fullMessageEntries.add("ALERT TRIGGERED");
            subject += " alert";
        }
        fullMessageEntries.add(""); // empty line

        if (network != null) {
            fullMessageEntries.add("Network: " + network.getName());
        }

        if (device != null) {
            fullMessageEntries.add("Device: " + device.getName() + " (mac:" + device.getMacAddress() + ", ip:"
                    + device.getIpAddress() + ")");
        }
        fullMessageEntries.add("UTC time: " + LocalDateTime.now(ZoneOffset.UTC).toString());
        fullMessageEntries.add(""); // empty line

        fullMessageEntries.add(baseMessage + ".");

        if (message != null && !message.isBlank()) {
            fullMessageEntries.add(""); // empty line
            fullMessageEntries.add("Additional info: " + message);
        }

        var fullMessage = String.join(System.lineSeparator(), fullMessageEntries);
        LOGGER.warn("fullMessage: {}", fullMessage);

        // Send email if network has an email address configured
        if (network != null && network.getEmailAddress() != null && !network.getEmailAddress().isEmpty()) {
            // figure out email subject

            if (device != null) {
                subject += " for device: " + device.getNameOrMac();
            }

            try {
                sendEmail(network.getEmailAddress(), subject, fullMessage);
                LOGGER.info("Alert email sent to: {}", network.getEmailAddress());
            } catch (Exception e) {
                LOGGER.error("Failed to send alert email", e);
                throw new RuntimeException("Failed to send alert email to " + network.getEmailAddress(), e);
            }
        }
    }

    public AlarmEntity openAlert(AlarmType alarmType, NetworkEntity network, DeviceEntity device, String message) {

        LOGGER.info("alarmType={}, network={}, device={}, message={}",
                alarmType, network.getName(),
                device != null ? device.getNameOrMac() : "N/A",
                message);

        // load latest alarm for this network/device and check if it's closed
        var latestAlarmOpt = alarmRepository.getLatestAlarm(network, device);
        if (latestAlarmOpt.isPresent() && latestAlarmOpt.get().getClosureTimestamp() == null) {
            throw new IllegalStateException("There's already an open alarm for this network/device");
        }

        // store alarm in database
        var alarm = alarmRepository.createAlarm(
                network,
                device,
                alarmType,
                message);

        // ensure INSERT is executed and ID is available
        // entityManager.flush();

        // store it also in the entity
        if (device == null) {
            network.setActiveAlarmId(alarm.getId());
            networkRepository.save(network);
        } else {
            device.setActiveAlarmId(alarm.getId());
            deviceRepository.save(device);
        }

        // send alert notification
        sendAlert(alarmType, false, network, device, message);

        // return created alarm (including its ID)
        return alarm;
    }

    public AlarmEntity closeAlert(NetworkEntity network, DeviceEntity device, String message) {

        LOGGER.info("network={}, device={}, message={}",
                network.getName(),
                device != null ? device.getNameOrMac() : "N/A",
                message);

        // load latest alarm for this network/device and check if it's closed
        var latestAlarmOpt = alarmRepository.getLatestAlarm(network, device);
        if (!latestAlarmOpt.isPresent() || latestAlarmOpt.get().getClosureTimestamp() != null) {
            throw new IllegalStateException("There's no open alarm for this network/device");
        }

        var alarm = latestAlarmOpt.get();

        // close alarm in database
        alarm.setClosureTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        alarmRepository.save(alarm);

        // close it also in the entity
        if (device == null) {
            network.setActiveAlarmId(null);
            networkRepository.save(network);
        } else {
            device.setActiveAlarmId(null);
            deviceRepository.save(device);
        }

        // append the information about the alarm we are closing to the message: alarm
        // timestamp and duration
        var duration = java.time.Duration.between(alarm.getTimestamp(), alarm.getClosureTimestamp());
        String durationInfo = "Alert opened at: " + alarm.getTimestamp().toString() + "\nDuration: "
                + String.format("%d days, %d hours, %d minutes, %d seconds",
                        duration.toDaysPart(),
                        duration.toHoursPart(),
                        duration.toMinutesPart(),
                        duration.toSecondsPart());
        message = (message != null ? message.trim() : "") + "\n" + durationInfo;

        // send alert notification
        sendAlert(alarm.getAlarmType(), true, network, device, message);

        // return closed alarm
        return alarm;
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
            if (network.getActiveAlarmId() == null) {
                // network is down, alert hasn't been sent yet
                openAlert(AlarmType.NETWORK_DOWN, network, null, null);
            }
            // there's nothing else to do if the entire network is down
            return;
        }

        // network is up
        if (network.getActiveAlarmId() != null) {
            // network was down, now it's back up - send recovery alert
            closeAlert(network, null, null);
        }

        // now check individual devices
        for (DeviceEntity device : deviceRepository.findAllForNetwork(network.getId())) {

            if (device.getDeviceOperationMode() == DeviceOperationMode.UNAUTHORIZED) {
                // the device is not allowed on the network
                // alerts for such cases are sent when the device first appears, so here we can
                // just check if it's gone
                if (device.getActiveAlarmId() != null && device.getLastSeen().isBefore(alertingThreshold)) {
                    // device is gone, clear alarm
                    closeAlert(network, device, null);
                }
            } else if (device.getDeviceOperationMode() == DeviceOperationMode.ALLOWED) {
                // the device is allowed, no alerts needed, but we can clear any active alarms
                // in case they were set before (e.g., if the device was previously
                // UNAUTHORIZED)
                if (device.getActiveAlarmId() != null) {
                    closeAlert(network, device, "device is now authorized");
                }
            } else if (device.getDeviceOperationMode() == DeviceOperationMode.ALWAYS_ON) {
                // the device should always be online, check its status
                if (device.getLastSeen().isBefore(alertingThreshold)) {
                    // device is down, alert hasn't been sent yet
                    if (device.getActiveAlarmId() == null) {
                        openAlert(AlarmType.DEVICE_DOWN, network, device, null);
                    }
                } else {
                    // device is up
                    if (device.getActiveAlarmId() != null
                            && deviceStatusRepository.findLatestByMacAddress(network, device.getMacAddress())
                                    .getTimestamp().isBefore(alertingThreshold)) {
                        // device was down, now it's back up and has been up for long enough - send
                        // recovery alert
                        closeAlert(network, device, null);
                    }
                }
            }
        }

    }

}
