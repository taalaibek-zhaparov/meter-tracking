package com.metertracking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель счётчика из справочника 1С.
 * Одна модель → много вариантов исполнения (MeterSpec).
 */
@Entity
@Table(
        name = "meter_model",
        uniqueConstraints = @UniqueConstraint(name = "uq_meter_model_code", columnNames = "code")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальный код из 1С */
    @Column(nullable = false, length = 100)
    private String code;

    /** Наименование модели (нормализованное) */
    @Column(nullable = false, length = 500)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "meterModel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeterSpec> specs = new ArrayList<>();
}
