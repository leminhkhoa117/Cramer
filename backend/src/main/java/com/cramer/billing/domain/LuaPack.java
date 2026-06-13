package com.cramer.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * A purchasable Lúa pack, table {@code lua_packs} (SPEC-15 §4). Packs are DB-driven; the total
 * granted on purchase is {@code lua_amount + bonus_lua}.
 */
@Entity
@Table(name = "lua_packs", schema = "public")
@Getter
@Setter
public class LuaPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "emoji")
    private String emoji;

    @Column(name = "lua_amount", nullable = false)
    private Integer luaAmount;

    @Column(name = "price_vnd", nullable = false)
    private Integer priceVnd;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent = 0;

    @Column(name = "bonus_lua", nullable = false)
    private Integer bonusLua = 0;

    @Column(name = "description_vi")
    private String descriptionVi;

    @Column(name = "description_en")
    private String descriptionEn;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /** Total Lúa granted on purchase (SPEC-15 §4). */
    public int totalLua() {
        return luaAmount + bonusLua;
    }
}
