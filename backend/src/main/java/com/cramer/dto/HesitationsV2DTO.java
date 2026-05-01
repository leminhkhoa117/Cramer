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
public class HesitationsV2DTO {

    private Integer totalCount;
    private List<HesitationPerPartV2DTO> perPart;
    private List<String> locationCategories;
}
