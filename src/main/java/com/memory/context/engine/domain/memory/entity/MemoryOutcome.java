package com.memory.context.engine.domain.memory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "memory_outcomes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memory_id", nullable = false, foreignKey = @ForeignKey(name = "fk_memory_outcome"))
    private Memory memory;

    @Column(columnDefinition = "TEXT")
    private String outcomeSummary;

    @Column(nullable = false)
    private int satisfactionScore; // -5 to +5

    @Column
    private Instant recordedAt;
}
