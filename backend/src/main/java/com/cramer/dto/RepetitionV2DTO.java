package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepetitionV2DTO {

    private String idea;
    private Integer count;
    private Boolean verbatim;
}
