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

    private Long memoryId;

    @Column(columnDefinition = "TEXT")
    private String outcomeSummary;

    private int satisfactionScore; // -5 to +5

    private Instant recordedAt;
}
