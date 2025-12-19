package com.matjazt.networkmonitor.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.matjazt.networkmonitor.entity.AlertEntity;
import com.matjazt.networkmonitor.entity.AlertType;
import com.matjazt.networkmonitor.entity.DeviceEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 * Repository for managing alert records.
 */
@Stateless
public class AlertRepository {

    @PersistenceContext(unitName = "NetworkMonitorPU")
    private EntityManager entityManager;

    /**
     * Save an alert to the database.
     */
    public AlertEntity save(AlertEntity alert) {
        if (alert.getId() == null) {
            entityManager.persist(alert);
            return alert;
        } else {
            return entityManager.merge(alert);
        }
    }

    /**
     * Create and save a new alert.
     */
    public AlertEntity createAlert(NetworkEntity network, DeviceEntity device,
            AlertType alertType, String message) {
        AlertEntity alert = new AlertEntity(
                LocalDateTime.now(ZoneOffset.UTC),
                network,
                device,
                alertType,
                message);
        return save(alert);
    }

    /**
     * Get the latest alert for a specific network.
     * 
     * @param network The network to search for
     * @return The most recent alert for this network, or empty if none exists
     */
    public Optional<AlertEntity> getLatestAlertForNetwork(NetworkEntity network) {
        try {
            AlertEntity alert = entityManager.createQuery(
                    "SELECT a FROM AlertEntity a WHERE a.network = :network AND a.device IS NULL " +
                            "ORDER BY a.timestamp DESC",
                    AlertEntity.class)
                    .setParameter("network", network)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(alert);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Get the latest alert for a specific device.
     * 
     * @param network The network the device belongs to
     * @param device  The device to search for
     * @return The most recent alert for this device, or empty if none exists
     */
    public Optional<AlertEntity> getLatestAlertForDevice(NetworkEntity network, DeviceEntity device) {
        try {
            AlertEntity alert = entityManager.createQuery(
                    "SELECT a FROM AlertEntity a WHERE a.network = :network AND a.device = :device " +
                            "ORDER BY a.timestamp DESC",
                    AlertEntity.class)
                    .setParameter("network", network)
                    .setParameter("device", device)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(alert);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Get the latest alert for a network or device.
     * 
     * @param network The network to search for
     * @param device  The device to search for (null for network-level alerts)
     * @return The most recent alert, or empty if none exists
     */
    public Optional<AlertEntity> getLatestAlert(NetworkEntity network, DeviceEntity device) {
        if (device == null) {
            return getLatestAlertForNetwork(network);
        } else {
            return getLatestAlertForDevice(network, device);
        }
    }

    /**
     * Get an alert by its ID.
     * 
     * @param id The alert ID
     * @return The alert, or empty if not found
     */
    public Optional<AlertEntity> getAlertById(Long id) {
        AlertEntity alert = entityManager.find(AlertEntity.class, id);
        return Optional.ofNullable(alert);
    }
}
