package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.CreditTransaction;
import com.cramer.billing.domain.UserCredit;
import com.cramer.billing.repository.CreditTransactionRepository;
import com.cramer.billing.repository.UserCreditRepository;
import com.cramer.platform.error.QuotaExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock UserCreditRepository credits;
    @Mock CreditTransactionRepository transactions;

    private CreditService service() {
        lenient().when(credits.save(any(UserCredit.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transactions.save(any(CreditTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        return new CreditService(credits, transactions);
    }

    private UserCredit credit(UUID user, int balance) {
        UserCredit c = new UserCredit();
        c.setUserId(user);
        c.setBalance(balance);
        c.setLifetimeEarned(balance);
        c.setLifetimeSpent(0);
        return c;
    }

    @Test
    @DisplayName("spend deducts and records a signed ledger row")
    void spendDeducts() {
        UUID user = UUID.randomUUID();
        UserCredit c = credit(user, 100);
        when(credits.findByUserIdForUpdate(user)).thenReturn(Optional.of(c));
        when(transactions.findFirstByUserIdAndReferenceIdAndCategory(eq(user), eq("ref1"), eq("AI_GRADING")))
                .thenReturn(Optional.empty());

        CreditResult r = service().spend(user, 20, CreditCategory.AI_GRADING, "ref1", "grade");

        assertThat(r.applied()).isTrue();
        assertThat(r.balanceAfter()).isEqualTo(80);
        assertThat(c.getBalance()).isEqualTo(80);
        assertThat(c.getLifetimeSpent()).isEqualTo(20);
        verify(transactions).save(any(CreditTransaction.class));
    }

    @Test
    @DisplayName("spend with insufficient balance throws 402 INSUFFICIENT_LUA and does not mutate")
    void spendInsufficient() {
        UUID user = UUID.randomUUID();
        UserCredit c = credit(user, 5);
        when(credits.findByUserIdForUpdate(user)).thenReturn(Optional.of(c));
        when(transactions.findFirstByUserIdAndReferenceIdAndCategory(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().spend(user, 20, CreditCategory.AI_GRADING, "ref1", "grade"))
                .isInstanceOf(QuotaExceededException.class)
                .satisfies(e -> assertThat(((QuotaExceededException) e).blockType()).isEqualTo("INSUFFICIENT_LUA"));
        assertThat(c.getBalance()).isEqualTo(5);
        verify(transactions, never()).save(any());
    }

    @Test
    @DisplayName("a repeat spend with the same reference is an idempotent no-op (no double charge)")
    void spendIdempotent() {
        UUID user = UUID.randomUUID();
        UserCredit c = credit(user, 80);
        when(credits.findByUserIdForUpdate(user)).thenReturn(Optional.of(c));
        when(transactions.findFirstByUserIdAndReferenceIdAndCategory(eq(user), eq("ref1"), eq("AI_GRADING")))
                .thenReturn(Optional.of(new CreditTransaction()));

        CreditResult r = service().spend(user, 20, CreditCategory.AI_GRADING, "ref1", "grade");

        assertThat(r.duplicate()).isTrue();
        assertThat(c.getBalance()).isEqualTo(80); // unchanged
        verify(transactions, never()).save(any());
    }

    @Test
    @DisplayName("refund adds Lúa back and records a REFUND row")
    void refundAddsBack() {
        UUID user = UUID.randomUUID();
        UserCredit c = credit(user, 50);
        c.setLifetimeSpent(20);
        when(credits.findByUserIdForUpdate(user)).thenReturn(Optional.of(c));
        when(transactions.findFirstByUserIdAndReferenceIdAndCategory(any(), any(), any()))
                .thenReturn(Optional.empty());

        CreditResult r = service().refund(user, 15, CreditCategory.SPEAKING_REFUND, "refund_session_9", "failed");

        assertThat(r.balanceAfter()).isEqualTo(65);
        assertThat(c.getLifetimeSpent()).isEqualTo(5);
        verify(transactions).save(any(CreditTransaction.class));
    }

    @Test
    @DisplayName("unreferenced spend is never treated as a duplicate")
    void unreferencedSpendApplies() {
        UUID user = UUID.randomUUID();
        UserCredit c = credit(user, 30);
        when(credits.findByUserIdForUpdate(user)).thenReturn(Optional.of(c));

        CreditResult r = service().spend(user, 10, CreditCategory.CHAT_EXTENSION, null, "chat");

        assertThat(r.applied()).isTrue();
        assertThat(c.getBalance()).isEqualTo(20);
        verify(transactions, never()).findFirstByUserIdAndReferenceIdAndCategory(any(), any(), any());
    }
}
