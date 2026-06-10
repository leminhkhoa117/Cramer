package com.cramer.dto;

import com.cramer.entity.SkillQuota;
import com.cramer.entity.UserQuota;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for returning quota status to frontend.
 * Contains global and per-skill quota information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaStatusDTO {

    // Global usage counts
    private int globalAttempt;
    private int globalAttemptAI;
    
    // Global caps (constants for frontend display)
    private int globalAttemptCap;
    private int globalAttemptAICap;
    
    // Per-skill breakdown
    @Builder.Default
    private Map<String, SkillQuotaInfo> skills = new HashMap<>();
    
    // Premium flag (if true, all limits are unlimited)
    // @JsonProperty needed because Lombok generates getter `isPremium()` (no `get` prefix
    // for `boolean is*`), and Jackson defaults to JSON key "premium" — strips the `is`.
    // Frontend (useQuotaStore.js) reads `quotaStatus?.isPremium`.
    @JsonProperty("isPremium")
    private boolean isPremium;

    @JsonIgnore
    public boolean isPremium() {
        return isPremium;
    }
    
    // Quota month (for display)
    private String quotaMonth;

    /**
     * Nested class for per-skill quota info.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillQuotaInfo {
        private int attempt;
        private int attemptAI;
        private int attemptCap;
        private int attemptAICap;
        
        /**
         * Create from entity with default caps.
         */
        public static SkillQuotaInfo fromEntity(SkillQuota entity) {
            if (entity == null) {
                return SkillQuotaInfo.builder()
                        .attempt(0)
                        .attemptAI(0)
                        .attemptCap(SkillQuota.LOCAL_ATTEMPT_CAP)
                        .attemptAICap(SkillQuota.LOCAL_ATTEMPT_AI_CAP)
                        .build();
            }
            return SkillQuotaInfo.builder()
                    .attempt(entity.getAttemptCount())
                    .attemptAI(entity.getAttemptAiCount())
                    .attemptCap(SkillQuota.LOCAL_ATTEMPT_CAP)
                    .attemptAICap(SkillQuota.LOCAL_ATTEMPT_AI_CAP)
                    .build();
        }
        
        /**
         * Create empty/default skill info.
         */
        public static SkillQuotaInfo empty() {
            return SkillQuotaInfo.builder()
                    .attempt(0)
                    .attemptAI(0)
                    .attemptCap(SkillQuota.LOCAL_ATTEMPT_CAP)
                    .attemptAICap(SkillQuota.LOCAL_ATTEMPT_AI_CAP)
                    .build();
        }
    }

    /**
     * Create DTO for premium user (unlimited).
     */
    public static QuotaStatusDTO forPremiumUser(String quotaMonth) {
        Map<String, SkillQuotaInfo> skills = new HashMap<>();
        for (SkillQuota.Skill skill : SkillQuota.Skill.values()) {
            skills.put(skill.name(), SkillQuotaInfo.empty());
        }
        return QuotaStatusDTO.builder()
                .globalAttempt(0)
                .globalAttemptAI(0)
                .globalAttemptCap(-1)  // -1 indicates unlimited
                .globalAttemptAICap(-1)
                .skills(skills)
                .isPremium(true)
                .quotaMonth(quotaMonth)
                .build();
    }

    /**
     * Create DTO from entities.
     */
    public static QuotaStatusDTO fromEntities(UserQuota userQuota, 
                                               Map<SkillQuota.Skill, SkillQuota> skillQuotas,
                                               String quotaMonth) {
        Map<String, SkillQuotaInfo> skills = new HashMap<>();
        for (SkillQuota.Skill skill : SkillQuota.Skill.values()) {
            SkillQuota sq = skillQuotas.get(skill);
            skills.put(skill.name(), SkillQuotaInfo.fromEntity(sq));
        }
        
        return QuotaStatusDTO.builder()
                .globalAttempt(userQuota != null ? userQuota.getAttemptCount() : 0)
                .globalAttemptAI(userQuota != null ? userQuota.getAttemptAiCount() : 0)
                .globalAttemptCap(UserQuota.GLOBAL_ATTEMPT_CAP)
                .globalAttemptAICap(UserQuota.GLOBAL_ATTEMPT_AI_CAP)
                .skills(skills)
                .isPremium(false)
                .quotaMonth(quotaMonth)
                .build();
    }
}
