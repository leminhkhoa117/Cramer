package com.cramer.service.implement;

import com.cramer.config.PayOSConfig;
import com.cramer.dto.PayOSWebhookDTO;
import com.cramer.dto.PaymentOrderDTO;
import com.cramer.dto.PaymentResponseDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.PaymentOrder;
import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserSubscription;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.PaymentOrderRepository;
import com.cramer.repository.SubscriptionTierRepository;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.CreditService;
import com.cramer.service.PaymentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of PaymentService for PayOS payment gateway integration.
 * Handles subscription payments and Lúa pack purchases via PayOS.
 * 
 * @since 2025-12-13
 */
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    // Lúa pack definitions (amount -> price in VND) - unified with frontend
    private static final Map<Integer, Integer> LUA_PACKS = Map.of(
            100, 10000, // Túi Lúa
            500, 45000, // Bao Lúa
            2000, 150000 // Xe Lúa
    );

    private final PayOSConfig payOSConfig;
    private final PaymentOrderRepository paymentOrderRepository;
    private final SubscriptionTierRepository tierRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final CreditService creditService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentServiceImpl(
            PayOSConfig payOSConfig,
            PaymentOrderRepository paymentOrderRepository,
            SubscriptionTierRepository tierRepository,
            UserSubscriptionRepository subscriptionRepository,
            CreditService creditService,
            ObjectMapper objectMapper) {
        this.payOSConfig = payOSConfig;
        this.paymentOrderRepository = paymentOrderRepository;
        this.tierRepository = tierRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.creditService = creditService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public PaymentResponseDTO createSubscriptionPayment(UUID userId, Integer tierId) {
        logger.info("💳 Creating subscription payment for user {} to tier {}", userId, tierId);

        SubscriptionTier tier = tierRepository.findById(tierId.longValue())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", "id", tierId));

        return createPaymentForTier(userId, tier);
    }

    @Override
    public PaymentResponseDTO createSubscriptionPaymentByCode(UUID userId, String tierCode) {
        logger.info("💳 Creating subscription payment for user {} to tier {}", userId, tierCode);

        SubscriptionTier tier = tierRepository.findByCode(tierCode)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", "code", tierCode));

        return createPaymentForTier(userId, tier);
    }

    @SuppressWarnings("null")
    private PaymentResponseDTO createPaymentForTier(UUID userId, SubscriptionTier tier) {
        if (tier.getPriceVnd() == null || tier.getPriceVnd() == 0) {
            throw new IllegalArgumentException("Cannot create payment for free tier");
        }

        // Generate unique order code using timestamp + random
        long orderCode = generateUniqueOrderCode();

        // Create description (max 25 chars for bank compatibility)
        String description = truncateDescription("CRAMER " + tier.getCode().toUpperCase());

        // Create payment order in database
        PaymentOrder order = PaymentOrder.builder()
                .userId(userId)
                .orderCode(orderCode)
                .type(PaymentOrder.Type.SUBSCRIPTION)
                .tierId(tier.getId())
                .tierCode(tier.getCode())
                .amountVnd(tier.getPriceVnd())
                .description(description)
                .status(PaymentOrder.Status.PENDING)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build();

        order = Objects.requireNonNull(paymentOrderRepository.save(order));
        logger.info("📝 Created payment order {} for tier {}", orderCode, tier.getCode());

        // Call PayOS API to create payment link
        return callPayOSCreatePayment(order, tier.getName());
    }

    @Override
    @SuppressWarnings("null")
    public PaymentResponseDTO createLuaPackPayment(UUID userId, Integer luaAmount, Integer priceVnd) {
        logger.info("💳 Creating Lúa pack payment for user {}: {} Lúa @ {}đ", userId, luaAmount, priceVnd);

        // Validate the Lúa pack
        if (!LUA_PACKS.containsKey(luaAmount) || !LUA_PACKS.get(luaAmount).equals(priceVnd)) {
            throw new IllegalArgumentException("Invalid Lúa pack: " + luaAmount + " @ " + priceVnd + "đ");
        }

        // Generate unique order code
        long orderCode = generateUniqueOrderCode();

        // Create description
        String description = truncateDescription("CRAMER " + luaAmount + " LUA");

        // Create payment order
        PaymentOrder order = PaymentOrder.builder()
                .userId(userId)
                .orderCode(orderCode)
                .type(PaymentOrder.Type.LUA_PACK)
                .luaAmount(luaAmount)
                .amountVnd(priceVnd)
                .description(description)
                .status(PaymentOrder.Status.PENDING)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build();

        order = Objects.requireNonNull(paymentOrderRepository.save(order));
        logger.info("📝 Created payment order {} for {} Lúa", orderCode, luaAmount);

        // Get pack name
        String packName = getLuaPackName(luaAmount);
        return callPayOSCreatePayment(order, packName);
    }

    private PaymentResponseDTO callPayOSCreatePayment(PaymentOrder order, String itemName) {
        if (!isPayOSConfigured()) {
            logger.warn("⚠️ PayOS not configured, returning mock response");
            return createMockResponse(order);
        }

        try {
            // Build request body
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("orderCode", order.getOrderCode());
            requestBody.put("amount", order.getAmountVnd());
            requestBody.put("description", order.getDescription());
            requestBody.put("cancelUrl", payOSConfig.getCancelUrl());
            requestBody.put("returnUrl", payOSConfig.getReturnUrl());

            // Add items
            List<Map<String, Object>> items = List.of(
                    Map.of(
                            "name", itemName,
                            "quantity", 1,
                            "price", order.getAmountVnd()));
            requestBody.put("items", items);

            // Generate signature
            String signature = generateSignature(Map.of(
                    "amount", order.getAmountVnd(),
                    "cancelUrl", payOSConfig.getCancelUrl(),
                    "description", order.getDescription(),
                    "orderCode", order.getOrderCode(),
                    "returnUrl", payOSConfig.getReturnUrl()));
            requestBody.put("signature", signature);

            // Expiration (24 hours from now)
            requestBody.put("expiredAt", order.getExpiresAt().toEpochSecond());

            logger.info("📤 Calling PayOS API: POST /v2/payment-requests");

            // Build HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payOSConfig.getClientId());
            headers.set("x-api-key", payOSConfig.getApiKey());

            // Create HTTP entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Call PayOS API
            String apiUrl = payOSConfig.getBaseUrl() + "/v2/payment-requests";
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    apiUrl,
                    Objects.requireNonNull(HttpMethod.POST),
                    entity,
                    String.class);

            if (responseEntity.getBody() == null) {
                throw new RuntimeException("Empty response from PayOS");
            }

            // Parse response
            Map<String, Object> response = objectMapper.readValue(
                    responseEntity.getBody(),
                    new TypeReference<Map<String, Object>>() {
                    });

            String code = (String) response.get("code");
            if (!"00".equals(code)) {
                String desc = (String) response.get("desc");
                logger.error("❌ PayOS API error: {} - {}", code, desc);
                throw new RuntimeException("PayOS error: " + desc);
            }

            // Parse response data
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String checkoutUrl = (String) data.get("checkoutUrl");
            String qrCode = (String) data.get("qrCode");
            String paymentLinkId = (String) data.get("paymentLinkId");

            // Update order with PayOS response
            order.setCheckoutUrl(checkoutUrl);
            order.setQrCode(qrCode);
            order.setPaymentLinkId(paymentLinkId);
            order = paymentOrderRepository.save(order);

            logger.info("✅ Payment link created: {}", checkoutUrl);

            return PaymentResponseDTO.fromEntity(order);

        } catch (Exception e) {
            logger.error("❌ Failed to create PayOS payment: {}", e.getMessage(), e);

            // Update order status to failed
            order.setStatus(PaymentOrder.Status.FAILED);
            paymentOrderRepository.save(order);

            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    private PaymentResponseDTO createMockResponse(PaymentOrder order) {
        // For development/testing without PayOS credentials
        String mockUrl = "https://pay.payos.vn/mock/" + order.getOrderCode();
        order.setCheckoutUrl(mockUrl);
        order.setPaymentLinkId("mock_" + order.getOrderCode());
        order = paymentOrderRepository.save(order);

        return PaymentResponseDTO.fromEntity(order);
    }

    @Override
    @Transactional
    public void handleWebhook(PayOSWebhookDTO webhook) {
        logger.info("🔔 Received PayOS webhook for order {}",
                webhook.getData() != null ? webhook.getData().getOrderCode() : "unknown");

        // Verify signature
        if (!verifyWebhookSignature(webhook)) {
            logger.error("❌ Invalid webhook signature");
            throw new SecurityException("Invalid webhook signature");
        }

        // Check if successful payment
        if (!webhook.isSuccess()) {
            logger.warn("⚠️ Webhook indicates non-success: code={}", webhook.getCode());
            return;
        }

        PayOSWebhookDTO.WebhookData data = webhook.getData();
        if (data == null || data.getOrderCode() == null) {
            logger.error("❌ Missing order code in webhook");
            return;
        }

        // Find the order
        PaymentOrder order = paymentOrderRepository.findByOrderCode(data.getOrderCode())
                .orElse(null);

        if (order == null) {
            logger.error("❌ Order not found: {}", data.getOrderCode());
            return;
        }

        // Skip if already paid
        if (order.isPaid()) {
            logger.info("ℹ️ Order {} already processed, skipping", data.getOrderCode());
            return;
        }

        // Mark as paid
        order.markAsPaid(data.getTransactionDateTime());
        paymentOrderRepository.save(order);
        logger.info("✅ Order {} marked as PAID", data.getOrderCode());

        // Process the payment based on type
        switch (order.getType()) {
            case SUBSCRIPTION -> processSubscriptionPayment(order);
            case LUA_PACK -> processLuaPackPayment(order);
        }
    }

    private void processSubscriptionPayment(PaymentOrder order) {
        logger.info("🔄 Processing subscription payment for user {} to tier {}",
                order.getUserId(), order.getTierCode());

        SubscriptionTier tier = tierRepository.findById(Objects.requireNonNull(order.getTierId()))
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", "id", order.getTierId()));

        // Get or create subscription
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(order.getUserId())
                .orElse(null);

        if (subscription == null) {
            // Create new subscription
            subscription = UserSubscription.builder()
                    .userId(order.getUserId())
                    .tier(tier)
                    .status(UserSubscription.Status.ACTIVE)
                    .aiGradingsUsed(0)
                    .paymentReference(order.getPaymentLinkId())
                    .autoRenew(false)
                    .build();
        } else {
            // Upgrade existing subscription
            subscription.setTier(tier);
            subscription.setPaymentReference(order.getPaymentLinkId());
            // Reset gradings used for new tier
            subscription.setAiGradingsUsed(0);
        }

        // Set expiration (1 month from now)
        subscription.setExpiresAt(OffsetDateTime.now().plusMonths(1));
        subscriptionRepository.save(subscription);

        // Add tier bonus Lúa if applicable
        if (tier.getInitialLua() != null && tier.getInitialLua() > 0) {
            creditService.earnCredits(
                    order.getUserId(),
                    tier.getInitialLua(),
                    CreditTransaction.Category.TIER_BONUS,
                    "Thưởng nâng cấp gói " + tier.getName(),
                    order.getPaymentLinkId());
        }

        logger.info("✅ Subscription upgraded to {} for user {}", tier.getCode(), order.getUserId());
    }

    private void processLuaPackPayment(PaymentOrder order) {
        logger.info("🔄 Processing Lúa pack payment for user {}: {} Lúa",
                order.getUserId(), order.getLuaAmount());

        creditService.earnCredits(
                order.getUserId(),
                order.getLuaAmount(),
                CreditTransaction.Category.PURCHASE,
                "Mua " + getLuaPackName(order.getLuaAmount()),
                order.getPaymentLinkId());

        logger.info("✅ Added {} Lúa to user {}", order.getLuaAmount(), order.getUserId());
    }

    @Override
    public String generateSignature(Map<String, Object> data) {
        // Sort keys alphabetically and build data string
        String dataString = data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        try {
            Mac hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    payOSConfig.getChecksumKey().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256);
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(dataString.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            logger.error("❌ Failed to generate signature: {}", e.getMessage());
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(PayOSWebhookDTO webhook) {
        if (webhook.getSignature() == null || webhook.getData() == null) {
            return false;
        }

        if (!isPayOSConfigured()) {
            // Skip verification in mock mode
            logger.warn("⚠️ PayOS not configured, skipping signature verification");
            return true;
        }

        try {
            PayOSWebhookDTO.WebhookData data = webhook.getData();

            // Build data map with all webhook data fields (sorted alphabetically)
            Map<String, Object> signData = new TreeMap<>();
            if (data.getAmount() != null)
                signData.put("amount", data.getAmount());
            if (webhook.getCode() != null)
                signData.put("code", webhook.getCode());
            if (data.getDescription() != null)
                signData.put("description", data.getDescription());
            if (data.getOrderCode() != null)
                signData.put("orderCode", data.getOrderCode());
            if (data.getPaymentLinkId() != null)
                signData.put("paymentLinkId", data.getPaymentLinkId());

            // Add other fields that PayOS includes
            if (data.getAccountNumber() != null)
                signData.put("accountNumber", data.getAccountNumber());
            if (data.getReference() != null)
                signData.put("reference", data.getReference());
            if (data.getTransactionDateTime() != null)
                signData.put("transactionDateTime", data.getTransactionDateTime());

            String expectedSignature = generateSignature(signData);
            return webhook.getSignature().equals(expectedSignature);

        } catch (Exception e) {
            logger.error("❌ Failed to verify webhook signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOrderDTO getOrderByCode(Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "orderCode", orderCode));
        return PaymentOrderDTO.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentOrderDTO> getPaymentHistory(UUID userId, Pageable pageable) {
        logger.info("📜 Fetching payment history for user: {}", userId);
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PaymentOrderDTO::fromEntity);
    }

    @Override
    public boolean isPayOSConfigured() {
        return payOSConfig.isConfigured();
    }

    /**
     * Generate a unique order code.
     * Uses timestamp + random component to ensure uniqueness.
     */
    private long generateUniqueOrderCode() {
        long code;
        do {
            // Use timestamp (last 9 digits) + random 4 digits = 13 digits max
            long timestamp = System.currentTimeMillis() % 1_000_000_000L;
            int random = new Random().nextInt(10000);
            code = timestamp * 10000L + random;
        } while (paymentOrderRepository.existsByOrderCode(code));
        return code;
    }

    /**
     * Truncate description to 25 chars (PayOS bank compatibility).
     */
    private String truncateDescription(String description) {
        if (description.length() <= 25) {
            return description;
        }
        return description.substring(0, 25);
    }

    /**
     * Get display name for Lúa pack.
     */
    private String getLuaPackName(int amount) {
        return switch (amount) {
            case 100 -> "Túi Lúa (100)";
            case 500 -> "Bao Lúa (500)";
            case 2000 -> "Xe Lúa (2000)";
            default -> amount + " Lúa";
        };
    }
}
