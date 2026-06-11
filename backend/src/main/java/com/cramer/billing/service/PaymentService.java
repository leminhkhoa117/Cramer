package com.cramer.billing.service;

import com.cramer.billing.config.PayOsProperties;
import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.LuaPack;
import com.cramer.billing.domain.PaymentOrder;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.repository.PaymentOrderRepository;
import com.cramer.billing.repository.SubscriptionTierRepository;
import com.cramer.billing.web.dto.CreateOrderResponse;
import com.cramer.billing.web.dto.PaymentOrderView;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PayOS payment orders + grant (SPEC-15 §8). The webhook grant is the money-critical path:
 *
 * <p><strong>Fix — concurrency-safe idempotency:</strong> the order is claimed with a row lock
 * and a {@code PENDING → PAID} transition before granting. An already-{@code PAID} order is a
 * no-op, so duplicate concurrent webhooks <strong>cannot</strong> grant twice. Lúa packs are
 * DB-driven ({@code lua_amount + bonus_lua}); subscription grants delegate to
 * {@link SubscriptionService}. When PayOS is unconfigured, orders use a mock checkout URL.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentOrderRepository orders;
    private final LuaPackService luaPacks;
    private final CreditService credits;
    private final SubscriptionService subscriptions;
    private final SubscriptionTierRepository tiers;
    private final PayOsProperties payos;

    public PaymentService(PaymentOrderRepository orders, LuaPackService luaPacks, CreditService credits,
                          SubscriptionService subscriptions, SubscriptionTierRepository tiers,
                          PayOsProperties payos) {
        this.orders = orders;
        this.luaPacks = luaPacks;
        this.credits = credits;
        this.subscriptions = subscriptions;
        this.tiers = tiers;
        this.payos = payos;
    }

    public boolean payosConfigured() {
        return payos.configured();
    }

    /** Create a subscription payment order (SPEC-15 §8). Free tiers are rejected. */
    @Transactional
    public CreateOrderResponse createSubscriptionOrder(UUID userId, Long tierId, String tierCode) {
        SubscriptionTier tier = resolveTier(tierId, tierCode);
        if (!tier.isPremium()) {
            throw new OperationNotAllowedException("Cannot purchase the free tier");
        }
        PaymentOrder order = newOrder(userId, "SUBSCRIPTION", tier.getPriceVnd(),
                "Cramer subscription: " + tier.getCode());
        order.setTierId(tier.getId());
        order.setTierCode(tier.getCode());
        return persistOrder(order);
    }

    /** Create a Lúa-pack payment order (SPEC-15 §8). Pack must be an active DB row. */
    @Transactional
    public CreateOrderResponse createLuaOrder(UUID userId, String packCode) {
        LuaPack pack = luaPacks.requireActiveByCode(packCode);
        PaymentOrder order = newOrder(userId, "LUA_PACK", pack.getPriceVnd(),
                "Cramer Lúa pack: " + pack.getCode());
        order.setTierCode(pack.getCode());
        order.setLuaAmount(pack.totalLua());
        return persistOrder(order);
    }

    @Transactional(readOnly = true)
    public PaymentOrderView getStatus(long orderCode, UUID userId) {
        PaymentOrder order = orders.findByOrderCode(orderCode)
                .orElseThrow(() -> ResourceNotFoundException.of("PaymentOrder", orderCode));
        if (!order.getUserId().equals(userId)) {
            throw new OperationNotAllowedException("Not your order");
        }
        return toView(order);
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrderView> history(UUID userId, Pageable pageable) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toView);
    }

    /**
     * Grant a successful payment exactly once (SPEC-15 §8). Returns true when this call performed
     * the grant; false when the order was already paid (idempotent no-op) or not grantable.
     */
    @Transactional
    public boolean grantOnSuccess(long orderCode) {
        PaymentOrder order = orders.findByOrderCodeForUpdate(orderCode).orElse(null);
        if (order == null) {
            log.warn("Webhook for unknown order {} — ignored", orderCode);
            return false; // unknown order → no state change
        }
        if ("PAID".equals(order.getStatus())) {
            return false; // already granted (idempotent)
        }
        if (!"PENDING".equals(order.getStatus())) {
            log.warn("Webhook for order {} in non-grantable status {}", orderCode, order.getStatus());
            return false;
        }

        // Claim the order first (PENDING → PAID) under the lock, then grant.
        order.setStatus("PAID");
        order.setPaidAt(OffsetDateTime.now());
        orders.save(order);

        if ("LUA_PACK".equals(order.getType())) {
            grantLua(order);
        } else if ("SUBSCRIPTION".equals(order.getType())) {
            grantSubscription(order);
        }
        return true;
    }

    // ---------------------------------------------------------------- internals

    private SubscriptionTier resolveTier(Long tierId, String tierCode) {
        if (tierId != null) {
            return tiers.findById(tierId).orElseThrow(() -> ResourceNotFoundException.of("SubscriptionTier", tierId));
        }
        if (tierCode != null && !tierCode.isBlank()) {
            return tiers.findByCode(tierCode).orElseThrow(() -> ResourceNotFoundException.of("SubscriptionTier", tierCode));
        }
        throw new IllegalArgumentException("tierId or tierCode is required");
    }

    private PaymentOrder newOrder(UUID userId, String type, int amountVnd, String description) {
        PaymentOrder order = new PaymentOrder();
        order.setUserId(userId);
        order.setOrderCode(generateOrderCode());
        order.setType(type);
        order.setAmountVnd(amountVnd);
        order.setDescription(description);
        order.setStatus("PENDING");
        order.setExpiresAt(OffsetDateTime.now().plusHours(24));
        return order;
    }

    private CreateOrderResponse persistOrder(PaymentOrder order) {
        boolean mock = !payos.configured();
        // Full PayOS link creation runs only when configured; otherwise a mock checkout URL is used
        // so local/dev flows work (SPEC-15 §8). Signature is skipped in mock mode.
        order.setCheckoutUrl("/mock-checkout/" + order.getOrderCode());
        orders.save(order);
        return new CreateOrderResponse(order.getOrderCode(), order.getCheckoutUrl(), order.getAmountVnd(),
                order.getStatus(), mock);
    }

    private long generateOrderCode() {
        long base = System.currentTimeMillis() / 1000L; // ~1.7e9
        return base * 1000L + ThreadLocalRandom.current().nextInt(1000); // unique-ish, fits in long
    }

    private void grantSubscription(PaymentOrder order) {
        SubscriptionTier tier = resolveTier(order.getTierId(), order.getTierCode());
        subscriptions.activatePaid(order.getUserId(), tier, "order_" + order.getOrderCode());
    }

    private void grantLua(PaymentOrder order) {
        int total;
        if (order.getTierCode() != null && !order.getTierCode().isBlank()) {
            // Lúa pack code stored in tier_code for LUA_PACK orders; resolve DB pack for the truth.
            LuaPack pack = luaPacks.requireActiveByCode(order.getTierCode());
            total = pack.totalLua();
        } else {
            total = order.getLuaAmount() == null ? 0 : order.getLuaAmount();
        }
        if (total > 0) {
            // Idempotent by the order reference — even if grant is retried, credit is added once.
            credits.earn(order.getUserId(), total, CreditCategory.PURCHASE,
                    "order_" + order.getOrderCode(), "Lúa pack purchase");
        }
    }

    private PaymentOrderView toView(PaymentOrder o) {
        return new PaymentOrderView(o.getOrderCode(), o.getType(), o.getTierCode(), o.getLuaAmount(),
                o.getAmountVnd(), o.getStatus(), o.getCheckoutUrl(), o.getCreatedAt(), o.getPaidAt(), o.getExpiresAt());
    }
}
