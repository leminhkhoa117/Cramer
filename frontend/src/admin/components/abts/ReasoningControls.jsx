import React, { useEffect } from 'react';
import { FiCpu } from 'react-icons/fi';
import './ReasoningControls.css';

/**
 * Capability-driven reasoning knobs.
 *
 * Renders only the controls a model actually supports, inferred from its
 * {@code knobType} capability descriptor. The parent owns the value; this
 * component emits partial patches via {@code onChange}.
 *
 * @param {Object|null} capabilities - model capability descriptor
 *   ({ knobType, validEfforts, budgetRange:{min,max}, ... }) or null when unknown.
 * @param {Object} value - { enableReasoning, reasoningEffort, reasoningBudget }
 * @param {(patch: Object) => void} onChange - receives a partial form patch.
 * @since 2026 - ABTS PART C (Model picker overhaul)
 */

// knobTypes that expose a token-budget slider + numeric input.
const BUDGET_KNOBS = new Set(['ANTHROPIC_BUDGET', 'GEMINI_BUDGET', 'DEEPSEEK_TOGGLE']);
// knobTypes that expose a plain on/off thinking toggle only.
const TOGGLE_ONLY_KNOBS = new Set(['QWEN_THINKING', 'GLM_THINKING', 'DEEPSEEK_TOGGLE']);

const BUDGET_STEP = 256;
// Common thinking-budget presets in tokens; filtered by the model's [min,max] at render time.
const BUDGET_PRESETS = [
    { label: '2K', value: 2048 },
    { label: '4K', value: 4096 },
    { label: '8K', value: 8192 },
    { label: '16K', value: 16384 },
    { label: '32K', value: 32768 }
];

function formatBudget(n) {
    if (n >= 1024 && n % 1024 === 0) return `${n / 1024}K`;
    return n.toLocaleString();
}

function effectiveBudgetRange(capabilities) {
    const range = capabilities?.budgetRange || {};
    const min = Number.isFinite(range.min) ? range.min : 1024;
    const max = Number.isFinite(range.max) ? range.max : 32768;
    return { min, max: Math.max(max, min) };
}

function clamp(value, min, max) {
    if (!Number.isFinite(value)) return min;
    return Math.min(Math.max(value, min), max);
}

export default function ReasoningControls({ capabilities, value, onChange }) {
    const knobType = capabilities?.knobType || null;
    const validEfforts = capabilities?.validEfforts || [];
    const enableReasoning = value?.enableReasoning ?? false;
    const reasoningEffort = value?.reasoningEffort ?? null;
    const reasoningBudget = value?.reasoningBudget ?? null;

    const isEffortKnob = knobType === 'EFFORT_LOW_MED_HIGH';
    const isBudgetKnob = BUDGET_KNOBS.has(knobType);
    const isToggleOnly = TOGGLE_ONLY_KNOBS.has(knobType) && !isBudgetKnob;
    const isUnsupported = !knobType || knobType === 'NONE' || knobType === 'KIMI_NONE';

    const { min: budgetMin, max: budgetMax } = effectiveBudgetRange(capabilities);

    // When the model (and thus its capabilities) changes, reconcile stale values:
    // reset effort to a supported value and clamp budget into the new range.
    useEffect(() => {
        if (!capabilities) return;
        const patch = {};

        if (isEffortKnob && validEfforts.length > 0
            && (!reasoningEffort || !validEfforts.includes(reasoningEffort))) {
            patch.reasoningEffort = validEfforts.includes('medium') ? 'medium' : validEfforts[0];
        }

        if (isBudgetKnob && reasoningBudget != null) {
            const clamped = clamp(reasoningBudget, budgetMin, budgetMax);
            if (clamped !== reasoningBudget) {
                patch.reasoningBudget = clamped;
            }
        }

        // Clear a now-irrelevant explicit budget when the model doesn't use one.
        if (!isBudgetKnob && reasoningBudget != null) {
            patch.reasoningBudget = null;
        }

        // FIX 6: clear a stale explicit effort when the model has no effort knob,
        // otherwise an unsupported effort silently rides along to the backend.
        if (!isEffortKnob && reasoningEffort != null) {
            patch.reasoningEffort = null;
        }

        if (Object.keys(patch).length > 0) {
            onChange(patch);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [capabilities?.id, knobType]);

    if (isUnsupported) {
        return (
            <div className="reasoning-controls reasoning-controls--muted">
                <FiCpu size={12} />
                <span>This model does not support extended thinking.</span>
            </div>
        );
    }

    const toggle = (
        <button
            type="button"
            className={'studio-toggle' + (enableReasoning ? ' is-active' : '')}
            onClick={() => onChange({ enableReasoning: !enableReasoning })}
            aria-pressed={enableReasoning}
        ><span /></button>
    );

    const budgetValue = clamp(reasoningBudget ?? budgetMin, budgetMin, budgetMax);

    return (
        <div className="reasoning-controls">
            <div className="reasoning-controls__row">
                <span className="reasoning-controls__label"><FiCpu size={11} /> Thinking</span>
                {toggle}
            </div>

            {isEffortKnob && enableReasoning && validEfforts.length > 0 && (
                <div className="reasoning-controls__row">
                    <span className="reasoning-controls__label">Effort</span>
                    <select
                        className="studio-input reasoning-controls__select"
                        value={reasoningEffort || ''}
                        onChange={(event) => onChange({ reasoningEffort: event.target.value })}
                    >
                        {validEfforts.map((effort) => (
                            <option key={effort} value={effort}>
                                {effort.charAt(0).toUpperCase() + effort.slice(1)}
                            </option>
                        ))}
                    </select>
                </div>
            )}

            {isBudgetKnob && enableReasoning && (
                <div className="reasoning-controls__budget">
                    <span className="reasoning-controls__label">
                        Budget <strong>{formatBudget(budgetValue)}</strong>
                        <span className="reasoning-controls__sublabel">
                            ({budgetValue.toLocaleString()} tokens)
                        </span>
                    </span>
                    <div className="reasoning-controls__presets" role="group" aria-label="Thinking budget presets">
                        {BUDGET_PRESETS
                            .filter((p) => p.value >= budgetMin && p.value <= budgetMax)
                            .map((p) => {
                                const active = budgetValue === p.value;
                                return (
                                    <button
                                        key={p.value}
                                        type="button"
                                        className={'reasoning-controls__chip' + (active ? ' is-active' : '')}
                                        aria-pressed={active}
                                        onClick={() => onChange({ reasoningBudget: p.value })}
                                    >{p.label}</button>
                                );
                            })}
                    </div>
                    <input
                        type="range"
                        className="studio-range"
                        aria-label="Thinking budget"
                        min={budgetMin}
                        max={budgetMax}
                        step={BUDGET_STEP}
                        value={budgetValue}
                        onChange={(event) => onChange({ reasoningBudget: parseInt(event.target.value, 10) })}
                    />
                </div>
            )}

            {isToggleOnly && !isBudgetKnob && (
                <p className="reasoning-controls__hint">Thinking on/off only — no effort or budget knob for this model.</p>
            )}
        </div>
    );
}
