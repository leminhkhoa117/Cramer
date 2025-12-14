package com.cramer.service.implement;

import com.cramer.dto.GradingStatusDTO;
import com.cramer.dto.SubscriptionStatusDTO;
import com.cramer.dto.SubscriptionTierDTO;
import com.cramer.dto.UserSubscriptionDTO;
import com.cramer.entity.ChatbotUsage;
import com.cramer.entity.PaymentOrder;
import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserCredit;
import com.cramer.entity.UserSubscription;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.ChatbotUsageRepository;
import com.cramer.repository.PaymentOrderRepository;
import com.cramer.repository.SubscriptionTierRepository;
import com.cramer.repository.UserCreditRepository;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.CreditService;
import com.cramer.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of SubscriptionService.
 * Manages subscription tiers, user subscriptions, and AI grading limits.
 */
@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private static final String FREE_TIER_CODE = "cramerie";

    private final SubscriptionTierRepository tierRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserCreditRepository creditRepository;
    private final ChatbotUsageRepository chatbotUsageRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final CreditService creditService;

    // Tier code to emoji mapping
    private static final Map<String, String> TIER_EMOJIS = Map.of(
            "cramerie", "🌾",
            "cramerich", "🌻",
            "cramerous", "🌟"
    );

    @Autowired
    public SubscriptionServiceImpl(
            SubscriptionTierRepository tierRepository,
            UserSubscriptionRepository subscriptionRepository,
            UserCreditRepository creditRepository,
            ChatbotUsageRepository chatbotUsageRepository,
            PaymentOrderRepository paymentOrderRepository,
            @Lazy CreditService creditService) {
        this.tierRepository = tierRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.creditRepository = creditRepository;
        this.chatbotUsageRepository = chatbotUsageRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.creditService = creditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTierDTO> getAllTiers() {
        logger.info("📋 Fetching all subscription tiers");
        return tierRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(SubscriptionTierDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionTierDTO getTierByCode(String code) {
        logger.info("🔍 Fetching tier by code: {}", code);
        SubscriptionTier tier = tierRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", "code", code));
        return SubscriptionTierDTO.fromEntity(tier);
    }

    @Override
    public UserSubscriptionDTO getUserSubscription(UUID userId) {
        logger.info("👤 Fetching subscription for user: {}", userId);
        
        return subscriptionRepository.findActiveByUserId(userId)
                .map(UserSubscriptionDTO::fromEntity)
                .orElseGet(() -> {
                    logger.info("⚠️ No active subscription found for user {}, creating free tier", userId);
                    return initializeNewUser(userId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public GradingStatusDTO checkAIGradingAllowed(UUID userId) {
        logger.info("🔍 Checking AI grading status for user: {}", userId);
        
        // Get user's credit balance
        int luaBalance = creditRepository.findByUserId(userId)
                .map(UserCredit::getBalance)
                .orElse(0);
        
        // Get subscription
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElse(null);
        
        if (subscription == null || subscription.getTier() == null) {
            // No subscription - treat as free tier
            logger.info("⚠️ No subscription for user {}, treating as free tier", userId);
            GradingStatusDTO status = GradingStatusDTO.freeTierBlocked(luaBalance);
            status.setTierCode(FREE_TIER_CODE);
            return status;
        }
        
        SubscriptionTier tier = subscription.getTier();
        int used = subscription.getAiGradingsUsed();
        int limit = tier.getIncludedAiGradings();
        
        // Free tier (cramerie) has 0 included gradings
        if (limit == 0) {
            GradingStatusDTO status = GradingStatusDTO.freeTierBlocked(luaBalance);
            status.setTierCode(tier.getCode());
            return status;
        }
        
        // Check if within limit
        if (used < limit) {
            GradingStatusDTO status = GradingStatusDTO.allowed(used, limit, luaBalance);
            status.setTierCode(tier.getCode());
            return status;
        }
        
        // Limit reached - check if can use Lúa
        GradingStatusDTO status = GradingStatusDTO.limitReachedWithLua(used, limit, luaBalance);
        status.setTierCode(tier.getCode());
        return status;
    }

    @Override
    public UserSubscriptionDTO incrementAIGradingUsage(UUID userId) {
        logger.info("➕ Incrementing AI grading usage for user: {}", userId);
        
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserSubscription", "userId", userId));
        
        Long subscriptionId = subscription.getId();
        subscriptionRepository.incrementAiGradingsUsed(subscriptionId);
        
        // Refresh entity
        UserSubscription refreshedSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("UserSubscription", "id", subscriptionId));
        
        logger.info("✅ AI grading usage incremented to {} for user {}", 
                refreshedSubscription.getAiGradingsUsed(), userId);
        
        return UserSubscriptionDTO.fromEntity(refreshedSubscription);
    }

    @Override
    @Transactional(readOnly = true)
    public int getMonthlyGradingsRemaining(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(UserSubscription::getRemainingAiGradings)
                .orElse(0);
    }

    @Override
    public UserSubscriptionDTO initializeNewUser(UUID userId) {
        logger.info("🆕 Initializing new user subscription: {}", userId);
        
        // Check if already has subscription
        if (subscriptionRepository.existsByUserId(userId)) {
            return subscriptionRepository.findActiveByUserId(userId)
                    .map(UserSubscriptionDTO::fromEntity)
                    .orElseThrow(() -> new ResourceNotFoundException("UserSubscription", "userId", userId));
        }
        
        // Get free tier
        SubscriptionTier freeTier = tierRepository.findFreeTier()
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionTier", "code", FREE_TIER_CODE));
        
        // Create subscription
        UserSubscription subscription = UserSubscription.builder()
                .userId(userId)
                .tier(freeTier)
                .status(UserSubscription.Status.ACTIVE)
                .aiGradingsUsed(0)
                .autoRenew(false)
                .build();
        
        subscription = subscriptionRepository.save(subscription);
        logger.info("✅ Created free tier subscription for user {}", userId);
        
        // Initialize credits with initial Lúa bonus
        creditService.initializeCredits(userId, freeTier.getInitialLua());
        
        return UserSubscriptionDTO.fromEntity(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    @Deprecated
    public int getDailyChatLimit(UUID userId) {
        // Deprecated: Use getMonthlyChatLimit instead
        // Returns chatbotMonthlyLimit for backward compatibility
        return getMonthlyChatLimit(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getMonthlyChatLimit(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(sub -> {
                    Integer limit = sub.getTier().getChatbotMonthlyLimit();
                    return limit != null ? limit : 50; // Default free tier limit
                })
                .orElse(50); // Default free tier limit
    }

    @Override
    @Transactional(readOnly = true)
    public int getRemainingChatMessages(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(sub -> {
                    Integer limit = sub.getTier().getChatbotMonthlyLimit();
                    if (limit == null || limit < 0) {
                        return -1; // Unlimited
                    }
                    Integer used = sub.getChatbotUsed();
                    return Math.max(0, limit - (used != null ? used : 0));
                })
                .orElse(50); // Default free tier remaining
    }

    @Override
    @Transactional
    public void incrementChatUsage(UUID userId) {
        subscriptionRepository.findActiveByUserId(userId).ifPresent(sub -> {
            Integer used = sub.getChatbotUsed();
            sub.setChatbotUsed((used != null ? used : 0) + 1);
            subscriptionRepository.save(sub);
            logger.debug("📊 Incremented chatbot usage for user {}: {}", userId, sub.getChatbotUsed());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusDTO getSubscriptionStatus(UUID userId) {
        logger.info("📊 Getting comprehensive subscription status for user: {}", userId);

        // Get subscription (creates free tier if none exists)
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElse(null);

        // If no subscription, initialize one
        if (subscription == null) {
            logger.info("⚠️ No subscription found, initializing free tier for user: {}", userId);
            initializeNewUser(userId);
            subscription = subscriptionRepository.findActiveByUserId(userId).orElse(null);
        }

        if (subscription == null) {
            throw new ResourceNotFoundException("UserSubscription", "userId", userId);
        }

        SubscriptionTier tier = subscription.getTier();

        // Build tier info
        SubscriptionStatusDTO.TierInfo tierInfo = SubscriptionStatusDTO.TierInfo.builder()
                .code(tier.getCode())
                .nameVi(tier.getNameVi())
                .nameEn(tier.getNameEn())
                .emoji(TIER_EMOJIS.getOrDefault(tier.getCode(), "📦"))
                .priceVnd(tier.getPriceVnd())
                .displayOrder(tier.getDisplayOrder())
                .isFree(tier.getPriceVnd() == null || tier.getPriceVnd() == 0)
                .build();

        // Build subscription info
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startedAt = subscription.getStartedAt();
        OffsetDateTime expiresAt = subscription.getExpiresAt();
        boolean isLifetime = (expiresAt == null);
        
        Integer daysRemaining = null;
        Double progressPercent = null;
        
        if (!isLifetime && expiresAt != null && startedAt != null) {
            long totalDays = ChronoUnit.DAYS.between(startedAt, expiresAt);
            long daysElapsed = ChronoUnit.DAYS.between(startedAt, now);
            daysRemaining = (int) Math.max(0, ChronoUnit.DAYS.between(now, expiresAt));
            progressPercent = totalDays > 0 ? Math.min(100.0, (daysElapsed * 100.0) / totalDays) : 0.0;
        }

        SubscriptionStatusDTO.SubscriptionInfo subscriptionInfo = SubscriptionStatusDTO.SubscriptionInfo.builder()
                .id(subscription.getId())
                .status(subscription.getStatus().name())
                .startedAt(startedAt)
                .expiresAt(expiresAt)
                .daysRemaining(daysRemaining)
                .progressPercent(progressPercent)
                .autoRenew(subscription.getAutoRenew())
                .isLifetime(isLifetime)
                .aiGradingEnabled(subscription.getAiGradingEnabled() != null ? subscription.getAiGradingEnabled() : true)
                .canEnableAiGrading(!tier.getCode().equals("cramerie")) // Only non-Cramerie users can enable AI grading
                .build();

        // Build ATTEMPT usage info (regular test attempts)
        int attemptsUsed = subscription.getAttemptsUsed() != null ? subscription.getAttemptsUsed() : 0;
        int attemptsLimit = tier.getMonthlyAttemptLimit() != null ? tier.getMonthlyAttemptLimit() : 0;
        boolean attemptsUnlimited = (attemptsLimit < 0);
        
        SubscriptionStatusDTO.UsageInfo attemptsUsage = SubscriptionStatusDTO.UsageInfo.builder()
                .used(attemptsUsed)
                .limit(attemptsUnlimited ? null : attemptsLimit)
                .remaining(attemptsUnlimited ? null : Math.max(0, attemptsLimit - attemptsUsed))
                .progressPercent(attemptsUnlimited || attemptsLimit == 0 ? 0.0 : Math.min(100.0, (attemptsUsed * 100.0) / attemptsLimit))
                .isUnlimited(attemptsUnlimited)
                .resetInfo("Đặt lại hàng tháng")
                .build();

        // Build ATTEMPT_AI usage info (AI graded attempts)
        int attemptAisUsed = subscription.getAttemptAisUsed() != null ? subscription.getAttemptAisUsed() : 0;
        int attemptAisLimit = tier.getMonthlyAttemptAiLimit() != null ? tier.getMonthlyAttemptAiLimit() : 0;
        boolean attemptAisUnlimited = (attemptAisLimit < 0);
        
        SubscriptionStatusDTO.UsageInfo attemptAisUsage = SubscriptionStatusDTO.UsageInfo.builder()
                .used(attemptAisUsed)
                .limit(attemptAisUnlimited ? null : attemptAisLimit)
                .remaining(attemptAisUnlimited ? null : Math.max(0, attemptAisLimit - attemptAisUsed))
                .progressPercent(attemptAisUnlimited || attemptAisLimit == 0 ? 0.0 : Math.min(100.0, (attemptAisUsed * 100.0) / attemptAisLimit))
                .isUnlimited(attemptAisUnlimited)
                .resetInfo("Đặt lại hàng tháng")
                .build();

        // Build chatbot usage info (monthly)
        int chatbotUsed = subscription.getChatbotUsed() != null ? subscription.getChatbotUsed() : 0;
        int chatbotLimit = tier.getChatbotMonthlyLimit() != null ? tier.getChatbotMonthlyLimit() : 0;
        boolean chatbotUnlimited = (chatbotLimit < 0);

        SubscriptionStatusDTO.UsageInfo chatbotUsage = SubscriptionStatusDTO.UsageInfo.builder()
                .used(chatbotUsed)
                .limit(chatbotUnlimited ? null : chatbotLimit)
                .remaining(chatbotUnlimited ? null : Math.max(0, chatbotLimit - chatbotUsed))
                .progressPercent(chatbotUnlimited || chatbotLimit == 0 ? 0.0 : Math.min(100.0, (chatbotUsed * 100.0) / chatbotLimit))
                .isUnlimited(chatbotUnlimited)
                .resetInfo("Đặt lại hàng tháng")
                .build();

        // Build translation usage info (daily - tracked in separate table)
        int translationLimit = tier.getDailyTranslationLimit() != null ? tier.getDailyTranslationLimit() : 0;
        boolean translationUnlimited = (translationLimit < 0);
        // TODO: Get translation usage from daily tracking table when implemented
        int translationUsed = 0;

        SubscriptionStatusDTO.UsageInfo translationUsage = SubscriptionStatusDTO.UsageInfo.builder()
                .used(translationUsed)
                .limit(translationUnlimited ? null : translationLimit)
                .remaining(translationUnlimited ? null : Math.max(0, translationLimit - translationUsed))
                .progressPercent(translationUnlimited || translationLimit == 0 ? 0.0 : Math.min(100.0, (translationUsed * 100.0) / translationLimit))
                .isUnlimited(translationUnlimited)
                .resetInfo("Đặt lại hàng ngày")
                .build();

        // Build vocabulary usage info (lifetime limit)
        int vocabularyLimit = tier.getMaxVocabularyEntries() != null ? tier.getMaxVocabularyEntries() : 0;
        boolean vocabularyUnlimited = (vocabularyLimit < 0);
        // TODO: Get vocabulary count from vocabulary table when implemented
        int vocabularyUsed = 0;

        SubscriptionStatusDTO.UsageInfo vocabularyUsage = SubscriptionStatusDTO.UsageInfo.builder()
                .used(vocabularyUsed)
                .limit(vocabularyUnlimited ? null : vocabularyLimit)
                .remaining(vocabularyUnlimited ? null : Math.max(0, vocabularyLimit - vocabularyUsed))
                .progressPercent(vocabularyUnlimited || vocabularyLimit == 0 ? 0.0 : Math.min(100.0, (vocabularyUsed * 100.0) / vocabularyLimit))
                .isUnlimited(vocabularyUnlimited)
                .resetInfo("Giới hạn tổng số từ")
                .build();

        // Build credit info
        UserCredit userCredit = creditRepository.findByUserId(userId).orElse(null);
        SubscriptionStatusDTO.CreditInfo creditInfo = SubscriptionStatusDTO.CreditInfo.builder()
                .balance(userCredit != null ? userCredit.getBalance() : 0)
                .lifetimeEarned(userCredit != null ? userCredit.getLifetimeEarned() : 0)
                .lifetimeSpent(userCredit != null ? userCredit.getLifetimeSpent() : 0)
                .build();

        // Get features list - handle both array and object formats
        List<String> features = new ArrayList<>();
        if (tier.getFeatures() != null && !tier.getFeatures().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String featuresJson = tier.getFeatures().trim();
                
                if (featuresJson.startsWith("[")) {
                    // Parse as array of strings
                    features = mapper.readValue(featuresJson, 
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                } else if (featuresJson.startsWith("{")) {
                    // Parse as object and extract keys with true values
                    java.util.Map<String, Object> featuresMap = mapper.readValue(featuresJson,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    for (java.util.Map.Entry<String, Object> entry : featuresMap.entrySet()) {
                        if (Boolean.TRUE.equals(entry.getValue())) {
                            features.add(entry.getKey());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("⚠️ Failed to parse tier features JSON: {} - Content: {}", 
                    e.getMessage(), tier.getFeatures());
            }
        }

        // Get recent payments (last 5)
        List<SubscriptionStatusDTO.PaymentInfo> recentPayments = paymentOrderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .stream()
                .map(order -> SubscriptionStatusDTO.PaymentInfo.builder()
                        .orderCode(order.getOrderCode())
                        .type(order.getType().name())
                        .amountVnd(order.getAmountVnd())
                        .status(order.getStatus().name())
                        .description(order.getDescription())
                        .createdAt(order.getCreatedAt())
                        .paidAt(order.getPaidAt())
                        .build())
                .collect(Collectors.toList());

        // Build final DTO
        return SubscriptionStatusDTO.builder()
                .userId(userId)
                .tier(tierInfo)
                .subscription(subscriptionInfo)
                .attempts(attemptsUsage)
                .attemptAis(attemptAisUsage)
                .chatbot(chatbotUsage)
                .translation(translationUsage)
                .vocabulary(vocabularyUsage)
                .credits(creditInfo)
                .features(features)
                .recentPayments(recentPayments)
                .build();
    }

    @Override
    @Transactional
    public boolean setAiGradingEnabled(UUID userId, boolean enabled) {
        logger.info("🔄 Setting AI grading enabled={} for user: {}", enabled, userId);
        
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserSubscription", "userId", userId));
        
        String tierCode = subscription.getTier().getCode();
        
        // Cramerie users cannot enable AI grading
        if (enabled && "cramerie".equals(tierCode)) {
            logger.warn("⚠️ Cramerie user {} tried to enable AI grading", userId);
            throw new IllegalStateException(
                "Tùy chọn Lượt chấm nâng cao chỉ dành cho người dùng gói Cramerich. " +
                "Hãy nâng cấp lên gói Cramerich để mở khóa tính năng này!"
            );
        }
        
        subscription.setAiGradingEnabled(enabled);
        subscriptionRepository.save(subscription);
        
        logger.info("✅ AI grading {} for user {}", enabled ? "enabled" : "disabled", userId);
        return enabled;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAiGradingEnabled(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(sub -> {
                    // Cramerie users always have AI grading disabled
                    if ("cramerie".equals(sub.getTier().getCode())) {
                        return false;
                    }
                    return sub.getAiGradingEnabled() != null ? sub.getAiGradingEnabled() : true;
                })
                .orElse(false);
    }
}
