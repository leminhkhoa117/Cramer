package com.cramer.identity.domain;

/**
 * Account lifecycle status, stored as varchar in {@code profiles.account_status}
 * (default {@code ACTIVE}). Written by admin (SPEC-17), read by identity (SPEC-10 §5).
 */
public enum AccountStatus {
    ACTIVE,
    BANNED,
    DEACTIVATED,
    DELETED
}
