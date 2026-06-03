package com.metertracking.dto;

import lombok.Data;


@Data
public class MeterDeviceDTO {
    private Long id;
    private String meterNumber;
    private String meterType;
    private String simCardNumber;
    private String iccidNumber;
    private String sealNumber;
    private Integer phases;
    private Integer amperage;
    private Integer znch;
    private String voltage;
    private boolean available;

    /**
     * Код модели из справочника 1С — заполняется если счётчик
     * привязан к справочнику при регистрации заводского номера.
     */
    private String meterModelCode;

    /**
     * Наименование модели из справочника — для отображения на фронте.
     */
    private String meterModelName;
}