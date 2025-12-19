package com.matjazt.networkmonitor.repository;

import java.util.List;

import com.matjazt.networkmonitor.entity.DeviceEntity;
import com.matjazt.networkmonitor.entity.DeviceStatusHistoryEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;

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
    public List<DeviceStatusHistoryEntity> findCurrentlyOnline(NetworkEntity network) {
        return em.createQuery(
                "SELECT d FROM DeviceStatusHistoryEntity d " +
                        "WHERE d.network = :network " +
                        "AND d.online = true " +
                        "AND d.timestamp = (" +
                        "  SELECT MAX(d2.timestamp) FROM DeviceStatusHistoryEntity d2 " +
                        "  WHERE d2.network = :network AND d2.device = d.device" +
                        ")",
                DeviceStatusHistoryEntity.class)
                .setParameter("network", network)
                .getResultList();
    }

    public DeviceStatusHistoryEntity findLatestByDevice(NetworkEntity network, DeviceEntity device) {
        List<DeviceStatusHistoryEntity> results = em.createQuery(
                "SELECT d FROM DeviceStatusHistoryEntity d " +
                        "WHERE d.network = :network " +
                        "AND d.device = :device " +
                        "ORDER BY d.id DESC",
                DeviceStatusHistoryEntity.class)
                .setParameter("network", network)
                .setParameter("device", device)
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
    public void save(DeviceStatusHistoryEntity status) {
        em.persist(status);
    }
}
