/**
 * HunkDiffView - Per-hunk diff cards with Apply/Reject approval.
 *
 * Renders each refinement hunk as a dark-theme card showing a before/after
 * JSON diff (inline, no external diff dependency) plus per-card Apply/Reject.
 *
 * @since 2026-01-07
 */
import React from 'react';
import { FiCheck, FiX } from 'react-icons/fi';
import './HunkDiffView.css';

const toLines = (value) => {
    if (value === undefined || value === null) return [];
    const text = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
    return String(text).split('\n');
};

const HunkDiffView = ({ hunks = [], acceptedHunkIds = [], onAccept, onReject }) => {
    if (!hunks.length) {
        return <div className="hunk-diff__empty">No proposed changes to review.</div>;
    }

    return (
        <div className="hunk-diff">
            {hunks.map((hunk) => {
                const accepted = acceptedHunkIds.includes(hunk.id);
                const beforeLines = toLines(hunk.before);
                const afterLines = toLines(hunk.after);
                return (
                    <article
                        key={hunk.id}
                        className={`hunk-diff__card ${accepted ? 'hunk-diff__card--accepted' : 'hunk-diff__card--rejected'}`}
                    >
                        <header className="hunk-diff__head">
                            <span className={`hunk-diff__op hunk-diff__op--${(hunk.op || 'replace').toLowerCase()}`}>
                                {hunk.op || 'replace'}
                            </span>
                            <code className="hunk-diff__path" title={hunk.path}>{hunk.path}</code>
                            {hunk.severity && (
                                <span className={`hunk-diff__sev hunk-diff__sev--${String(hunk.severity).toLowerCase()}`}>
                                    {hunk.severity}
                                </span>
                            )}
                        </header>

                        {hunk.summary && <p className="hunk-diff__summary">{hunk.summary}</p>}

                        <div className="hunk-diff__body">
                            {beforeLines.map((line, i) => (
                                <div key={`b-${i}`} className="hunk-diff__line hunk-diff__line--del">
                                    <span className="hunk-diff__gutter">-</span>{line}
                                </div>
                            ))}
                            {afterLines.map((line, i) => (
                                <div key={`a-${i}`} className="hunk-diff__line hunk-diff__line--add">
                                    <span className="hunk-diff__gutter">+</span>{line}
                                </div>
                            ))}
                        </div>

                        <footer className="hunk-diff__actions">
                            <button
                                type="button"
                                className={`studio-btn studio-btn--sm ${accepted ? 'studio-btn--primary' : 'studio-btn--ghost'}`}
                                onClick={() => onAccept?.(hunk.id)}
                                aria-pressed={accepted}
                            >
                                <FiCheck size={13} /> Apply
                            </button>
                            <button
                                type="button"
                                className={`studio-btn studio-btn--sm ${!accepted ? 'studio-btn--danger' : 'studio-btn--ghost'}`}
                                onClick={() => onReject?.(hunk.id)}
                                aria-pressed={!accepted}
                            >
                                <FiX size={13} /> Reject
                            </button>
                        </footer>
                    </article>
                );
            })}
        </div>
    );
};

export default HunkDiffView;
