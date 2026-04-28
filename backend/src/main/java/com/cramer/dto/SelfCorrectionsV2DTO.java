package com.cramer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfCorrectionsV2DTO {

    private Integer total;
    private SelfCorrectionsByCategoryV2DTO byCategory;
    private List<SelfCorrectionExampleV2DTO> examples;
}
