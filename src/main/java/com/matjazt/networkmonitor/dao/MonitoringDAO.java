package com.matjazt.networkmonitor.dao;

import java.util.List;
import java.util.Optional;

import com.matjazt.networkmonitor.entity.DeviceEntity;
import com.matjazt.networkmonitor.entity.DeviceStatusHistoryEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Repository (Data Access Layer) for Network entities.
 * 
 * Repositories handle database operations for a specific entity.
 * Similar to Entity Framework's DbContext/DbSet pattern.
 * 
 * @ApplicationScoped: single instance shared across the application.
 * @Transactional: methods automatically run in database transactions.
 */
@ApplicationScoped
public class MonitoringDAO {

    /**
     * EntityManager is JPA's main interface for database operations.
     * Similar to DbContext in Entity Framework.
     * 
     * @PersistenceContext injects the EntityManager automatically.
     *                     Container manages its lifecycle (creation, transaction
     *                     binding, cleanup).
     */
    @PersistenceContext(unitName = "NetworkMonitorPU")
    private EntityManager em;

    /**
     * Find a network by its name.
     * 
     * @param name Network name to search for
     * @return Optional containing the network if found, empty otherwise
     */
    public Optional<NetworkEntity> findNetworkByName(String name) {
        try {
            // JPQL (JPA Query Language) - similar to SQL but uses entity names
            // :name is a named parameter to prevent SQL injection
            NetworkEntity network = em.createQuery(
                    "SELECT n FROM NetworkEntity n WHERE n.name = :name", NetworkEntity.class)
                    .setParameter("name", name)
                    .getSingleResult();
            return Optional.of(network);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Get all networks.
     * 
     * @return List of all networks ordered by name
     */
    public List<NetworkEntity> findAllNetworks() {
        return em.createQuery("SELECT n FROM NetworkEntity n ORDER BY n.id", NetworkEntity.class)
                .getResultList();
    }

    /**
     * Save a new network or update existing one.
     * 
     * @param network Network to save
     * @return The saved/updated network (with ID if new)
     */
    @Transactional // Requires transaction for write operations
    public NetworkEntity save(NetworkEntity network) {
        if (network.getId() == null) {
            // New entity - persist adds it to the database
            em.persist(network);
            return network;
        } else {
            // Existing entity - merge updates it
            return em.merge(network);
        }
    }

    /**
     * Get all devices for a specific network.
     * 
     * @param networkId The ID of the network
     * @return List of all devices for the given network ID
     */
    public List<DeviceEntity> findAllDevicesForNetwork(long networkId) {
        return em.createQuery("SELECT n FROM DeviceEntity n WHERE n.network.id = :networkId", DeviceEntity.class)
                .setParameter("networkId", networkId)
                .getResultList();
    }

    /**
     * Save a new device or update existing one.
     * 
     * @param device Device to save
     * @return The saved/updated device (with ID if new)
     */
    @Transactional // Requires transaction for write operations
    public DeviceEntity save(DeviceEntity device) {
        if (device.getId() == null) {
            // New entity - persist adds it to the database
            em.persist(device);
            return device;
        } else {
            // Existing entity - merge updates it
            return em.merge(device);
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
    public List<DeviceStatusHistoryEntity> findCurrentlyOnlineDevices(NetworkEntity network) {
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

    public DeviceStatusHistoryEntity findLatestHistoryEntryByDevice(NetworkEntity network, DeviceEntity device) {
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
