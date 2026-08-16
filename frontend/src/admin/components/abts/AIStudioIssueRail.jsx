import React, { useMemo, useState } from 'react';
import { FiAlertCircle, FiAlertTriangle, FiCheckCircle, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import IssueSelector from './IssueSelector';
import HunkDiffView from './HunkDiffView';
import useABTSStore from '../../stores/useABTSStore';
import { getAIStudioIssueCounts, getAIStudioValidationBuckets } from './aiStudioStatus';

export default function AIStudioIssueRail({ generationResult, isGenerating, isCollapsed: controlledCollapsed, onCollapsedChange }) {
    const [internalCollapsed, setInternalCollapsed] = useState(false);
    const [railView, setRailView] = useState('issues');
    const isCollapsed = controlledCollapsed ?? internalCollapsed;
    const buckets = useMemo(() => getAIStudioValidationBuckets(generationResult), [generationResult]);
    const counts = useMemo(() => getAIStudioIssueCounts(generationResult), [generationResult]);
    const hasResult = Boolean(generationResult?.content);

    // Loopable refinement state
    const refinement = useABTSStore((state) => state.refinement);
    const isRefining = useABTSStore((state) => state.isRefining);
    const abtsStatus = useABTSStore((state) => state.abtsStatus);
    const acceptHunk = useABTSStore((state) => state.acceptHunk);
    const rejectHunk = useABTSStore((state) => state.rejectHunk);
    const applyAcceptedHunks = useABTSStore((state) => state.applyAcceptedHunks);
    const refineAgain = useABTSStore((state) => state.refineAgain);

    const hunks = refinement?.hunks || [];
    const acceptedHunkIds = refinement?.acceptedHunkIds || [];
    const round = refinement?.round || 0;
    const isApplying = refinement?.isApplying || false;
    const isLooping = refinement?.isLooping || false;
    const skippedHunks = refinement?.lastSkippedHunks || []; // FIX 9
    const hasHunks = hunks.length > 0;
    const activeView = hasHunks ? railView : 'issues';
    // FIX 11: round cap from backend status instead of hardcoded 5.
    const maxRounds = abtsStatus?.maxRefinementRounds || 5;
    const atLimit = round >= maxRounds;

    const errorGroups = [
        { key: 'contentErrors', title: 'Content Errors', issues: buckets.contentErrors },
    ];
    const toggleCollapsed = () => {
        const nextCollapsed = !isCollapsed;
        if (onCollapsedChange) {
            onCollapsedChange(nextCollapsed);
        } else {
            setInternalCollapsed(nextCollapsed);
        }
    };

    return (
        <aside
            className={`ai-studio-issue-rail ${isCollapsed ? 'ai-studio-issue-rail--collapsed' : ''}`}
            aria-label="Validation issues"
            data-state={isCollapsed ? 'collapsed' : 'expanded'}
        >
            <div className="ai-studio-issue-rail__header">
                <div className="ai-studio-issue-rail__heading">
                    <p className="ai-studio-issue-rail__eyebrow">Validation</p>
                    <h2 className="ai-studio-issue-rail__title">
                        {counts.total > 0 ? `${counts.total} issue${counts.total > 1 ? 's' : ''}` : 'Issue summary'}
                        {round > 0 && (
                            <span className="ai-studio-issue-rail__round-pill" title={`Refinement round ${round} of ${maxRounds}`}>
                                Round {round} / {maxRounds}
                            </span>
                        )}
                    </h2>
                </div>
                <button
                    type="button"
                    className="studio-btn studio-btn--ghost studio-btn--icon ai-studio-issue-rail__toggle"
                    onClick={toggleCollapsed}
                    aria-expanded={!isCollapsed}
                    title={isCollapsed ? 'Expand validation rail' : 'Collapse validation rail'}
                >
                    {isCollapsed ? <FiChevronLeft size={16} /> : <FiChevronRight size={16} />}
                </button>
            </div>

            <div className="ai-studio-issue-rail__counts">
                <span
                    className="ai-studio-issue-rail__count ai-studio-issue-rail__count--error"
                    aria-label={`${counts.errorCount} validation errors`}
                    title={`${counts.errorCount} validation errors`}
                >
                    <FiAlertCircle size={13} />
                    <span className="ai-studio-issue-rail__count-text">{counts.errorCount} errors</span>
                    <span className="ai-studio-issue-rail__count-value" aria-hidden="true">{counts.errorCount}</span>
                </span>
                <span
                    className="ai-studio-issue-rail__count ai-studio-issue-rail__count--warning"
                    aria-label={`${counts.warningCount} validation warnings`}
                    title={`${counts.warningCount} validation warnings`}
                >
                    <FiAlertTriangle size={13} />
                    <span className="ai-studio-issue-rail__count-text">{counts.warningCount} warnings</span>
                    <span className="ai-studio-issue-rail__count-value" aria-hidden="true">{counts.warningCount}</span>
                </span>
            </div>

            {!isCollapsed && (
                <div className="ai-studio-issue-rail__body">
                    {hasHunks && (
                        <div className="ai-studio-issue-rail__viewbar">
                            <div className="ai-studio-issue-rail__viewtoggle" role="tablist" aria-label="Rail view">
                                <button
                                    type="button"
                                    role="tab"
                                    aria-selected={activeView === 'issues'}
                                    className={`ai-studio-issue-rail__viewtab ${activeView === 'issues' ? 'is-active' : ''}`}
                                    onClick={() => setRailView('issues')}
                                >
                                    Issues
                                </button>
                                <button
                                    type="button"
                                    role="tab"
                                    aria-selected={activeView === 'diff'}
                                    className={`ai-studio-issue-rail__viewtab ${activeView === 'diff' ? 'is-active' : ''}`}
                                    onClick={() => setRailView('diff')}
                                >
                                    Diff
                                </button>
                            </div>
                            <span className="ai-studio-issue-rail__accepted-badge" title="Accepted hunks">
                                {acceptedHunkIds.length} of {hunks.length} accepted
                            </span>
                        </div>
                    )}

                    {activeView === 'diff' ? (
                        <HunkDiffView
                            hunks={hunks}
                            acceptedHunkIds={acceptedHunkIds}
                            onAccept={acceptHunk}
                            onReject={rejectHunk}
                        />
                    ) : (
                        <>
                            {!hasResult && (
                                <div className="ai-studio-issue-rail__empty">
                                    {isGenerating ? 'Validation will update when generation completes.' : 'No generated content has been validated yet.'}
                                </div>
                            )}

                            {hasResult && counts.total === 0 && (
                                <div className="ai-studio-issue-rail__success">
                                    <FiCheckCircle size={16} />
                                    <span>No validation issues reported.</span>
                                </div>
                            )}

                            {errorGroups.map(group => group.issues.length > 0 && (
                                <section key={group.key} className="ai-studio-issue-rail__section">
                                    <h3 className="ai-studio-issue-rail__section-title">{group.title}</h3>
                                    <ul className="ai-studio-issue-rail__list">
                                        {group.issues.map(issue => (
                                            <li key={issue.id}>{issue.message}</li>
                                        ))}
                                    </ul>
                                </section>
                            ))}

                            {buckets.warnings.length > 0 && (
                                <section className="ai-studio-issue-rail__section ai-studio-issue-rail__section--warning">
                                    <h3 className="ai-studio-issue-rail__section-title">Warnings</h3>
                                    <IssueSelector issues={buckets.warnings} type="warning" />
                                </section>
                            )}
                        </>
                    )}

                    {/* FIX 9: hunks the backend skipped during apply, with reasons. */}
                    {skippedHunks.length > 0 && (
                        <section className="ai-studio-issue-rail__section ai-studio-issue-rail__section--warning">
                            <h3 className="ai-studio-issue-rail__section-title">
                                {skippedHunks.length} change{skippedHunks.length > 1 ? 's' : ''} skipped
                            </h3>
                            <ul className="ai-studio-issue-rail__list">
                                {skippedHunks.map((sk) => (
                                    <li key={sk.id}>
                                        <code>{sk.id}</code>{sk.reason ? ` — ${sk.reason}` : ''}
                                    </li>
                                ))}
                            </ul>
                        </section>
                    )}

                    {hasHunks && (
                        <div className="ai-studio-issue-rail__footer">
                            <button
                                type="button"
                                className="studio-btn studio-btn--primary studio-btn--sm"
                                onClick={applyAcceptedHunks}
                                disabled={acceptedHunkIds.length === 0 || isApplying}
                            >
                                {isApplying
                                    ? (<><span className="ai-studio-issue-rail__spinner" aria-hidden="true" /> Applying…</>)
                                    : `Apply Accepted (${acceptedHunkIds.length})`}
                            </button>
                            <button
                                type="button"
                                className="studio-btn studio-btn--ghost studio-btn--sm"
                                onClick={refineAgain}
                                disabled={atLimit || isRefining || isLooping || isApplying}
                                title={atLimit ? `Refinement limit reached (${maxRounds} rounds)` : 'Run another refinement round'}
                            >
                                Refine again
                            </button>
                        </div>
                    )}
                </div>
            )}
        </aside>
    );
}