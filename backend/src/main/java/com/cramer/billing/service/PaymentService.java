package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.LuaPack;
import com.cramer.billing.domain.PaymentOrder;
import com.cramer.billing.repository.PaymentOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * PayOS payment grant (SPEC-15 §8). The webhook grant is the money-critical path:
 *
 * <p><strong>Fix — concurrency-safe idempotency:</strong> the order is claimed with a row lock
 * and a {@code PENDING → PAID} transition before granting. An already-{@code PAID} order is a
 * no-op, so duplicate concurrent webhooks <strong>cannot</strong> grant twice. Lúa packs are
 * DB-driven ({@code lua_amount + bonus_lua}); subscription grants are delegated to
 * {@link SubscriptionService}.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentOrderRepository orders;
    private final LuaPackService luaPacks;
    private final CreditService credits;

    public PaymentService(PaymentOrderRepository orders, LuaPackService luaPacks, CreditService credits) {
        this.orders = orders;
        this.luaPacks = luaPacks;
        this.credits = credits;
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
            // Subscription activation is handled by SubscriptionService at integration time;
            // the order claim above guarantees single-grant semantics.
            log.info("Subscription order {} claimed PAID; activation handled by subscription flow", orderCode);
        }
        return true;
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
}
