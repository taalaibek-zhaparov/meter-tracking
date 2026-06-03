package com.metertracking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record MeterImportItem(
        @NotNull
        @JsonProperty("Код")
        String code,

        @NotNull
        @JsonProperty("Наименование")
        String name,

        @JsonProperty("Ампер")
        String amp,

        @JsonProperty("Значность")
        Short digits,

        @JsonProperty("Фазность")
        Short phase,

        @JsonProperty("Напряжение")
        String voltage
){}
