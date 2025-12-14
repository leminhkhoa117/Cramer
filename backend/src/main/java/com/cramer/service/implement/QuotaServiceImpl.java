package com.cramer.service.implement;

import com.cramer.dto.QuotaStatusDTO;
import com.cramer.entity.SkillQuota;
import com.cramer.entity.UserQuota;
import com.cramer.repository.SkillQuotaRepository;
import com.cramer.repository.UserQuotaRepository;
import com.cramer.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of QuotaService.
 * Manages monthly quota tracking for Cramerie (free tier) users.
 */
@Service
@Transactional
public class QuotaServiceImpl implements QuotaService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaServiceImpl.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserQuotaRepository userQuotaRepository;
    private final SkillQuotaRepository skillQuotaRepository;

    @Autowired
    public QuotaServiceImpl(UserQuotaRepository userQuotaRepository,
                           SkillQuotaRepository skillQuotaRepository) {
        this.userQuotaRepository = userQuotaRepository;
        this.skillQuotaRepository = skillQuotaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaStatusDTO getQuotaStatus(UUID userId) {
        logger.info("📊 Getting quota status for user: {}", userId);
        
        LocalDate currentMonth = getFirstDayOfCurrentMonth();
        String monthStr = currentMonth.format(MONTH_FORMATTER);
        
        // Get or create global quota
        UserQuota userQuota = getOrCreateUserQuota(userId, currentMonth);
        
        // Get all skill quotas for current month
        List<SkillQuota> skillQuotaList = skillQuotaRepository.findAllByUserIdAndQuotaMonth(userId, currentMonth);
        Map<SkillQuota.Skill, SkillQuota> skillQuotas = new HashMap<>();
        for (SkillQuota sq : skillQuotaList) {
            skillQuotas.put(sq.getSkill(), sq);
        }
        
        return QuotaStatusDTO.fromEntities(userQuota, skillQuotas, monthStr);
    }

    @Override
    public void incrementAttempt(UUID userId, String skill, boolean isAI) {
        logger.info("➕ Incrementing {} quota for user {}, skill {}", 
                isAI ? "AI" : "regular", userId, skill);
        
        LocalDate currentMonth = getFirstDayOfCurrentMonth();
        SkillQuota.Skill skillEnum = SkillQuota.Skill.valueOf(skill.toUpperCase());
        
        // Ensure quota rows exist
        getOrCreateUserQuota(userId, currentMonth);
        getOrCreateSkillQuota(userId, skillEnum, currentMonth);
        
        // Increment global quota
        if (isAI) {
            userQuotaRepository.incrementAttemptAiCount(userId, currentMonth);
            skillQuotaRepository.incrementAttemptAiCount(userId, skillEnum, currentMonth);
        } else {
            userQuotaRepository.incrementAttemptCount(userId, currentMonth);
            skillQuotaRepository.incrementAttemptCount(userId, skillEnum, currentMonth);
        }
        
        logger.info("✅ Quota incremented for user {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAttempt(UUID userId, String skill, boolean isAI) {
        return !isGlobalCapHit(userId, isAI) && !isLocalCapHit(userId, skill, isAI);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGlobalCapHit(UUID userId, boolean isAI) {
        LocalDate currentMonth = getFirstDayOfCurrentMonth();
        
        UserQuota quota = userQuotaRepository.findByUserIdAndQuotaMonth(userId, currentMonth)
                .orElse(null);
        
        if (quota == null) {
            return false; // No quota record means no usage yet
        }
        
        return isAI ? quota.isAttemptAiCapReached() : quota.isAttemptCapReached();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocalCapHit(UUID userId, String skill, boolean isAI) {
        LocalDate currentMonth = getFirstDayOfCurrentMonth();
        SkillQuota.Skill skillEnum = SkillQuota.Skill.valueOf(skill.toUpperCase());
        
        SkillQuota quota = skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(
                userId, skillEnum, currentMonth).orElse(null);
        
        if (quota == null) {
            return false; // No quota record means no usage yet
        }
        
        return isAI ? quota.isAttemptAiCapReached() : quota.isAttemptCapReached();
    }

    // ===== PRIVATE HELPERS =====

    /**
     * Get first day of current month (e.g., 2025-12-01).
     */
    private LocalDate getFirstDayOfCurrentMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * Get or create UserQuota for current month.
     */
    private UserQuota getOrCreateUserQuota(UUID userId, LocalDate quotaMonth) {
        return userQuotaRepository.findByUserIdAndQuotaMonth(userId, quotaMonth)
                .orElseGet(() -> {
                    logger.info("🆕 Creating new UserQuota for user {} month {}", userId, quotaMonth);
                    UserQuota newQuota = UserQuota.builder()
                            .userId(userId)
                            .quotaMonth(quotaMonth)
                            .attemptCount(0)
                            .attemptAiCount(0)
                            .build();
                    return userQuotaRepository.save(newQuota);
                });
    }

    /**
     * Get or create SkillQuota for current month.
     */
    private SkillQuota getOrCreateSkillQuota(UUID userId, SkillQuota.Skill skill, LocalDate quotaMonth) {
        return skillQuotaRepository.findByUserIdAndSkillAndQuotaMonth(userId, skill, quotaMonth)
                .orElseGet(() -> {
                    logger.info("🆕 Creating new SkillQuota for user {} skill {} month {}", 
                            userId, skill, quotaMonth);
                    SkillQuota newQuota = SkillQuota.builder()
                            .userId(userId)
                            .skill(skill)
                            .quotaMonth(quotaMonth)
                            .attemptCount(0)
                            .attemptAiCount(0)
                            .build();
                    return skillQuotaRepository.save(newQuota);
                });
    }
}
