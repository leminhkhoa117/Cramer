package com.cramer.dto;

import java.time.OffsetDateTime;

/**
 * Admin User DTO - Dữ liệu user cho Admin CMS
 * 
 * Chứa thông tin đầy đủ về user để hiển thị trong Admin panel.
 */
public class AdminUserDTO {
    
    private String id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String avatarUrl;
    
    // Subscription info
    private String subscription; // FREE, CRAMERICH
    private OffsetDateTime subscriptionStart;
    private OffsetDateTime subscriptionEnd;
    private Boolean autoRenew;
    
    // Credits (Lúa)
    private Integer credits;
    
    // Account status
    private String accountStatus; // ACTIVE, BANNED, DEACTIVATED, DELETED
    private String statusReason;
    
    // Activity info
    private OffsetDateTime lastLoginAt;
    private Integer totalTests;
    private Integer totalVocabulary;
    
    // Timestamps
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Constructors
    public AdminUserDTO() {}
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public String getSubscription() {
        return subscription;
    }
    
    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }
    
    public OffsetDateTime getSubscriptionStart() {
        return subscriptionStart;
    }
    
    public void setSubscriptionStart(OffsetDateTime subscriptionStart) {
        this.subscriptionStart = subscriptionStart;
    }
    
    public OffsetDateTime getSubscriptionEnd() {
        return subscriptionEnd;
    }
    
    public void setSubscriptionEnd(OffsetDateTime subscriptionEnd) {
        this.subscriptionEnd = subscriptionEnd;
    }
    
    public Boolean getAutoRenew() {
        return autoRenew;
    }
    
    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }
    
    public Integer getCredits() {
        return credits;
    }
    
    public void setCredits(Integer credits) {
        this.credits = credits;
    }
    
    public String getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
    
    public String getStatusReason() {
        return statusReason;
    }
    
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
    
    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public Integer getTotalTests() {
        return totalTests;
    }
    
    public void setTotalTests(Integer totalTests) {
        this.totalTests = totalTests;
    }
    
    public Integer getTotalVocabulary() {
        return totalVocabulary;
    }
    
    public void setTotalVocabulary(Integer totalVocabulary) {
        this.totalVocabulary = totalVocabulary;
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
}
