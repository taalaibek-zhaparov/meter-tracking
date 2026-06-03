package com.metertracking.dto;

import java.util.List;

public record MeterImportRequest(
        List<MeterImportItem> items
) {}
