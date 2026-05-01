package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfCorrectionsByCategoryV2DTO {

    private Integer verbTense;
    private Integer pronunciation;
    private Integer grammar;
}
