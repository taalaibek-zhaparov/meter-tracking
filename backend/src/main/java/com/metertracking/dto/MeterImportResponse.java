package com.metertracking.dto;

public record MeterImportResponse(
        int totalReceived,
        int modelsCreated,
        int modelsSkipped,
        int specsCreated,
        int specsSkipped,
        int specsInvalid
) {}
