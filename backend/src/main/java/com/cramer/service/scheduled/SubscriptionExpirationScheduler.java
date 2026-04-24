package com.cramer.service.scheduled;

import com.cramer.entity.UserSubscription;
import com.cramer.repository.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled job that flips subscriptions with status=ACTIVE and expires_at < now()
 * to status=EXPIRED so DB state stays consistent for reporting/analytics.
 *
 * <p>Without this job, paid subscriptions silently "disappear" from active queries
 * (filtered by expires_at > now) but remain status=ACTIVE in DB, causing the
 * "paying customer 404 on subscription page" bug (see BUG_AUDIT_2026-04-23.md — T4).
 *
 * <p>The same logic also runs opportunistically on user access inside
 * {@code SubscriptionServiceImpl.initializeNewUser}; this scheduler handles the
 * case where users don't log in for a long time.
 */
@Component
public class SubscriptionExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionExpirationScheduler.class);

    private final UserSubscriptionRepository subscriptionRepository;

    public SubscriptionExpirationScheduler(UserSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Run daily at 00:05 Asia/Ho_Chi_Minh.
     * Cron expression: second minute hour day month day-of-week
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void expireOldSubscriptions() {
        List<UserSubscription> expired = subscriptionRepository.findExpiredActiveSubscriptions();
        if (expired.isEmpty()) {
            logger.debug("⏰ Expiration check: no active subscriptions past expires_at");
            return;
        }

        for (UserSubscription sub : expired) {
            sub.setStatus(UserSubscription.Status.EXPIRED);
        }
        subscriptionRepository.saveAll(expired);

        logger.info("⏰ Auto-expired {} subscription(s) via scheduled job", expired.size());
    }
}
