package com.memory.context.engine.domain.relationship.entity;

import com.memory.context.engine.domain.memory.entity.Memory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a directed relationship between two memories.
 * Enables graph traversal for connected memories.
 */
@Entity
@Table(name = "memory_relationships", indexes = {
        @Index(name = "idx_relationships_source", columnList = "source_memory_id"),
        @Index(name = "idx_relationships_target", columnList = "target_memory_id")
}, uniqueConstraints = @UniqueConstraint(columnNames = { "source_memory_id", "target_memory_id", "relationship_type" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoryRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_memory_id", nullable = false)
    private Memory sourceMemory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_memory_id", nullable = false)
    private Memory targetMemory;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private RelationshipType relationshipType;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal strength = BigDecimal.ONE;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
