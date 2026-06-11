package com.cramer.billing.web;

import com.cramer.billing.domain.CreditTransaction;
import com.cramer.billing.service.CreditService;
import com.cramer.billing.service.LuaPackService;
import com.cramer.billing.service.PaymentService;
import com.cramer.billing.web.dto.BillingRequests;
import com.cramer.billing.web.dto.CreateOrderResponse;
import com.cramer.billing.web.dto.CreditStatsView;
import com.cramer.billing.web.dto.LuaPackView;
import com.cramer.billing.web.dto.TransactionView;
import com.cramer.platform.security.CurrentUser;
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
import java.util.UUID;

/**
 * Lúa credit endpoints (SPEC-15 §9). Balance/stats/transactions are scoped to the authenticated
 * user; packages are DB-driven; purchase creates a payment order.
 */
@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditService credits;
    private final LuaPackService luaPacks;
    private final PaymentService payments;
    private final CurrentUser currentUser;

    public CreditController(CreditService credits, LuaPackService luaPacks, PaymentService payments,
                            CurrentUser currentUser) {
        this.credits = credits;
        this.luaPacks = luaPacks;
        this.payments = payments;
        this.currentUser = currentUser;
    }

    @GetMapping
    public CreditStatsView balance() {
        return credits.stats(currentUser.requireUserId());
    }

    @GetMapping("/stats")
    public CreditStatsView stats() {
        return credits.stats(currentUser.requireUserId());
    }

    @GetMapping("/check/{amount}")
    public Map<String, Object> check(@PathVariable int amount) {
        int balance = credits.balance(currentUser.requireUserId());
        return Map.of("sufficient", balance >= amount, "balance", balance, "required", amount);
    }

    @GetMapping("/transactions")
    public List<TransactionView> transactions(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return credits.transactions(currentUser.requireUserId(), PageRequest.of(page, Math.min(size, 100)))
                .map(this::toView).getContent();
    }

    @GetMapping("/history")
    public List<TransactionView> history(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return transactions(page, size);
    }

    @GetMapping("/packages")
    public List<LuaPackView> packages() {
        return luaPacks.listActive();
    }

    @PostMapping("/purchase")
    public CreateOrderResponse purchase(@RequestBody BillingRequests.CreateLuaOrder request) {
        return payments.createLuaOrder(currentUser.requireUserId(), request.packCode());
    }

    private TransactionView toView(CreditTransaction t) {
        return new TransactionView(t.getId(), t.getAmount(), t.getBalanceAfter(),
                t.getType() == null ? null : t.getType().name(), t.getCategory(),
                t.getDescription(), t.getReferenceId(), t.getCreatedAt());
    }
}
