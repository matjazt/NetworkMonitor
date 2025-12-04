package com.matjazt.networkmonitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a monitored network.
 * 
 * JPA (Jakarta Persistence API) is similar to Entity Framework in .NET.
 * Entities are POJOs (Plain Old Java Objects) that map to database tables.
 * 
 * This entity stores basic information about each monitored network.
 * The network name is extracted from the MQTT topic.
 */
@Entity  // Marks this class as a database entity
@Table(name = "networks")  // Maps to "networks" table in database
public class Network {

    @Id  // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment by database
    private Long id;

    /**
     * Network name (extracted from MQTT topic).
     * For topic "network/MaliGrdi", this would be "MaliGrdi".
     * Unique constraint ensures we don't duplicate networks.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * When this network was first seen.
     * @Column with columnDefinition allows us to use PostgreSQL's TIMESTAMP type.
     */
    @Column(name = "first_seen", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime firstSeen;

    /**
     * When we last received data for this network.
     */
    @Column(name = "last_seen", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime lastSeen;

    // JPA requires a no-argument constructor
    public Network() {
    }

    public Network(String name) {
        this.name = name;
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    // Getters and setters - standard Java bean pattern
    // In Java, private fields are accessed via public methods (encapsulation)
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Updates the last seen timestamp to now.
     */
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }
}
