package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InaccurateV2DTO {

    private String phrase;
    private String issue;
    private String correction;
}
