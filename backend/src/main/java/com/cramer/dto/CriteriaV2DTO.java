package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriteriaV2DTO {

    private FluencyCoherenceV2DTO fluencyCoherence;
    private LexicalResourceV2DTO lexicalResource;
    private GrammaticalRangeAccuracyV2DTO grammaticalRangeAccuracy;
    private PronunciationV2DTO pronunciation;
}
