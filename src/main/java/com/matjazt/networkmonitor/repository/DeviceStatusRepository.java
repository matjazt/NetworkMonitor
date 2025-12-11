package com.matjazt.networkmonitor.repository;

import java.util.List;

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

    public DeviceStatusHistory findLatestByMacAddress(Network network, String macAddress) {
        List<DeviceStatusHistory> results = em.createQuery(
                "SELECT d FROM DeviceStatusHistory d " +
                        "WHERE d.network = :network " +
                        "AND d.macAddress = :macAddress " +
                        "ORDER BY d.id DESC",
                DeviceStatusHistory.class)
                .setParameter("network", network)
                .setParameter("macAddress", macAddress)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
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
