package com.cramer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing user profiles.
 * Linked to Supabase auth.users table via id (UUID).
 */
@Entity
@Table(name = "profiles", schema = "public")
public class Profile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // Mirrors auth.users.id

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public Profile() {
    }

    public Profile(UUID id, String username) {
        this.id = id;
        this.username = username;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
