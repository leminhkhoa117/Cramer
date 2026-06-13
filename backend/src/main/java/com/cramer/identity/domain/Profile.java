package com.cramer.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mirror of the Supabase auth user in the {@code profiles} table (SPEC-10 §1). The backend
 * never creates or deletes profiles — Supabase auth owns the lifecycle. Schema is frozen
 * (SPEC-00 §5); columns map the canonical {@code profiles} definition.
 */
@Entity
@Table(name = "profiles", schema = "public")
@Getter
@Setter
public class Profile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // == auth.users.id

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "hero_background_url")
    private String heroBackgroundUrl;

    @Column(name = "page_background_url")
    private String pageBackgroundUrl;

    @Column(name = "llm_api_key")
    private String llmApiKey;

    @Column(name = "llm_model")
    private String llmModel;

    @Column(name = "llm_provider")
    private String llmProvider = "deepseek";

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** True when a non-blank personal LLM API key is stored (the key itself is never exposed). */
    public boolean hasLlmApiKey() {
        return llmApiKey != null && !llmApiKey.isBlank();
    }
}
