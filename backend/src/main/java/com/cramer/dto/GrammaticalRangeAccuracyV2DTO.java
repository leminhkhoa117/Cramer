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
public class GrammaticalRangeAccuracyV2DTO {

    private BigDecimal band;
    private String feedback;
    private List<InaccurateStructureV2DTO> inaccurateStructures;
    private List<ProblematicSentenceV2DTO> problematicSentences;
}
