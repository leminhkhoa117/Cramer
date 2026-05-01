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
public class PronunciationV2DTO {

    private BigDecimal band;
    private String feedback;
    private String confidence;
    private List<InaccurateStressV2DTO> inaccurateStresses;
    private List<InaccurateIntonationV2DTO> inaccurateIntonations;
    private List<InaccuratePronunciationV2DTO> inaccuratePronunciations;
    private String connectedSpeech;
    private IntelligibilityV2DTO intelligibility;
}
