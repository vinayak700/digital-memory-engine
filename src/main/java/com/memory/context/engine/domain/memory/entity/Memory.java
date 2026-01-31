package com.memory.context.engine.domain.memory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "memories", indexes = {
        @Index(name = "idx_memory_user", columnList = "user_id"),
        @Index(name = "idx_memory_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Memory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    public String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> context;

    @Column(nullable = false)
    private int importanceScore;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "memory_topics", joinColumns = @JoinColumn(name = "memory_id"), inverseJoinColumns = @JoinColumn(name = "topic_id"))
    @Builder.Default
    @ToString.Exclude
    private Set<com.memory.context.engine.domain.topic.entity.Topic> topics = new java.util.HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
