package com.cramer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for PayOS webhook payload.
 * This is sent by PayOS when a payment status changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "PayOS webhook payload")
public class PayOSWebhookDTO {

    @Schema(description = "Response code from PayOS", example = "00")
    private String code;

    @Schema(description = "Description/message", example = "Success")
    private String desc;

    @Schema(description = "Success flag")
    private Boolean success;

    @Schema(description = "Payment data")
    private WebhookData data;

    @Schema(description = "HMAC-SHA256 signature for verification")
    private String signature;

    /**
     * Nested data object containing payment details.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookData {

        @Schema(description = "Order code from original request", example = "123456789")
        private Long orderCode;

        @Schema(description = "Payment amount in VND", example = "79000")
        private Integer amount;

        @Schema(description = "Payment description")
        private String description;

        @Schema(description = "Account number (masked)")
        private String accountNumber;

        @Schema(description = "PayOS payment link ID")
        private String paymentLinkId;

        @Schema(description = "Reference number")
        private String reference;

        @Schema(description = "Transaction reference")
        private String transactionReference;

        @Schema(description = "Payment datetime", example = "2023-02-04 18:25:00")
        private String transactionDateTime;

        @Schema(description = "Payer's account number")
        private String counterAccountBankId;

        @Schema(description = "Payer's bank name")
        private String counterAccountBankName;

        @Schema(description = "Payer's account name")
        private String counterAccountName;

        @Schema(description = "Payer's account number")
        private String counterAccountNumber;

        @Schema(description = "Virtual account number")
        private String virtualAccountNumber;

        @Schema(description = "Virtual account name")
        private String virtualAccountName;

        @JsonProperty("code")
        @Schema(description = "Internal code in data object")
        private String dataCode;
    }

    /**
     * Check if this webhook indicates a successful payment.
     */
    public boolean isSuccess() {
        return "00".equals(code) && Boolean.TRUE.equals(success);
    }
}
