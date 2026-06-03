package com.metertracking.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class OneCMeterReadingDTO {
    private String account;
    private String tp;
    private String region;
    private String meterNumber;
    private String meterType;
    private Double reading;
    private LocalDate date;

    // НОВЫЕ ПОЛЯ из HTTP публикации 1С
    private String fio;     // ФИО абонента
    private String adres;   // Адрес абонента

    // Конструктор для JPQL / Hibernate (если используется)
    public OneCMeterReadingDTO(String account, String tp, String region,
                               String meterNumber, String meterType,
                               Double reading, LocalDate date) {
        this.account = account;
        this.tp = tp;
        this.region = region;
        this.meterNumber = meterNumber;
        this.meterType = meterType;
        this.reading = reading;
        this.date = date;
    }

    // Конструктор без аргументов для HTTP сервиса
    public OneCMeterReadingDTO() {}
}