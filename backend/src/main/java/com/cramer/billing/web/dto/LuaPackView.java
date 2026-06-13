package com.cramer.billing.web.dto;

import com.cramer.billing.domain.LuaPack;

/** Outbound Lúa pack projection (SPEC-15 §4). {@code totalLua = lua_amount + bonus_lua}. */
public record LuaPackView(
        Long id,
        String code,
        String name,
        String emoji,
        int luaAmount,
        int priceVnd,
        int discountPercent,
        int bonusLua,
        int totalLua,
        String descriptionVi,
        String descriptionEn,
        Integer displayOrder) {

    public static LuaPackView of(LuaPack p) {
        return new LuaPackView(
                p.getId(), p.getCode(), p.getName(), p.getEmoji(), p.getLuaAmount(), p.getPriceVnd(),
                p.getDiscountPercent(), p.getBonusLua(), p.totalLua(), p.getDescriptionVi(),
                p.getDescriptionEn(), p.getDisplayOrder());
    }
}
