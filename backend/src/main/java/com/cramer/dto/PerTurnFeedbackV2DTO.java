package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerTurnFeedbackV2DTO {

    private Integer turnIndex;
    private Integer partNumber;
    private String shortNote;
    private String sampleAnswer;
}
