package com.metertracking.dto;

import java.util.List;

public record MeterModelDTO(
        Long id,
        String code,
        String name,
        List<MeterSpecDTO> specs
) {}