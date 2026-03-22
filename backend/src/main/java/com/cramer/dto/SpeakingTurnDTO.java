package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingTurnDTO {

    private Integer turnIndex;
    private Integer partNumber;
    private Long sourceQuestionId;
    private JsonNode questionSnapshot;
}
