package com.cramer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Lúa package purchase response.
 * Contains package details and PayOS checkout URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response after initiating a Lúa package purchase")
public class LuaPurchaseResponseDTO {

    @Schema(description = "Package code", example = "medium")
    private String packageCode;

    @Schema(description = "Package display name", example = "Gói Lúa")
    private String packageName;

    @Schema(description = "Base Lúa amount", example = "500")
    private Integer luaAmount;

    @Schema(description = "Bonus Lúa amount", example = "50")
    private Integer bonusAmount;

    @Schema(description = "Total Lúa (base + bonus)", example = "550")
    private Integer totalLua;

    @Schema(description = "Price in VND", example = "45000")
    private Integer priceVnd;

    @Schema(description = "Bonus percentage", example = "10")
    private Integer bonusPercent;

    @Schema(description = "PayOS checkout URL for payment")
    private String checkoutUrl;

    @Schema(description = "PayOS order code for tracking")
    private Long orderCode;

    @Schema(description = "Status message")
    private String message;

    @Schema(description = "Whether the request was successful")
    private Boolean success;
}
