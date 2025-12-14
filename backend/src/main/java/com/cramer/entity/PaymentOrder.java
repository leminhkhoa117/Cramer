package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a payment order for subscriptions or Lúa purchases.
 * Tracks the complete lifecycle of a payment from creation to completion.
 */
@Entity
@Table(name = "payment_orders", schema = "public", indexes = {
    @Index(name = "idx_payment_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_orders_order_code", columnList = "order_code", unique = true),
    @Index(name = "idx_payment_orders_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    /**
     * Payment type: SUBSCRIPTION for tier upgrades, LUA_PACK for Lúa purchases
     */
    public enum Type {
        SUBSCRIPTION,
        LUA_PACK
    }

    /**
     * Payment status lifecycle
     */
    public enum Status {
        PENDING,    // Payment created, waiting for user action
        PAID,       // Payment completed successfully
        CANCELLED,  // User cancelled the payment
        EXPIRED,    // Payment link expired
        FAILED      // Payment failed
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * PayOS unique order code.
     * Must be unique integer, max value: 9007199254740991
     */
    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    /**
     * PayOS payment link ID returned after creation.
     */
    @Column(name = "payment_link_id", length = 255)
    private String paymentLinkId;

    /**
     * Payment checkout URL for user to complete payment.
     */
    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    /**
     * QR code data for payment (optional).
     */
    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Type type;

    /**
     * Subscription tier ID (for SUBSCRIPTION type).
     */
    @Column(name = "tier_id")
    private Long tierId;

    /**
     * Subscription tier code for display (e.g., "cramerich").
     */
    @Column(name = "tier_code", length = 50)
    private String tierCode;

    /**
     * Lúa amount being purchased (for LUA_PACK type).
     */
    @Column(name = "lua_amount")
    private Integer luaAmount;

    /**
     * Payment amount in VND.
     */
    @Column(name = "amount_vnd", nullable = false)
    private Integer amountVnd;

    /**
     * Short description (max 25 chars for bank compatibility).
     */
    @Column(name = "description", length = 25)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * PayOS transaction datetime when payment was made.
     */
    @Column(name = "transaction_datetime", length = 50)
    private String transactionDatetime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when payment was completed.
     */
    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    /**
     * Timestamp when payment link expires.
     */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /**
     * Check if this order is still pending.
     */
    public boolean isPending() {
        return status == Status.PENDING;
    }

    /**
     * Check if this order has been paid.
     */
    public boolean isPaid() {
        return status == Status.PAID;
    }

    /**
     * Mark order as paid with current timestamp.
     */
    public void markAsPaid(String transactionDateTime) {
        this.status = Status.PAID;
        this.paidAt = OffsetDateTime.now();
        this.transactionDatetime = transactionDateTime;
    }
}
