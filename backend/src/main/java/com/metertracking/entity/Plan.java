package com.metertracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "plan_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(name = "new_nomer_schetchika")
    private String newNomerSchetchika;

    @Column(name = "new_pokazaniya")
    private Double newPokazaniya;

    // ✅ НОВЫЕ ПОЛЯ из 1С HTTP публикации
    @Column(name = "fio")
    private String fio;     // ФИО абонента

    @Column(name = "adres")
    private String adres;   // Адрес абонента

    @Column(name = "document_type")
    private String documentType; // По акту, По заявлению, Плановая
}