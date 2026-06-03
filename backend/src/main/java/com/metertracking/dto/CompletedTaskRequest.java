package com.metertracking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletedTaskRequest {
    private Long planId;
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

    private String oldNomerSchetchika;
    private Double oldPokazaniya;
    private Double newPokazaniya;

    private String plombaGos;
    private String naKryshke;
    private String naYashike;

    private Integer newPhases;
    private Integer newAmperage;

    private String fio;
    private String adres;
    private Integer znch;

    /**
     * Полный формат тока из справочника 1С: "5(80)А", "100А".
     * Передаётся фронтом при выборе счётчика из справочника.
     * Используется в поле "Amper" при отправке в 1С.
     */
    private String ampSpec;

    /**
     * Числовой код модели в 1С (поле MeterCode).
     * Берётся из справочника при выборе счётчика.
     */
    private Integer meterCode;

    /**
     * Долг абонента из 1С (поле Summa).
     * Подтягивается при запросе данных лицевого счёта из 1С.
     */
    private Double summa;

    private String signatureAbonent;
    private String signatureMaster;
    private String documentType;
}
