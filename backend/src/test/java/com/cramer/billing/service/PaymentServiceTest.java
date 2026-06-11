package com.cramer.billing.service;

import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.PaymentOrder;
import com.cramer.billing.repository.PaymentOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentOrderRepository orders;
    @Mock LuaPackService luaPacks;
    @Mock CreditService credits;

    private PaymentService service() {
        return new PaymentService(orders, luaPacks, credits);
    }

    private PaymentOrder order(String status, String type) {
        PaymentOrder o = new PaymentOrder();
        o.setOrderCode(12345L);
        o.setUserId(UUID.randomUUID());
        o.setStatus(status);
        o.setType(type);
        o.setLuaAmount(100);
        return o;
    }

    @Test
    @DisplayName("a pending LUA_PACK order is claimed PAID and Lúa granted once (idempotent by order ref)")
    void grantsPendingOnce() {
        PaymentOrder o = order("PENDING", "LUA_PACK");
        when(orders.findByOrderCodeForUpdate(12345L)).thenReturn(Optional.of(o));

        boolean granted = service().grantOnSuccess(12345L);

        assertThat(granted).isTrue();
        assertThat(o.getStatus()).isEqualTo("PAID");
        verify(credits).earn(eq(o.getUserId()), eq(100), eq(CreditCategory.PURCHASE), eq("order_12345"), any());
    }

    @Test
    @DisplayName("an already-PAID order is an idempotent no-op (no double grant)")
    void alreadyPaidNoop() {
        when(orders.findByOrderCodeForUpdate(12345L)).thenReturn(Optional.of(order("PAID", "LUA_PACK")));

        boolean granted = service().grantOnSuccess(12345L);

        assertThat(granted).isFalse();
        verify(credits, never()).earn(any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("an unknown order causes no state change")
    void unknownOrderNoop() {
        when(orders.findByOrderCodeForUpdate(999L)).thenReturn(Optional.empty());

        boolean granted = service().grantOnSuccess(999L);

        assertThat(granted).isFalse();
        verify(orders, never()).save(any());
        verify(credits, never()).earn(any(), anyInt(), any(), any(), any());
    }
}
