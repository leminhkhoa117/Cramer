package com.cramer.dto;

import lombok.*;

/**
 * DTO for AI grading status/availability check.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingStatusDTO {

    private Boolean allowed;
    private Integer used;
    private Integer limit;
    private Integer remaining;
    private Integer luaBalance;
    private Integer luaCostForExtra;
    private Boolean canUseExtraWithLua;
    private String message;
    private String tierCode;
    
    /**
     * Create a response for when grading is allowed.
     */
    public static GradingStatusDTO allowed(int used, int limit, int luaBalance) {
        int remaining = Math.max(0, limit - used);
        return GradingStatusDTO.builder()
                .allowed(true)
                .used(used)
                .limit(limit)
                .remaining(remaining)
                .luaBalance(luaBalance)
                .luaCostForExtra(10)
                .canUseExtraWithLua(luaBalance >= 10)
                .message("AI grading available")
                .build();
    }
    
    /**
     * Create a response for when limit is reached but Lúa can be used.
     */
    public static GradingStatusDTO limitReachedWithLua(int used, int limit, int luaBalance) {
        boolean canUse = luaBalance >= 10;
        return GradingStatusDTO.builder()
                .allowed(canUse)
                .used(used)
                .limit(limit)
                .remaining(0)
                .luaBalance(luaBalance)
                .luaCostForExtra(10)
                .canUseExtraWithLua(canUse)
                .message(canUse 
                    ? "Monthly limit reached. Extra grading costs 10 Lúa."
                    : "Monthly limit reached and insufficient Lúa balance.")
                .build();
    }
    
    /**
     * Create a response for free tier users.
     */
    public static GradingStatusDTO freeTierBlocked(int luaBalance) {
        boolean canUse = luaBalance >= 10;
        return GradingStatusDTO.builder()
                .allowed(canUse)
                .used(0)
                .limit(0)
                .remaining(0)
                .luaBalance(luaBalance)
                .luaCostForExtra(10)
                .canUseExtraWithLua(canUse)
                .tierCode("cramerie")
                .message(canUse 
                    ? "Free tier. AI grading costs 10 Lúa per use."
                    : "Free tier. Upgrade to Cramerich for AI grading or purchase Lúa.")
                .build();
    }
}
