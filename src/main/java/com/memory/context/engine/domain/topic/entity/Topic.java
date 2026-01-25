package com.memory.context.engine.domain.topic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Topic entity for categorizing memories.
 * Enables memory organization and discovery.
 */
@Entity
@Table(name = "topics", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "name" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
