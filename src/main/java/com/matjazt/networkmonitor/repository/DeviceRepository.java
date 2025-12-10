package com.matjazt.networkmonitor.repository;

import java.util.List;

import com.matjazt.networkmonitor.entity.Device;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Repository (Data Access Layer) for Device entities.
 * 
 * Repositories handle database operations for a specific entity.
 * Similar to Entity Framework's DbContext/DbSet pattern.
 * 
 * @ApplicationScoped: single instance shared across the application.
 * @Transactional: methods automatically run in database transactions.
 */
@ApplicationScoped
public class DeviceRepository {

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
     * Get all devices for a specific network.
     * 
     * @param networkId The ID of the network
     * @return List of all devices for the given network ID
     */
    public List<Device> findAllForNetwork(long networkId) {
        return em.createQuery("SELECT n FROM Device n WHERE n.network.id = :networkId", Device.class)
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
    public Device save(Device device) {
        if (device.getId() == null) {
            // New entity - persist adds it to the database
            em.persist(device);
            return device;
        } else {
            // Existing entity - merge updates it
            return em.merge(device);
        }
    }
}
