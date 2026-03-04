package com.cramer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Entity representing a Lúa (virtual currency) pack available for purchase.
 * Packs are defined in the database and can be managed dynamically.
 */
@Entity
@Table(name = "lua_packs", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuaPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // lua_100, lua_500, lua_2000

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "emoji", length = 10)
    @Builder.Default
    private String emoji = "🌾";

    @Column(name = "lua_amount", nullable = false)
    private Integer luaAmount;

    @Column(name = "price_vnd", nullable = false)
    private Integer priceVnd;

    @Column(name = "discount_percent", nullable = false)
    @Builder.Default
    private Integer discountPercent = 0;

    @Column(name = "bonus_lua", nullable = false)
    @Builder.Default
    private Integer bonusLua = 0;

    @Column(name = "description_vi")
    private String descriptionVi;

    @Column(name = "description_en")
    private String descriptionEn;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Get total Lúa including bonus.
     */
    public int getTotalLua() {
        return luaAmount + bonusLua;
    }

    /**
     * Calculate effective price per 100 Lúa.
     */
    public int getPricePer100Lua() {
        if (luaAmount == 0)
            return 0;
        return (int) Math.round((double) priceVnd / luaAmount * 100);
    }
}
