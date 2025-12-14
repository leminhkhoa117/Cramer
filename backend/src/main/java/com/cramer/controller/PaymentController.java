package com.cramer.controller;

import com.cramer.dto.PayOSWebhookDTO;
import com.cramer.dto.PaymentCreateDTO;
import com.cramer.dto.PaymentOrderDTO;
import com.cramer.dto.PaymentResponseDTO;
import com.cramer.entity.PaymentOrder;
import com.cramer.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Payment operations via PayOS.
 * Handles subscription upgrades and Lúa credit purchases.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "APIs for payment processing via PayOS")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(
        summary = "Create subscription payment",
        description = "Create a payment link for subscription upgrade via PayOS"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment link created"),
        @ApiResponse(responseCode = "400", description = "Invalid tier or already on this tier"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/subscription")
    public ResponseEntity<PaymentResponseDTO> createSubscriptionPayment(
            Authentication authentication,
            @RequestBody PaymentCreateDTO request) {
        
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("💳 POST /api/payments/subscription - User: {}", userId);
        
        if (request.getType() != PaymentOrder.Type.SUBSCRIPTION) {
            return ResponseEntity.badRequest().build();
        }
        
        PaymentResponseDTO response;
        if (request.getTierId() != null) {
            response = paymentService.createSubscriptionPayment(userId, request.getTierId());
        } else if (request.getTierCode() != null) {
            response = paymentService.createSubscriptionPaymentByCode(userId, request.getTierCode());
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Create Lúa pack payment",
        description = "Create a payment link for Lúa credit purchase via PayOS"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment link created"),
        @ApiResponse(responseCode = "400", description = "Invalid Lúa pack"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/lua")
    public ResponseEntity<PaymentResponseDTO> createLuaPackPayment(
            Authentication authentication,
            @RequestBody PaymentCreateDTO request) {
        
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("💳 POST /api/payments/lua - User: {}, Amount: {}", userId, request.getLuaAmount());
        
        if (request.getType() != PaymentOrder.Type.LUA_PACK) {
            return ResponseEntity.badRequest().build();
        }
        
        if (request.getLuaAmount() == null || request.getPriceVnd() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        PaymentResponseDTO response = paymentService.createLuaPackPayment(
                userId, request.getLuaAmount(), request.getPriceVnd());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "PayOS Webhook",
        description = "Webhook endpoint for PayOS to notify payment status. This endpoint is public (no auth required)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed"),
        @ApiResponse(responseCode = "400", description = "Invalid webhook payload"),
        @ApiResponse(responseCode = "403", description = "Invalid signature")
    })
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody PayOSWebhookDTO webhook) {
        logger.info("🔔 POST /api/payments/webhook - Order: {}", 
                webhook.getData() != null ? webhook.getData().getOrderCode() : "unknown");
        
        try {
            paymentService.handleWebhook(webhook);
            
            // PayOS expects a success response
            return ResponseEntity.ok(Map.of(
                    "code", "00",
                    "message", "Success"
            ));
            
        } catch (SecurityException e) {
            logger.error("❌ Webhook security error: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of(
                    "code", "403",
                    "message", "Invalid signature"
            ));
            
        } catch (Exception e) {
            logger.error("❌ Webhook processing error: {}", e.getMessage());
            // Still return 200 to prevent PayOS from retrying
            return ResponseEntity.ok(Map.of(
                    "code", "99",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @Operation(
        summary = "Get payment status",
        description = "Check the status of a payment by order code"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment order found"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<PaymentOrderDTO> getPaymentStatus(
            Authentication authentication,
            @PathVariable Long orderCode) {
        
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("🔍 GET /api/payments/status/{} - User: {}", orderCode, userId);
        
        PaymentOrderDTO order = paymentService.getOrderByCode(orderCode);
        
        // Security check: only allow users to view their own orders
        if (!order.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(order);
    }

    @Operation(
        summary = "Get payment history",
        description = "Get the authenticated user's payment history"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment history returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<PaymentOrderDTO>> getPaymentHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("📜 GET /api/payments/history - User: {}, Page: {}", userId, page);
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<PaymentOrderDTO> history = paymentService.getPaymentHistory(userId, pageable);
        
        return ResponseEntity.ok(history);
    }

    @Operation(
        summary = "Get Lúa pack options",
        description = "Get all available Lúa pack purchase options"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lúa packs returned")
    })
    @GetMapping("/lua-packs")
    public ResponseEntity<Map<String, Object>> getLuaPacks() {
        logger.info("🌾 GET /api/payments/lua-packs");
        
        return ResponseEntity.ok(Map.of(
                "packs", java.util.List.of(
                        Map.of("amount", 50, "priceVnd", 10000, "name", "Túi Lúa", "discount", 0),
                        Map.of("amount", 100, "priceVnd", 20000, "name", "Gói Lúa", "discount", 0),
                        Map.of("amount", 300, "priceVnd", 50000, "name", "Bao Lúa", "discount", 17),
                        Map.of("amount", 500, "priceVnd", 80000, "name", "Bao Lúa lớn", "discount", 20),
                        Map.of("amount", 1000, "priceVnd", 160000, "name", "Xe Lúa", "discount", 20)
                )
        ));
    }

    @Operation(
        summary = "Check PayOS configuration",
        description = "Check if PayOS payment gateway is properly configured"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration status returned")
    })
    @GetMapping("/config-status")
    public ResponseEntity<Map<String, Object>> getConfigStatus() {
        boolean configured = paymentService.isPayOSConfigured();
        return ResponseEntity.ok(Map.of(
                "configured", configured,
                "message", configured ? "PayOS is configured" : "PayOS credentials not set"
        ));
    }
}
