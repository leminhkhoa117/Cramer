package com.cramer.billing.service;

import com.cramer.billing.domain.LuaPack;
import com.cramer.billing.repository.LuaPackRepository;
import com.cramer.billing.web.dto.LuaPackView;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lúa pack catalog (SPEC-15 §4). <strong>DB-driven only</strong> — both the credits and payment
 * paths read active {@code lua_packs} rows; there are no hardcoded pack tables (the old
 * hardcoded list is removed).
 */
@Service
@Transactional(readOnly = true)
public class LuaPackService {

    private final LuaPackRepository packs;

    public LuaPackService(LuaPackRepository packs) {
        this.packs = packs;
    }

    public List<LuaPackView> listActive() {
        return packs.findByIsActiveTrueOrderByDisplayOrderAscIdAsc().stream().map(LuaPackView::of).toList();
    }

    /** Resolve an active pack by code (used by purchase + webhook validation). */
    public LuaPack requireActiveByCode(String code) {
        return packs.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> ResourceNotFoundException.of("LuaPack", code));
    }
}
