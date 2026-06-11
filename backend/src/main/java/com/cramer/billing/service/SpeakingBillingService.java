package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Speaking billing (SPEC-15 §6) implementing {@link SpeakingBillingPort}. Deduct and refund are
 * idempotent by session id via the credit ledger reference ({@code session_{id}} /
 * {@code refund_session_{id}}), so a retried complete or a watchdog refund never double-charges
 * or double-refunds.
 */
@Service
public class SpeakingBillingService implements SpeakingBillingPort {

    private final CreditService credits;

    public SpeakingBillingService(CreditService credits) {
        this.credits = credits;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAfford(UUID userId, int luaCost) {
        return luaCost <= 0 || credits.balance(userId) >= luaCost;
    }

    @Override
    @Transactional
    public void deduct(UUID userId, long sessionId, int luaCost) {
        if (luaCost > 0) {
            credits.spend(userId, luaCost, CreditCategory.SPEAKING_SESSION, "session_" + sessionId, "Speaking session");
        }
    }

    @Override
    @Transactional
    public void refund(UUID userId, long sessionId, int luaCost) {
        if (luaCost > 0) {
            credits.refund(userId, luaCost, CreditCategory.SPEAKING_REFUND, "refund_session_" + sessionId,
                    "Speaking grading failed");
        }
    }
}
