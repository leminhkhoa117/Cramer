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
public class FluencyCoherenceV2DTO {

    private BigDecimal band;
    private String feedback;
    private HesitationsV2DTO hesitations;
    private List<RepetitionV2DTO> repetitions;
    private SelfCorrectionsV2DTO selfCorrections;
    private TopicDevelopmentV2DTO topicDevelopment;
}
