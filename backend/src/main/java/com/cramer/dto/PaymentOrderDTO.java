package com.cramer.dto;

import com.cramer.entity.PaymentOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for payment order details.
 * Used for tracking and history purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payment order details")
public class PaymentOrderDTO {

    @Schema(description = "Internal order ID")
    private Long id;

    @Schema(description = "User ID who made the payment")
    private UUID userId;

    @Schema(description = "PayOS unique order code", example = "1702548000123")
    private Long orderCode;

    @Schema(description = "PayOS payment link ID")
    private String paymentLinkId;

    @Schema(description = "Payment checkout URL")
    private String checkoutUrl;

    @Schema(description = "Payment type")
    private PaymentOrder.Type type;

    @Schema(description = "Subscription tier ID (for SUBSCRIPTION type)")
    private Long tierId;

    @Schema(description = "Subscription tier code", example = "cramerich")
    private String tierCode;

    @Schema(description = "Lúa amount (for LUA_PACK type)", example = "100")
    private Integer luaAmount;

    @Schema(description = "Payment amount in VND", example = "79000")
    private Integer amountVnd;

    @Schema(description = "Short description", example = "CRAMER CRAMERICH")
    private String description;

    @Schema(description = "Current payment status")
    private PaymentOrder.Status status;

    @Schema(description = "PayOS transaction datetime when paid")
    private String transactionDatetime;

    @Schema(description = "Order creation timestamp")
    private OffsetDateTime createdAt;

    @Schema(description = "Payment completion timestamp")
    private OffsetDateTime paidAt;

    @Schema(description = "Payment expiration timestamp")
    private OffsetDateTime expiresAt;

    /**
     * Convert from PaymentOrder entity.
     */
    public static PaymentOrderDTO fromEntity(PaymentOrder order) {
        return PaymentOrderDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderCode(order.getOrderCode())
                .paymentLinkId(order.getPaymentLinkId())
                .checkoutUrl(order.getCheckoutUrl())
                .type(order.getType())
                .tierId(order.getTierId())
                .tierCode(order.getTierCode())
                .luaAmount(order.getLuaAmount())
                .amountVnd(order.getAmountVnd())
                .description(order.getDescription())
                .status(order.getStatus())
                .transactionDatetime(order.getTransactionDatetime())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .expiresAt(order.getExpiresAt())
                .build();
    }
}
