package com.cramer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TargetDTO(
    String examName,
    LocalDate examDate,
    Double listening,
    Double reading,
    Double writing,
    Double speaking
) {}