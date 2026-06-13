package com.cramer.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A PayOS payment order, table {@code payment_orders} (SPEC-15 §8). {@code status} ∈ PENDING,
 * PAID, CANCELLED, EXPIRED, FAILED; {@code type} ∈ SUBSCRIPTION, LUA_PACK. The PENDING→PAID
 * transition is claimed under a row lock for concurrency-safe webhook idempotency.
 */
@Entity
@Table(name = "payment_orders", schema = "public")
@Getter
@Setter
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "tier_id")
    private Long tierId;

    @Column(name = "tier_code")
    private String tierCode;

    @Column(name = "lua_amount")
    private Integer luaAmount;

    @Column(name = "amount_vnd", nullable = false)
    private Integer amountVnd;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "transaction_datetime")
    private String transactionDatetime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
