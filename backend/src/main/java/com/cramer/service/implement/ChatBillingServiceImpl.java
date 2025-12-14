package com.cramer.service.implement;

import com.cramer.entity.CreditTransaction;
import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserSubscription;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.ChatBillingService;
import com.cramer.service.CreditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of ChatBillingService.
 * Handles chatbot quota checking and Lúa billing.
 */
@Service
@Transactional
public class ChatBillingServiceImpl implements ChatBillingService {

    private static final Logger logger = LoggerFactory.getLogger(ChatBillingServiceImpl.class);

    // Default overage cost if not set in tier
    private static final int DEFAULT_CHATBOT_OVERAGE_COST = 2;

    private final UserSubscriptionRepository subscriptionRepository;
    private final CreditService creditService;

    @Autowired
    public ChatBillingServiceImpl(
            UserSubscriptionRepository subscriptionRepository,
            @Lazy CreditService creditService) {
        this.subscriptionRepository = subscriptionRepository;
        this.creditService = creditService;
    }

    @Override
    public ChatBillingResult processChatBilling(UUID userId) {
        logger.info("💳 Processing chat billing for user: {}", userId);

        // Get user's subscription
        UserSubscription sub = subscriptionRepository.findActiveByUserId(userId).orElse(null);
        if (sub == null || sub.getTier() == null) {
            logger.warn("⚠️ No subscription found for user {}", userId);
            return ChatBillingResult.blocked("Không tìm thấy gói đăng ký. Vui lòng đăng nhập lại.");
        }

        SubscriptionTier tier = sub.getTier();
        Integer limit = tier.getChatbotMonthlyLimit();
        int overageCost = tier.getChatbotOverageCost() != null
                ? tier.getChatbotOverageCost()
                : DEFAULT_CHATBOT_OVERAGE_COST;

        // Check if unlimited
        if (limit != null && limit < 0) {
            logger.debug("⭐ User {} has unlimited chatbot messages", userId);
            incrementChatUsage(sub);
            return ChatBillingResult.allowed(-1);
        }

        // Get current usage
        int used = sub.getChatbotUsed() != null ? sub.getChatbotUsed() : 0;
        int remaining = (limit != null) ? Math.max(0, limit - used) : 0;

        // Within quota
        if (remaining > 0) {
            logger.debug("✅ User {} within chatbot quota ({}/{})", userId, used, limit);
            incrementChatUsage(sub);
            return ChatBillingResult.allowed(remaining - 1);
        }

        // Over quota - need to charge Lúa
        logger.info("⚠️ User {} exceeded chatbot quota, checking Lúa balance", userId);

        if (!creditService.hasEnoughCredits(userId, overageCost)) {
            logger.warn("❌ User {} has insufficient Lúa ({} required)", userId, overageCost);
            return ChatBillingResult.blocked(
                    "Đã hết lượt hỏi miễn phí trong tháng. Cần " + overageCost + " Lúa để tiếp tục.");
        }

        // Charge Lúa
        creditService.spendCredits(userId, overageCost,
                CreditTransaction.Category.AI_GRADING,
                "Tin nhắn chatbot (vượt hạn mức tháng)");
        incrementChatUsage(sub);

        logger.info("💰 Charged {} Lúa for chatbot overage, user {}", overageCost, userId);
        return ChatBillingResult.charged(overageCost, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public int getRemainingMessages(UUID userId) {
        UserSubscription sub = subscriptionRepository.findActiveByUserId(userId).orElse(null);
        if (sub == null || sub.getTier() == null) {
            return 0;
        }

        SubscriptionTier tier = sub.getTier();
        Integer limit = tier.getChatbotMonthlyLimit();

        if (limit == null || limit < 0) {
            return -1; // Unlimited
        }

        int used = sub.getChatbotUsed() != null ? sub.getChatbotUsed() : 0;
        return Math.max(0, limit - used);
    }

    // ===== PRIVATE HELPERS =====

    private void incrementChatUsage(UserSubscription sub) {
        Integer used = sub.getChatbotUsed();
        sub.setChatbotUsed((used != null ? used : 0) + 1);
        subscriptionRepository.save(sub);
        logger.debug("📊 Incremented chatbot usage to {}", sub.getChatbotUsed());
    }
}
