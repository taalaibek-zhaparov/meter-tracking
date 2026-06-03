package com.metertracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "completed_task_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletedTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = true)
    @JsonIgnoreProperties({"user", "hibernateLazyInitializer", "handler"})
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnoreProperties({"roles", "password", "deleted", "deletedAt", "hibernateLazyInitializer", "handler"})
    private User user;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String tp;

    @Column(nullable = false)
    private String licevoy;

    @Column(nullable = false)
    private String tip;

    @Column(name = "nomer_schetchika", nullable = false)
    private String nomerSchetchika;

    @Column(nullable = false)
    private Double pokazaniya;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "nomer_plomby", nullable = false)
    private String nomerPlomby;

    @Column(name = "nomer_sim_karty", nullable = false)
    private String nomerSimKarty;

    @Column(name = "nomer_iccid", nullable = false)
    private String nomerIccid;

    @Column(name = "old_nomer_schetchika")
    private String oldNomerSchetchika;

    @Column(name = "old_pokazaniya")
    private Double oldPokazaniya;

    @Column(name = "new_pokazaniya")
    private Double newPokazaniya = 0.0;

    @Column(name = "plomba_gos")
    private String plombaGos;

    @Column(name = "na_kryshke")
    private String naKryshke;

    @Column(name = "na_yashike")
    private String naYashike;

    /** Фазность нового счётчика: 1 или 3 */
    @Column(name = "phases")
    private Integer phases;

    /** Ампераж нового счётчика (число): 5, 10, 60 и т.д. */
    @Column(name = "amperage")
    private Integer amperage;

    /**
     * Полный формат тока из справочника 1С: "5(80)А", "100А".
     * Берётся из MeterSpec.amp при регистрации счётчика.
     * Именно это поле отправляется в 1С в поле "Amper".
     */
    @Column(name = "amp_spec", length = 50)
    private String ampSpec;

    /**
     * Внутренний числовой код модели счётчика в 1С (поле MeterCode).
     * Заполняется если код 1С — числовой.
     */
    @Column(name = "meter_code")
    private Integer meterCode = 0;

    /**
     * Потреблённые кВт за период (newPokazaniya - oldPokazaniya).
     * Рассчитывается автоматически при сохранении.
     */
    @Column(name = "kwt")
    private Double kwt = 0.0;

    /**
     * Сумма задолженности абонента (из 1С).
     */
    @Column(name = "summa")
    private Double summa = 0.0;

    @Column(name = "fio")
    private String fio;

    @Column(name = "adres")
    private String adres;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "document_number", unique = true)
    private String documentNumber; // формат: 204-000001

    @Column(name = "znch")
    private Integer znch;

    @Column(name = "signature_abonent", columnDefinition = "TEXT")
    private String signatureAbonent;

    @Column(name = "signature_master", columnDefinition = "TEXT")
    private String signatureMaster;

    @Column(name = "document_type")
    private String documentType;

    /** Тип старого счётчика (из плана, до замены) — для корректной отправки в 1С */
    @Column(name = "old_meter_type")
    private String oldMeterType;
}
