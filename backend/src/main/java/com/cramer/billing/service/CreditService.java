package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.CreditTransaction;
import com.cramer.billing.domain.TransactionType;
import com.cramer.billing.domain.UserCredit;
import com.cramer.billing.repository.CreditTransactionRepository;
import com.cramer.billing.repository.UserCreditRepository;
import com.cramer.platform.error.QuotaExceededException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lúa balance management (SPEC-15 §3). <strong>Money correctness</strong> is the priority:
 *
 * <ul>
 *   <li><b>Atomic</b>: every mutation first acquires a {@code PESSIMISTIC_WRITE} lock on the
 *       user's {@code user_credits} row, serializing concurrent mutations for that user.</li>
 *   <li><b>Idempotent by {@code (user, reference, category)}</b>: a repeat with the same
 *       reference is a no-op returning the prior balance (checked inside the lock, so it is
 *       race-safe even without a DB unique constraint, which the frozen schema lacks).</li>
 *   <li><b>Spend rejects</b> when {@code balance < amount} (→ 402 {@code INSUFFICIENT_LUA}).</li>
 * </ul>
 */
@Service
public class CreditService {

    private final UserCreditRepository credits;
    private final CreditTransactionRepository transactions;

    public CreditService(UserCreditRepository credits, CreditTransactionRepository transactions) {
        this.credits = credits;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public int balance(UUID userId) {
        return credits.findByUserId(userId).map(UserCredit::getBalance).orElse(0);
    }

    /** Whether a transaction with the given reference + category already exists (for refund gating). */
    @Transactional(readOnly = true)
    public boolean hasTransaction(UUID userId, String referenceId, CreditCategory category) {
        return referenceId != null && !referenceId.isBlank()
                && transactions.findFirstByUserIdAndReferenceIdAndCategory(userId, referenceId, category.name()).isPresent();
    }

    /** Add Lúa (earn/bonus/purchase). Idempotent by reference. */
    @Transactional
    public CreditResult earn(UUID userId, int amount, CreditCategory category, String referenceId, String description) {
        requirePositive(amount);
        UserCredit c = lockOrCreate(userId);
        CreditResult dup = duplicate(userId, referenceId, category, c);
        if (dup != null) {
            return dup;
        }
        int newBalance = c.getBalance() + amount;
        c.setBalance(newBalance);
        c.setLifetimeEarned(c.getLifetimeEarned() + amount);
        credits.save(c);
        record(userId, amount, newBalance, category.defaultType(), category, referenceId, description);
        return CreditResult.applied(newBalance);
    }

    /** Spend Lúa. Rejects with 402 when the balance is insufficient. Idempotent by reference. */
    @Transactional
    public CreditResult spend(UUID userId, int amount, CreditCategory category, String referenceId, String description) {
        requirePositive(amount);
        UserCredit c = lockOrCreate(userId);
        CreditResult dup = duplicate(userId, referenceId, category, c);
        if (dup != null) {
            return dup;
        }
        if (c.getBalance() < amount) {
            throw new QuotaExceededException("INSUFFICIENT_LUA",
                    "Not enough Lúa: need " + amount + ", have " + c.getBalance());
        }
        int newBalance = c.getBalance() - amount;
        c.setBalance(newBalance);
        c.setLifetimeSpent(c.getLifetimeSpent() + amount);
        credits.save(c);
        record(userId, -amount, newBalance, TransactionType.SPEND, category, referenceId, description);
        return CreditResult.applied(newBalance);
    }

    /** Refund Lúa (e.g. failed grading/speaking). Idempotent by reference. */
    @Transactional
    public CreditResult refund(UUID userId, int amount, CreditCategory category, String referenceId, String description) {
        requirePositive(amount);
        UserCredit c = lockOrCreate(userId);
        CreditResult dup = duplicate(userId, referenceId, category, c);
        if (dup != null) {
            return dup;
        }
        int newBalance = c.getBalance() + amount;
        c.setBalance(newBalance);
        // a refund reduces lifetime_spent (the spend is being reversed), floored at 0
        c.setLifetimeSpent(Math.max(0, c.getLifetimeSpent() - amount));
        credits.save(c);
        record(userId, amount, newBalance, TransactionType.REFUND, category, referenceId, description);
        return CreditResult.applied(newBalance);
    }

    private CreditResult duplicate(UUID userId, String referenceId, CreditCategory category, UserCredit current) {
        if (referenceId == null || referenceId.isBlank()) {
            return null; // unreferenced mutations are not deduped
        }
        return transactions.findFirstByUserIdAndReferenceIdAndCategory(userId, referenceId, category.name())
                .map(txn -> CreditResult.duplicate(current.getBalance()))
                .orElse(null);
    }

    private UserCredit lockOrCreate(UUID userId) {
        return credits.findByUserIdForUpdate(userId).orElseGet(() -> {
            UserCredit c = new UserCredit();
            c.setUserId(userId);
            c.setBalance(0);
            c.setLifetimeEarned(0);
            c.setLifetimeSpent(0);
            return credits.saveAndFlush(c);
        });
    }

    private void record(UUID userId, int signedAmount, int balanceAfter, TransactionType type,
                        CreditCategory category, String referenceId, String description) {
        CreditTransaction t = new CreditTransaction();
        t.setUserId(userId);
        t.setAmount(signedAmount);
        t.setBalanceAfter(balanceAfter);
        t.setType(type);
        t.setCategory(category.name());
        t.setReferenceId(referenceId);
        t.setDescription(description);
        transactions.save(t);
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
