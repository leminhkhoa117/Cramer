package com.cramer.dto;

import com.cramer.entity.LuaPack;
import lombok.*;

/**
 * DTO for Lúa pack information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuaPackDTO {

    private Long id;
    private String code;
    private String nameVi;
    private String nameEn;
    private String emoji;
    private Integer luaAmount;
    private Integer priceVnd;
    private Integer discountPercent;
    private Integer bonusLua;
    private Integer totalLua;
    private Integer pricePer100Lua;
    private String descriptionVi;
    private String descriptionEn;
    private Integer displayOrder;

    /**
     * Create a DTO from an entity.
     */
    public static LuaPackDTO fromEntity(LuaPack entity) {
        if (entity == null) return null;

        return LuaPackDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nameVi(entity.getNameVi())
                .nameEn(entity.getNameEn())
                .emoji(entity.getEmoji())
                .luaAmount(entity.getLuaAmount())
                .priceVnd(entity.getPriceVnd())
                .discountPercent(entity.getDiscountPercent())
                .bonusLua(entity.getBonusLua())
                .totalLua(entity.getTotalLua())
                .pricePer100Lua(entity.getPricePer100Lua())
                .descriptionVi(entity.getDescriptionVi())
                .descriptionEn(entity.getDescriptionEn())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
