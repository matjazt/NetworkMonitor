package com.matjazt.networkmonitor.repository;

import java.util.List;
import java.util.Optional;

import com.matjazt.networkmonitor.entity.DeviceStatusHistory;
import com.matjazt.networkmonitor.entity.Network;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Repository for DeviceStatusHistory entities.
 */
@ApplicationScoped
public class DeviceStatusRepository {

    @PersistenceContext(unitName = "NetworkMonitorPU")
    private EntityManager em;

    /**
     * Find the most recent status record for a device in a network.
     * Used to determine current state before deciding if a change occurred.
     * 
     * @param network    The network
     * @param macAddress Device MAC address
     * @return Optional containing the latest status record if exists
     */
    public Optional<DeviceStatusHistory> findLatestStatus(Network network, String macAddress) {
        try {
            DeviceStatusHistory status = em.createQuery(
                    "SELECT d FROM DeviceStatusHistory d " +
                            "WHERE d.network = :network AND d.macAddress = :mac " +
                            "ORDER BY d.timestamp DESC",
                    DeviceStatusHistory.class)
                    .setParameter("network", network)
                    .setParameter("mac", macAddress)
                    .setMaxResults(1) // Only need the most recent
                    .getSingleResult();
            return Optional.of(status);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Get all currently online devices for a network.
     * 
     * This query finds devices whose most recent status is "online".
     * Uses a subquery to get the latest timestamp for each device.
     * 
     * @param network The network to query
     * @return List of status records for currently online devices
     */
    public List<DeviceStatusHistory> findCurrentlyOnline(Network network) {
        return em.createQuery(
                "SELECT d FROM DeviceStatusHistory d " +
                        "WHERE d.network = :network " +
                        "AND d.online = true " +
                        "AND d.timestamp = (" +
                        "  SELECT MAX(d2.timestamp) FROM DeviceStatusHistory d2 " +
                        "  WHERE d2.network = :network AND d2.macAddress = d.macAddress" +
                        ")",
                DeviceStatusHistory.class)
                .setParameter("network", network)
                .getResultList();
    }

    /**
     * Save a new status history record.
     * 
     * @param status The status record to save
     */
    @Transactional
    public void save(DeviceStatusHistory status) {
        em.persist(status);
    }
}
