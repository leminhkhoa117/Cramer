package com.cramer.billing.service;

import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.repository.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Flips expired {@code ACTIVE} subscriptions to {@code EXPIRED} daily at 00:05 Asia/Ho_Chi_Minh
 * (SPEC-15 §2). Active = {@code status=ACTIVE} AND ({@code expires_at} null or future).
 */
@Component
public class SubscriptionExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryScheduler.class);

    private final UserSubscriptionRepository subscriptions;

    public SubscriptionExpiryScheduler(UserSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void expireSubscriptions() {
        List<UserSubscription> expired = subscriptions.findExpired(OffsetDateTime.now());
        if (expired.isEmpty()) {
            return;
        }
        for (UserSubscription sub : expired) {
            sub.setStatus("EXPIRED");
        }
        subscriptions.saveAll(expired);
        log.info("Expired {} subscription(s)", expired.size());
    }
}
