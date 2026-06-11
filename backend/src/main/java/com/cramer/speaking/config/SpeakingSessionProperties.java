package com.cramer.speaking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Speaking session tuning (SPEC-14 §2 defaults). Bound from {@code speaking.session.*}.
 *
 * @param luaCost          Lúa cost reserved per session (default 15)
 * @param luaCheckOnCreate check affordability at create time (default true)
 * @param chargeOnComplete deduct Lúa on completion (default true)
 * @param part1SelectMin   min Part 1 turns to select
 * @param part1SelectMax   max Part 1 turns to select
 * @param part3SelectMin   min Part 3 turns to select
 * @param part3SelectMax   max Part 3 turns to select
 */
@ConfigurationProperties(prefix = "speaking.session")
public record SpeakingSessionProperties(
        Integer luaCost,
        Boolean luaCheckOnCreate,
        Boolean chargeOnComplete,
        Integer part1SelectMin,
        Integer part1SelectMax,
        Integer part3SelectMin,
        Integer part3SelectMax) {

    public int resolvedLuaCost() {
        return luaCost == null ? 15 : luaCost;
    }

    public boolean resolvedCheckOnCreate() {
        return luaCheckOnCreate == null || luaCheckOnCreate;
    }

    public boolean resolvedChargeOnComplete() {
        return chargeOnComplete == null || chargeOnComplete;
    }

    public int p1Min() {
        return part1SelectMin == null ? 8 : part1SelectMin;
    }

    public int p1Max() {
        return part1SelectMax == null ? 12 : part1SelectMax;
    }

    public int p3Min() {
        return part3SelectMin == null ? 3 : part3SelectMin;
    }

    public int p3Max() {
        return part3SelectMax == null ? 6 : part3SelectMax;
    }
}
