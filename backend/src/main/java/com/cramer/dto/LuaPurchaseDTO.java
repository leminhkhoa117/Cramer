package com.cramer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for initiating a Lúa package purchase.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to purchase a Lúa package")
public class LuaPurchaseDTO {

    @NotBlank(message = "Package code is required")
    @Schema(description = "Package code: 'small', 'medium', or 'large'", example = "medium")
    private String packageCode;
}
