package com.metertracking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Вариант исполнения модели счётчика (ток, фазность, значность, напряжение).
 */
@Entity
@Table(
        name = "meter_spec",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_meter_spec",
                columnNames = {"meter_model_id", "amp", "digits", "phase", "voltage"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_model_id", nullable = false)
    private MeterModel meterModel;

    /** Ток — нормализован: upper-case, только лат. "A", без пробелов. Пример: "5(80)A" */
    @Column(nullable = false, length = 50)
    private String amp;

    /** Значность (количество разрядов), например 7 */
    @Column(nullable = false)
    private Short digits;

    /** Фазность: 1 или 3 */
    @Column(nullable = false)
    private Short phase;

    /** Напряжение — нормализовано: без пробелов. Пример: "220В" */
    @Column(nullable = false, length = 50)
    private String voltage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}