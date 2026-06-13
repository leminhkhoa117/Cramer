package com.cramer.admin.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.service.CreditResult;
import com.cramer.billing.service.CreditService;
import com.cramer.billing.service.SubscriptionService;
import com.cramer.engagement.service.ActivityPort;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock JdbcTemplate jdbc;
    @Mock CreditService creditService;
    @Mock SubscriptionService subscriptionService;
    @Mock AuditPort audit;
    @Mock ActivityPort activity;

    private AdminUserService service() {
        return new AdminUserService(jdbc, creditService, subscriptionService, audit, activity);
    }

    @Test
    void positiveAdjustmentEarnsAndAuditsAdd() {
        UUID admin = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(creditService.earn(eq(user), eq(50), eq(CreditCategory.ADMIN_ADJUSTMENT), any(), any()))
                .thenReturn(CreditResult.applied(150));

        int balance = service().adjustCredits(admin, user, 50, "promo");

        assertThat(balance).isEqualTo(150);
        verify(audit).record(eq(admin), eq("CREDITS_ADD"), eq("USER"), eq(user.toString()), any(),
                any(), any(JsonNode.class));
        verify(activity).log(eq(user), eq("CREDITS_ADJUSTED"), any(), any(), any(JsonNode.class));
    }

    @Test
    void negativeAdjustmentSpendsAndAuditsSubtract() {
        UUID admin = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(creditService.spend(eq(user), eq(20), eq(CreditCategory.ADMIN_ADJUSTMENT), any(), any()))
                .thenReturn(CreditResult.applied(80));

        int balance = service().adjustCredits(admin, user, -20, "correction");

        assertThat(balance).isEqualTo(80);
        verify(audit).record(eq(admin), eq("CREDITS_SUBTRACT"), eq("USER"), eq(user.toString()), any(),
                any(), any(JsonNode.class));
    }

    @Test
    void zeroAdjustmentRejected() {
        assertThatThrownBy(() -> service().adjustCredits(UUID.randomUUID(), UUID.randomUUID(), 0, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
