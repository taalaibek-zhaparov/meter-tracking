package com.metertracking.dto;

public record MeterSpecDTO(
        Long id,
        String amp,
        Short digits,
        Short phase,
        String voltage
) {}
