/**
 * RefinementModal - Loop controller for Agent 2 refinement
 *
 * Displays near the warnings panel with:
 * - Streaming output from Agent 2
 * - Round counter + proposed-change summary (per-hunk review lives in the Issue Rail Diff view)
 * - A single Done control (Apply Accepted / Refine again live in the Issue Rail)
 *
 * @since 2026-01-04
 */
import React, { useState } from 'react';
import useABTSStore from '../../stores/useABTSStore';
import './RefinementModal.css';

const RefinementModal = () => {
    const {
        isRefining,
        refinementResult,
        refinementStream,
        refinement,
        abtsStatus,
        closeRefinement
    } = useABTSStore();

    const [isCollapsed, setIsCollapsed] = useState(false);

    const hunks = refinement?.hunks || [];
    const acceptedHunkIds = refinement?.acceptedHunkIds || [];
    const round = refinement?.round || 0;
    const hasHunks = hunks.length > 0;

    // Don't render if no refinement activity
    if (!isRefining && !refinementResult && !hasHunks) return null;

    const hasError = refinementResult?.error;
    // FIX 11: round cap comes from the backend status (config.maxRefinementRounds)
    // instead of a hardcoded 5, so server + UI stay in sync.
    const maxRounds = abtsStatus?.maxRefinementRounds || 5;
    const atLimit = round >= maxRounds;
    // FIX 10: refinement finished but proposed nothing for the selected issues.
    const isEmptyResult = !isRefining && !hasHunks && !!refinementResult && !hasError;

    return (
        <div className={`refinement-modal ${isCollapsed ? 'collapsed' : ''}`}>
            <div className="refinement-modal__header" onClick={() => setIsCollapsed(!isCollapsed)}>
                <span className="refinement-modal__title">
                    🔧 Refinement — Round {round || 1}
                    {isRefining && <span className="refinement-modal__status">Processing…</span>}
                    {!isRefining && hasHunks && (
                        <span className="refinement-modal__status success">
                            {hunks.length} change{hunks.length > 1 ? 's' : ''} proposed
                        </span>
                    )}
                </span>
                <button className="refinement-modal__toggle">
                    {isCollapsed ? '▼' : '▲'}
                </button>
            </div>

            {!isCollapsed && (
                <div className="refinement-modal__content">
                    {/* Streaming output */}
                    {isRefining && (
                        <div className="refinement-modal__streaming">
                            <div className="refinement-modal__progress">
                                <span className="spinner"></span>
                                <span>Analyzing and proposing fixes…</span>
                            </div>
                            {refinementStream.length > 0 && (
                                <div className="refinement-modal__log">
                                    {refinementStream
                                        .filter(evt => evt.type !== 'AI_CHUNK' && evt.type !== 'AI_THINKING')
                                        .slice(-5)
                                        .map((evt, idx) => (
                                            <div key={idx} className="log-entry">
                                                {evt.message || evt.type}
                                            </div>
                                        ))}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Error / limit state */}
                    {hasError && (
                        <div className="refinement-modal__error">
                            <span>❌ {refinementResult.error}</span>
                            <button onClick={closeRefinement}>Close</button>
                        </div>
                    )}

                    {/* Ready state — hunks proposed, awaiting per-hunk review in the Issue Rail */}
                    {!isRefining && hasHunks && (
                        <div className="refinement-modal__result">
                            <p className="refinement-modal__hint">
                                Review and toggle each change in the <strong>Issue Rail → Diff</strong> view,
                                then click <strong>Apply Accepted</strong> in the rail to commit them.
                            </p>
                            <div className="refinement-modal__summary">
                                <span className="refinement-modal__count">
                                    {acceptedHunkIds.length} of {hunks.length} accepted
                                </span>
                                {atLimit && (
                                    <span className="refinement-modal__limit">Refinement limit reached ({maxRounds} rounds)</span>
                                )}
                            </div>

                            <div className="refinement-modal__actions">
                                <button
                                    className="btn-discard"
                                    onClick={closeRefinement}
                                >
                                    Done
                                </button>
                            </div>
                        </div>
                    )}

                    {/* FIX 10: empty result — refinement ran but proposed no changes */}
                    {isEmptyResult && (
                        <div className="refinement-modal__result">
                            <p className="refinement-modal__hint">
                                No changes proposed for the selected issues. Select different issues
                                and use <strong>Refine again</strong> in the Issue Rail, or close this panel.
                            </p>
                            <div className="refinement-modal__actions">
                                <button
                                    className="btn-discard"
                                    onClick={closeRefinement}
                                >
                                    Done
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default RefinementModal;
