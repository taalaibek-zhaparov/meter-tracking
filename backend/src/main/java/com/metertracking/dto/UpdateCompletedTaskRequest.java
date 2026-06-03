package com.metertracking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO для обновления выполненной задачи.
 * Поля region, tp, licevoy, tip, nomerSchetchika, pokazaniya, data
 * приходят из фронтенда но на бэкенде игнорируются (readonly в UI).
 * Редактируемые поля: nomerPlomby, nomerSimKarty, nomerIccid,
 * naKryshke, naYashike, plombaGos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompletedTaskRequest {
    private Long completedTaskId;
    private String region;
    private String tp;
    private String licevoy;
    private String tip;
    private String nomerSchetchika;
    private Double pokazaniya;
    private LocalDate data;
    private String nomerPlomby;
    private String nomerSimKarty;
    private String nomerIccid;

    // ✅ ДОБАВЛЕНО: новые поля которых не хватало
    private String naKryshke;
    private String naYashike;
    private String plombaGos;
}