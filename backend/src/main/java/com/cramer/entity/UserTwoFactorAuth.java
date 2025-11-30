package com.cramer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity for storing user two-factor authentication settings.
 * Linked to Supabase auth.users table via userId (UUID).
 */
@Entity
@Table(name = "user_two_factor_auth", schema = "public")
public class UserTwoFactorAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId; // References auth.users.id

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = false;

    @Column(name = "secret_key")
    private String secretKey; // Encrypted TOTP secret

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes; // JSON array of encrypted backup codes

    @Column(name = "preferred_method")
    @Enumerated(EnumType.STRING)
    private TwoFactorMethod preferredMethod = TwoFactorMethod.AUTHENTICATOR;

    @Column(name = "phone_number")
    private String phoneNumber; // For SMS-based 2FA

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;

    // Enum for 2FA methods
    public enum TwoFactorMethod {
        AUTHENTICATOR,
        SMS,
        EMAIL
    }

    // Constructors
    public UserTwoFactorAuth() {
    }

    public UserTwoFactorAuth(UUID userId) {
        this.userId = userId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(String backupCodes) {
        this.backupCodes = backupCodes;
    }

    public TwoFactorMethod getPreferredMethod() {
        return preferredMethod;
    }

    public void setPreferredMethod(TwoFactorMethod preferredMethod) {
        this.preferredMethod = preferredMethod;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(OffsetDateTime lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }
}
