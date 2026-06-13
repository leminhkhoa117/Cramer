package com.cramer.billing.web;

import com.cramer.billing.service.LuaPackService;
import com.cramer.billing.service.PaymentService;
import com.cramer.billing.web.dto.BillingRequests;
import com.cramer.billing.web.dto.CreateOrderResponse;
import com.cramer.billing.web.dto.LuaPackView;
import com.cramer.billing.web.dto.PaymentOrderView;
import com.cramer.platform.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Payment endpoints (SPEC-15 §8, §9). {@code /webhook}, {@code /lua-packs} and
 * {@code /config-status} are public (SPEC-04 §1.1); order creation and history are authenticated.
 * The webhook always returns {@code 200 {code:"00"}} so PayOS stops retrying.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService payments;
    private final LuaPackService luaPacks;
    private final CurrentUser currentUser;

    public PaymentController(PaymentService payments, LuaPackService luaPacks, CurrentUser currentUser) {
        this.payments = payments;
        this.luaPacks = luaPacks;
        this.currentUser = currentUser;
    }

    @PostMapping("/subscription")
    public CreateOrderResponse createSubscription(@RequestBody BillingRequests.CreateSubscriptionOrder request) {
        return payments.createSubscriptionOrder(currentUser.requireUserId(), request.tierId(), request.tierCode());
    }

    @PostMapping("/lua")
    public CreateOrderResponse createLua(@RequestBody BillingRequests.CreateLuaOrder request) {
        return payments.createLuaOrder(currentUser.requireUserId(), request.packCode());
    }

    /** PayOS webhook (public). Success grants the order exactly once (idempotent). */
    @PostMapping("/webhook")
    public Map<String, String> webhook(@RequestBody JsonNode body) {
        try {
            String code = body.path("code").asText("");
            boolean success = "00".equals(code) && body.path("success").asBoolean(false);
            long orderCode = body.path("data").path("orderCode").asLong(body.path("orderCode").asLong(0));
            if (success && orderCode > 0) {
                payments.grantOnSuccess(orderCode);
            } else {
                log.info("Webhook ignored (code={}, success={}, order={})", code, success, orderCode);
            }
        } catch (RuntimeException e) {
            log.warn("Webhook processing error: {}", e.getMessage());
        }
        return Map.of("code", "00", "desc", "success");
    }

    @GetMapping("/status/{orderCode}")
    public PaymentOrderView status(@PathVariable long orderCode) {
        return payments.getStatus(orderCode, currentUser.requireUserId());
    }

    @GetMapping("/history")
    public List<PaymentOrderView> history(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return payments.history(currentUser.requireUserId(), PageRequest.of(page, Math.min(size, 100))).getContent();
    }

    @GetMapping("/lua-packs")
    public List<LuaPackView> luaPacks() {
        return luaPacks.listActive();
    }

    @GetMapping("/config-status")
    public Map<String, Object> configStatus() {
        return Map.of("configured", payments.payosConfigured());
    }
}
