package com.cramer.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingGradingResultV2DTO {

    private String schemaVersion;
    private BigDecimal overallBand;
    private String gradingMode;
    private String degradedReason;
    private CriteriaV2DTO criteria;
    private List<PerPartFeedbackV2DTO> perPartFeedback;
    private List<PerTurnFeedbackV2DTO> perTurnFeedback;
    private List<String> improvementTips;
}
