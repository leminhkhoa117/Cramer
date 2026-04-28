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
public class HesitationPerPartV2DTO {

    private Integer partNumber;
    private Integer count;
    private List<Integer> questionsCausingPauses;
}
