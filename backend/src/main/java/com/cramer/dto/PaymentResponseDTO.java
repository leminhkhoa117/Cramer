package com.cramer.dto;

import com.cramer.entity.PaymentOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for payment creation response.
 * Contains the checkout URL and payment details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response after creating a payment")
public class PaymentResponseDTO {

    @Schema(description = "Internal order ID")
    private Long id;

    @Schema(description = "PayOS unique order code", example = "1702548000123")
    private Long orderCode;

    @Schema(description = "PayOS payment link ID")
    private String paymentLinkId;

    @Schema(description = "URL for user to complete payment", 
            example = "https://pay.payos.vn/web/...")
    private String checkoutUrl;

    @Schema(description = "QR code data for payment (if available)")
    private String qrCode;

    @Schema(description = "Payment type")
    private PaymentOrder.Type type;

    @Schema(description = "Payment amount in VND", example = "79000")
    private Integer amountVnd;

    @Schema(description = "Short description", example = "CRAMER CRAMERICH")
    private String description;

    @Schema(description = "Current payment status")
    private PaymentOrder.Status status;

    @Schema(description = "Payment creation timestamp")
    private OffsetDateTime createdAt;

    @Schema(description = "Payment expiration timestamp")
    private OffsetDateTime expiresAt;

    /**
     * Convert from PaymentOrder entity.
     */
    public static PaymentResponseDTO fromEntity(PaymentOrder order) {
        return PaymentResponseDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .paymentLinkId(order.getPaymentLinkId())
                .checkoutUrl(order.getCheckoutUrl())
                .qrCode(order.getQrCode())
                .type(order.getType())
                .amountVnd(order.getAmountVnd())
                .description(order.getDescription())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .expiresAt(order.getExpiresAt())
                .build();
    }
}
