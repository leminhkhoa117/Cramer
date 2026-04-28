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
public class LexicalResourceV2DTO {

    private BigDecimal band;
    private String feedback;
    private List<String> decentlyUsed;
    private List<WeakOrImprovableV2DTO> weakOrImprovable;
    private List<InaccurateV2DTO> inaccurate;
    private List<IdiomaticV2DTO> idiomatic;
}
