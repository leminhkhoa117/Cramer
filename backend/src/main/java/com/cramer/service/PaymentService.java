package com.cramer.service;

import com.cramer.dto.PayOSWebhookDTO;
import com.cramer.dto.PaymentOrderDTO;
import com.cramer.dto.PaymentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for PayOS payment operations.
 * Handles subscription payments and Lúa pack purchases.
 */
public interface PaymentService {

    /**
     * Create a payment for subscription upgrade.
     * 
     * @param userId The user making the payment
     * @param tierId The subscription tier ID to upgrade to
     * @return PaymentResponseDTO with checkout URL and order details
     */
    PaymentResponseDTO createSubscriptionPayment(UUID userId, Integer tierId);

    /**
     * Create a payment for subscription upgrade by tier code.
     * 
     * @param userId The user making the payment
     * @param tierCode The subscription tier code (e.g., "cramerich")
     * @return PaymentResponseDTO with checkout URL and order details
     */
    PaymentResponseDTO createSubscriptionPaymentByCode(UUID userId, String tierCode);

    /**
     * Create a payment for Lúa pack purchase.
     * 
     * @param userId The user making the payment
     * @param luaAmount Amount of Lúa to purchase
     * @param priceVnd Price in VND
     * @return PaymentResponseDTO with checkout URL and order details
     */
    PaymentResponseDTO createLuaPackPayment(UUID userId, Integer luaAmount, Integer priceVnd);

    /**
     * Handle webhook notification from PayOS.
     * Verifies signature and processes the payment.
     * 
     * @param webhook The webhook payload from PayOS
     */
    void handleWebhook(PayOSWebhookDTO webhook);

    /**
     * Generate HMAC-SHA256 signature for PayOS API.
     * 
     * @param data Data to sign (will be sorted alphabetically)
     * @return The HMAC-SHA256 signature as hex string
     */
    String generateSignature(Map<String, Object> data);

    /**
     * Verify webhook signature from PayOS.
     * 
     * @param webhook The webhook payload to verify
     * @return true if signature is valid
     */
    boolean verifyWebhookSignature(PayOSWebhookDTO webhook);

    /**
     * Get payment order by PayOS order code.
     * 
     * @param orderCode The PayOS order code
     * @return PaymentOrderDTO with order details
     */
    PaymentOrderDTO getOrderByCode(Long orderCode);

    /**
     * Get user's payment history.
     * 
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of PaymentOrderDTO
     */
    Page<PaymentOrderDTO> getPaymentHistory(UUID userId, Pageable pageable);

    /**
     * Check if PayOS is properly configured.
     * 
     * @return true if all required credentials are set
     */
    boolean isPayOSConfigured();
}
