/**
 * RefinementModal - Collapsible modal for Agent 2 refinement results
 * 
 * Displays near the warnings panel with:
 * - Streaming output from Agent 2
 * - Diff view of proposed changes
 * - Apply/Discard buttons
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
        applyRefinement,
        discardRefinement
    } = useABTSStore();

    const [isCollapsed, setIsCollapsed] = useState(false);

    // Don't render if no refinement activity
    if (!isRefining && !refinementResult) return null;

    const hasError = refinementResult?.error;
    const patches = refinementResult?.patches || [];

    return (
        <div className={`refinement-modal ${isCollapsed ? 'collapsed' : ''}`}>
            <div className="refinement-modal__header" onClick={() => setIsCollapsed(!isCollapsed)}>
                <span className="refinement-modal__title">
                    🔧 Agent 2 - Refinement
                    {isRefining && <span className="refinement-modal__status">Đang xử lý...</span>}
                    {!isRefining && refinementResult && !hasError && (
                        <span className="refinement-modal__status success">
                            ✓ {patches.length} thay đổi
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
                                <span>Đang phân tích và sửa lỗi...</span>
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

                    {/* Error state */}
                    {hasError && (
                        <div className="refinement-modal__error">
                            <span>❌ {refinementResult.error}</span>
                            <button onClick={discardRefinement}>Đóng</button>
                        </div>
                    )}

                    {/* Success state with patches */}
                    {!isRefining && refinementResult && !hasError && (
                        <div className="refinement-modal__result">
                            <div className="refinement-modal__patches">
                                <h4>Thay đổi được đề xuất:</h4>
                                {patches.map((patch, idx) => (
                                    <div key={idx} className="patch-item">
                                        <span className="patch-badge">
                                            Q{patch.questionNumber || '?'}
                                        </span>
                                        <span className="patch-desc">
                                            {patch.description}
                                        </span>
                                    </div>
                                ))}
                                {patches.length === 0 && (
                                    <p className="no-patches">Không có thay đổi nào được thực hiện.</p>
                                )}
                            </div>

                            {/* Show warning if no refinedJson */}
                            {!refinementResult.refinedJson && refinementResult.errorMessage && (
                                <div className="refinement-modal__warning">
                                    ⚠️ {refinementResult.errorMessage}
                                </div>
                            )}

                            <div className="refinement-modal__actions">
                                <button
                                    className="btn-apply"
                                    onClick={applyRefinement}
                                    disabled={!refinementResult.refinedJson || !refinementResult.success}
                                    title={!refinementResult.refinedJson ? 'JSON không hợp lệ' : ''}
                                >
                                    ✓ Áp dụng
                                </button>
                                <button
                                    className="btn-discard"
                                    onClick={discardRefinement}
                                >
                                    ✕ Hủy
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
