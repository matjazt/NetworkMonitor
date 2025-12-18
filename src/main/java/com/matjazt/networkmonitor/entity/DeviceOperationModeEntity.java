package com.matjazt.networkmonitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for device_operation_mode reference table.
 * This entity exists for OpenJPA schema validation and foreign key
 * relationships.
 * Runtime code uses DeviceOperationMode enum directly - this entity is never
 * queried.
 */
@Entity
@Table(name = "device_operation_mode")
public class DeviceOperationModeEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    // No getters/setters needed - this entity is never used in runtime code
}
