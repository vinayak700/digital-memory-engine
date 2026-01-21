package com.memory.context.engine.domain.memory.entity;

import com.memory.context.engine.domain.relationship.entity.RelationshipType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "memory_relationships",
        indexes = {
                @Index(name = "idx_from_memory", columnList = "fromMemoryId"),
                @Index(name = "idx_to_memory", columnList = "toMemoryId")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_memory_id", nullable = false)
    private Memory fromMemory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_memory_id", nullable = false)
    private Memory toMemory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType type;

    @Column(nullable = false)
    private int weight;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
