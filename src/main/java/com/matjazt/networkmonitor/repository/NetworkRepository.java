package com.matjazt.networkmonitor.repository;

import java.util.List;
import java.util.Optional;

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
public class NetworkRepository {

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
    public Optional<NetworkEntity> findByName(String name) {
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
    public List<NetworkEntity> findAll() {
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
}
