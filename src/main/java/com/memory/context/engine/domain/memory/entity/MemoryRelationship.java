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

    private Long fromMemoryId;
    private Long toMemoryId;

    @Enumerated(EnumType.STRING)
    private RelationshipType type;

    private int weight;

    private Instant createdAt;
}
