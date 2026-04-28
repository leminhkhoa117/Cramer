package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeakOrImprovableV2DTO {

    private String phrase;
    private String alternativeBetter;
}
