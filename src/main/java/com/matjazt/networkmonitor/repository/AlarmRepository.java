package com.matjazt.networkmonitor.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.matjazt.networkmonitor.entity.AlarmEntity;
import com.matjazt.networkmonitor.entity.AlarmType;
import com.matjazt.networkmonitor.entity.DeviceEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 * Repository for managing alarm records.
 */
@Stateless
public class AlarmRepository {

    @PersistenceContext(unitName = "NetworkMonitorPU")
    private EntityManager entityManager;

    /**
     * Save an alarm to the database.
     */
    public AlarmEntity save(AlarmEntity alarm) {
        if (alarm.getId() == null) {
            entityManager.persist(alarm);
            return alarm;
        } else {
            return entityManager.merge(alarm);
        }
    }

    /**
     * Create and save a new alarm.
     */
    public AlarmEntity createAlarm(NetworkEntity network, DeviceEntity device,
            AlarmType alarmType, String message) {
        AlarmEntity alarm = new AlarmEntity(
                LocalDateTime.now(),
                network,
                device,
                alarmType,
                message);
        return save(alarm);
    }

    /**
     * Get the latest alarm for a specific network.
     * 
     * @param network The network to search for
     * @return The most recent alarm for this network, or empty if none exists
     */
    public Optional<AlarmEntity> getLatestAlarmForNetwork(NetworkEntity network) {
        try {
            AlarmEntity alarm = entityManager.createQuery(
                    "SELECT a FROM AlarmEntity a WHERE a.network = :network AND a.device IS NULL " +
                            "ORDER BY a.timestamp DESC",
                    AlarmEntity.class)
                    .setParameter("network", network)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(alarm);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Get the latest alarm for a specific device.
     * 
     * @param network The network the device belongs to
     * @param device  The device to search for
     * @return The most recent alarm for this device, or empty if none exists
     */
    public Optional<AlarmEntity> getLatestAlarmForDevice(NetworkEntity network, DeviceEntity device) {
        try {
            AlarmEntity alarm = entityManager.createQuery(
                    "SELECT a FROM AlarmEntity a WHERE a.network = :network AND a.device = :device " +
                            "ORDER BY a.timestamp DESC",
                    AlarmEntity.class)
                    .setParameter("network", network)
                    .setParameter("device", device)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(alarm);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Get the latest alarm for a network or device.
     * 
     * @param network The network to search for
     * @param device  The device to search for (null for network-level alarms)
     * @return The most recent alarm, or empty if none exists
     */
    public Optional<AlarmEntity> getLatestAlarm(NetworkEntity network, DeviceEntity device) {
        if (device == null) {
            return getLatestAlarmForNetwork(network);
        } else {
            return getLatestAlarmForDevice(network, device);
        }
    }
}
