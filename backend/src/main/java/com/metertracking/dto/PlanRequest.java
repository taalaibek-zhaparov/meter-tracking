package com.metertracking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {
    private Long userId;
    private String region;
    private String tp;
    private String licevoy;
    private String tip;
    private String nomerSchetchika;
    private Double pokazaniya;
    private LocalDate data;

    // ✅ НОВЫЕ ПОЛЯ
    private String fio;     // ФИО абонента
    private String adres;   // Адрес абонента
    private String documentType;
}