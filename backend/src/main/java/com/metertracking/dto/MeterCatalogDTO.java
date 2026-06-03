package com.metertracking.dto;

import lombok.Data;

import java.util.List;

/**
 * Контейнер DTO для справочника счётчиков.
 * Используется только для группировки типов (не обязателен, но допустим в архитектуре).
 */
public class MeterCatalogDTO {

    // ─────────────────────────────────────────────
    // MODEL DTO
    // ─────────────────────────────────────────────

    public record MeterModelDTO(
            Long id,
            String code,
            String name,
            List<MeterSpecDTO> specs
    ) {}

    public record MeterSpecDTO(
            Long id,
            String amp,
            Short digits,
            Short phase,
            String voltage
    ) {}

    // ─────────────────────────────────────────────
    // DEVICE DTO (используется в linkDeviceToModel)
    // ─────────────────────────────────────────────

    @Data
    public static class MeterDeviceDTO {
        private Long id;
        private String meterNumber;
        private String meterType;
        private String simCardNumber;
        private String iccidNumber;
        private String sealNumber;
        private Integer phases;
        private Integer amperage;
        private Integer znch;
        private boolean available;
        private String meterModelCode;
        private String meterModelName;
    }
}
