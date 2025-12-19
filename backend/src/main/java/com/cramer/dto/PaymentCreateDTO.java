package com.cramer.dto;

import com.cramer.entity.PaymentOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a payment request.
 * Supports both subscription payments and Lúa pack purchases.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new payment")
public class PaymentCreateDTO {

    @Schema(description = "Payment type", example = "SUBSCRIPTION", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentOrder.Type type;

    @Schema(description = "Subscription tier ID (required for SUBSCRIPTION type)", example = "2")
    private Integer tierId;

    @Schema(description = "Subscription tier code (alternative to tierId)", example = "cramerich")
    private String tierCode;

    @Schema(description = "Lúa amount to purchase (required for LUA_PACK type)", example = "100")
    private Integer luaAmount;

    @Schema(description = "Price in VND (required for LUA_PACK type)", example = "20000")
    private Integer priceVnd;

    /**
     * Validate the payment request based on type.
     */
    public boolean isValid() {
        if (type == null) {
            return false;
        }

        return switch (type) {
            case SUBSCRIPTION -> tierId != null || (tierCode != null && !tierCode.isEmpty());
            case LUA_PACK -> luaAmount != null && luaAmount > 0 && priceVnd != null && priceVnd > 0;
        };
    }
}
