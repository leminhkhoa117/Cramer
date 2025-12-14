package com.cramer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for billing result from quota check.
 * Returned by QuotaBillingService.processAttemptBilling().
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingResultDTO {

    /**
     * Whether the attempt is allowed to proceed.
     */
    private boolean allowed;

    /**
     * Amount of Lua charged (0 if within free quota or premium).
     */
    private int luaCharged;

    /**
     * Reason message (Vietnamese for UI display).
     * - null if allowed
     * - Error message if blocked
     */
    private String reason;

    /**
     * Type of block encountered (null if allowed).
     * - "global" - Global quota exceeded
     * - "local" - Per-skill quota exceeded
     * - "insufficient_lua" - Not enough Lua to pay overage
     */
    private String blockType;

    // ===== FACTORY METHODS =====

    /**
     * Allowed without charge (within free quota or premium user).
     */
    public static BillingResultDTO allowed() {
        return BillingResultDTO.builder()
                .allowed(true)
                .luaCharged(0)
                .reason(null)
                .blockType(null)
                .build();
    }

    /**
     * Allowed with Lua charge (exceeded quota, paid overage).
     */
    public static BillingResultDTO charged(int luaAmount) {
        return BillingResultDTO.builder()
                .allowed(true)
                .luaCharged(luaAmount)
                .reason(null)
                .blockType(null)
                .build();
    }

    /**
     * Blocked due to global quota and insufficient Lua.
     */
    public static BillingResultDTO blockedGlobal(int requiredLua) {
        return BillingResultDTO.builder()
                .allowed(false)
                .luaCharged(0)
                .reason("Bạn đã hết quota tháng này. Cần " + requiredLua + " Lua để tiếp tục.")
                .blockType("global")
                .build();
    }

    /**
     * Blocked due to local (per-skill) quota and insufficient Lua.
     */
    public static BillingResultDTO blockedLocal(String skill, int requiredLua) {
        return BillingResultDTO.builder()
                .allowed(false)
                .luaCharged(0)
                .reason("Bạn đã hết quota " + skill + " tháng này. Cần " + requiredLua + " Lua để tiếp tục.")
                .blockType("local")
                .build();
    }

    /**
     * Blocked due to insufficient Lua to pay overage.
     */
    public static BillingResultDTO insufficientLua(int requiredLua) {
        return BillingResultDTO.builder()
                .allowed(false)
                .luaCharged(0)
                .reason("Không đủ Lua. Cần " + requiredLua + " Lua để tiếp tục.")
                .blockType("insufficient_lua")
                .build();
    }
}
