/**
 * StreamingDisplay - Real-time ABTS generation workbench.
 *
 * Shows streamed response chunks, compact run metrics, model trace,
 * and recent generation events while AI content is being produced.
 * 
 * @since 2025-12-21 - Cat C Feature
 */

import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
    FiActivity,
    FiAlertTriangle,
    FiCheck,
    FiChevronDown,
    FiChevronRight,
    FiCode,
    FiCpu,
    FiDatabase,
    FiEye,
    FiFileText,
    FiList,
    FiLoader,
    FiStopCircle
} from 'react-icons/fi';
import './StreamingDisplay.css';

const STAGES = {
    CONNECTING: { icon: '~', label: 'Connecting', order: 1 },
    SENDING: { icon: '>', label: 'Sending request', order: 2 },
    CONNECTED: { icon: '+', label: 'Connected', order: 3 },
    STARTED: { icon: '*', label: 'Started', order: 4 },
    PROMPT_BUILT: { icon: '#', label: 'Prompt ready', order: 5 },
    AI_THINKING: { icon: '?', label: 'Model trace', order: 7 },
    AI_CHUNK: { icon: '.', label: 'Receiving output', order: 7 },
    COMPLETED: { icon: '!', label: 'Completed', order: 11 },
    RETRY: { icon: 'R', label: 'Retrying', order: 0 },
    FAILED: { icon: 'X', label: 'Failed', order: -1 },
    TIMEOUT_WARNING: { icon: 'W', label: 'Waiting', order: 6 },
    ABORTED: { icon: 'X', label: 'Aborted', order: -1 },
    PROGRESS: { icon: '.', label: 'Processing', order: 6 }
};

function formatReasoning(text) {
    if (!text || text === 'null') return '';
    const thinkMatch = text.match(/<think>([\s\S]*?)<\/think>/);
    if (thinkMatch) return thinkMatch[1].trim();
    const reasonMatch = text.match(/Reasoning:([\s\S]*?)(?=Output:|$)/i);
    if (reasonMatch) return reasonMatch[1].trim();
    return text.trim();
}

function formatPreviewText(text) {
    if (!text || text === 'null') return '';
    const rawText = typeof text === 'string' ? text : JSON.stringify(text, null, 2);
    return rawText
        .replace(/\\r\\n/g, '\n')
        .replace(/\\n/g, '\n')
        .replace(/\\t/g, '  ')
        .trimStart();
}

function getPreviewType(text) {
    const trimmedText = text.trim();
    if (!trimmedText) return 'Waiting for response';
    if (trimmedText.startsWith('{') || trimmedText.startsWith('[')) return 'JSON stream';
    if (trimmedText.includes('<strong>') || trimmedText.toLowerCase().includes('passage')) return 'Content stream';
    return 'Text stream';
}

function getStatusTone({ error, progress, isActive }) {
    if (error) return 'error';
    if (progress >= 100) return 'complete';
    if (isActive) return 'active';
    return 'idle';
}

export default function StreamingDisplay({
    events = [],
    streamPreview = '',
    streamChunkCount = 0,
    progress = 0,
    isActive = false,
    error = null,
    reasoning = '',
    partErrors = null,
    onAbort,
    onBack
}) {
    const logRef = useRef(null);
    const previewRef = useRef(null);
    const [expandedReasoning, setExpandedReasoning] = useState(false);
    const [displayedReasoning, setDisplayedReasoning] = useState('');

    const safeProgress = Math.min(100, Math.max(0, progress || 0));
    const previewText = useMemo(() => formatPreviewText(streamPreview), [streamPreview]);
    const previewLines = previewText ? previewText.split(/\r\n|\r|\n/).length : 0;
    const previewType = getPreviewType(previewText);
    const statusTone = getStatusTone({ error, progress: safeProgress, isActive });

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            if (logRef.current) {
                logRef.current.scrollTop = logRef.current.scrollHeight;
            }
        }, 50);
        return () => clearTimeout(timeoutId);
    }, [events]);

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            if (previewRef.current) {
                previewRef.current.scrollTop = previewRef.current.scrollHeight;
            }
        }, 40);
        return () => clearTimeout(timeoutId);
    }, [previewText]);

    useEffect(() => {
        if (!reasoning || reasoning === 'null') {
            setDisplayedReasoning('');
            return undefined;
        }

        if (reasoning.length < displayedReasoning.length) {
            setDisplayedReasoning(reasoning);
            return undefined;
        }

        if (reasoning.length > displayedReasoning.length) {
            const timeoutId = setTimeout(() => {
                setDisplayedReasoning(reasoning.slice(0, displayedReasoning.length + 8));
            }, 5);
            return () => clearTimeout(timeoutId);
        }

        return undefined;
    }, [reasoning, displayedReasoning]);

    const currentStage = useMemo(() => {
        if (events.length === 0) return null;
        const lastEvent = events[events.length - 1];
        return STAGES[lastEvent.type] || { icon: '*', label: lastEvent.type, order: 0 };
    }, [events]);

    const deduplicatedEvents = useMemo(() => {
        if (events.length <= 1) return events;

        const result = [];
        let eventIndex = 0;

        while (eventIndex < events.length) {
            const event = events[eventIndex];

            if (event.type === 'PROGRESS' || event.type === 'AI_CHUNK') {
                let duplicateIndex = eventIndex;
                while (
                    duplicateIndex < events.length - 1 &&
                    events[duplicateIndex + 1].type === event.type &&
                    events[duplicateIndex + 1].message === event.message
                ) {
                    duplicateIndex += 1;
                }

                result.push({
                    ...event,
                    progress: events[duplicateIndex].progress
                });
                eventIndex = duplicateIndex + 1;
            } else {
                result.push(event);
                eventIndex += 1;
            }
        }

        return result;
    }, [events]);

    const latestEvent = deduplicatedEvents[deduplicatedEvents.length - 1];
    const visibleEvents = deduplicatedEvents.slice(-12);
    const reasoningText = formatReasoning(displayedReasoning);
    const metrics = [
        { label: 'Progress', value: `${Math.round(safeProgress)}%`, Icon: FiActivity },
        { label: 'Chunks', value: streamChunkCount.toLocaleString(), Icon: FiDatabase },
        { label: 'Preview', value: `${previewText.length.toLocaleString()} ch`, Icon: FiFileText },
        { label: 'Trace', value: deduplicatedEvents.length.toLocaleString(), Icon: FiList }
    ];

    return (
        <div className={`streaming-display streaming-display--${statusTone}`}>
            <div className="streaming-header">
                <div className="streaming-status">
                    {isActive ? (
                        <FiLoader className="spin" size={18} />
                    ) : error ? (
                        <FiAlertTriangle size={18} />
                    ) : progress >= 100 ? (
                        <FiCheck size={18} />
                    ) : (
                        <FiCpu size={18} />
                    )}
                    <span className="streaming-status__copy">
                        <span className="streaming-status__eyebrow">Live generation</span>
                        <strong>
                            {error ? 'Generation error' : currentStage?.label || (isActive ? 'Initializing' : 'Ready')}
                        </strong>
                    </span>
                    <span className="progress-text-header">{Math.round(safeProgress)}%</span>
                </div>

                <div className="streaming-header__actions">
                    <span className={`streaming-live-pill ${isActive ? 'is-active' : ''}`}>
                        <span className="streaming-live-dot" />
                        {isActive ? 'Streaming' : 'Idle'}
                    </span>
                    {onAbort && isActive && (
                        <button className="abort-btn" onClick={onAbort} title="Stop generation" type="button">
                            <FiStopCircle />
                            <span>Stop</span>
                        </button>
                    )}
                </div>
            </div>

            <div
                className="streaming-progress"
                role="progressbar"
                aria-valuemin="0"
                aria-valuemax="100"
                aria-valuenow={Math.round(safeProgress)}
            >
                <div className="progress-fill" style={{ width: `${safeProgress}%` }} />
            </div>

            <div className="streaming-workbench">
                <section className="streaming-panel streaming-panel--preview" aria-label="Live response preview">
                    <div className="streaming-panel__header">
                        <div className="streaming-panel__title">
                            <FiEye size={15} />
                            <div>
                                <strong>Live response</strong>
                                <span>{previewType}</span>
                            </div>
                        </div>
                        <div className="streaming-panel__stats" aria-label="Preview metrics">
                            <span>{previewText.length.toLocaleString()} chars</span>
                            <span>{previewLines.toLocaleString()} lines</span>
                        </div>
                    </div>

                    <div className={`streaming-preview ${previewText ? '' : 'streaming-preview--empty'}`} ref={previewRef}>
                        {previewText ? (
                            <pre>
                                {previewText}
                                {isActive && <span className="typing-cursor" />}
                            </pre>
                        ) : (
                            <div className="streaming-empty-state">
                                <FiCode size={20} />
                                <strong>Awaiting response stream</strong>
                                <span>{currentStage?.label || 'Connection not started'}</span>
                            </div>
                        )}
                    </div>
                </section>

                <aside className="streaming-inspector" aria-label="Generation inspector">
                    <section className="streaming-stats-grid" aria-label="Run metrics">
                        {metrics.map(({ label, value, Icon }) => (
                            <div className="streaming-stat" key={label}>
                                <Icon />
                                <span>{label}</span>
                                <strong>{value}</strong>
                            </div>
                        ))}
                    </section>

                    <section className={`reasoning-section ${reasoningText ? '' : 'reasoning-section--empty'}`}>
                        <button
                            className="reasoning-header"
                            onClick={() => setExpandedReasoning(!expandedReasoning)}
                            type="button"
                            aria-expanded={expandedReasoning}
                        >
                            <span className="reasoning-header__title">
                                <FiCpu size={14} />
                                <span>Model trace</span>
                            </span>
                            <span className="reasoning-header__meta">
                                {reasoningText ? `${reasoningText.length.toLocaleString()} chars` : 'empty'}
                                {expandedReasoning ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}
                            </span>
                        </button>

                        {expandedReasoning && (
                            <div className="reasoning-content expanded">
                                {reasoningText ? (
                                    <pre>
                                        {reasoningText}
                                        {isActive && <span className="typing-cursor" />}
                                    </pre>
                                ) : (
                                    <span>No reasoning tokens received yet.</span>
                                )}
                            </div>
                        )}
                    </section>

                    <section className="streaming-panel streaming-panel--trace" aria-label="Recent generation events">
                        <div className="streaming-panel__header streaming-panel__header--compact">
                            <div className="streaming-panel__title">
                                <FiList size={15} />
                                <div>
                                    <strong>Trace</strong>
                                    <span>{latestEvent?.message || currentStage?.label || 'No events yet'}</span>
                                </div>
                            </div>
                        </div>

                        <div className="streaming-log" ref={logRef}>
                            {visibleEvents.length > 0 ? visibleEvents.map((event, index) => {
                                const stage = STAGES[event.type] || { icon: '*' };
                                const typeClass = `type-${(event.type || 'unknown').toLowerCase()}`;
                                const hasErrors = event.type === 'VALIDATION_RESULT' && Array.isArray(event.data) && event.data.length > 0;

                                return (
                                    <div key={`${event.type}-${index}`} className={`log-entry ${typeClass}`}>
                                        <span className="log-icon">{stage.icon}</span>
                                        <span className="log-message">{event.message || event.type}</span>
                                        {event.progress != null && <span className="log-progress">{event.progress}%</span>}
                                        {hasErrors && (
                                            <div className="log-errors">
                                                {event.data.slice(0, 3).map((validationError, errorIndex) => (
                                                    <div key={errorIndex}>- {validationError}</div>
                                                ))}
                                                {event.data.length > 3 && <div>...and {event.data.length - 3} more</div>}
                                            </div>
                                        )}
                                    </div>
                                );
                            }) : (
                                <div className="log-entry type-connecting">
                                    <span className="log-icon">~</span>
                                    <span className="log-message">Establishing connection to AI...</span>
                                </div>
                            )}
                        </div>
                    </section>
                </aside>
            </div>

            {error && (
                <div className="streaming-error">
                    <FiAlertTriangle size={16} />
                    <span>{error}</span>
                </div>
            )}

            {partErrors && typeof partErrors === 'object' && Object.keys(partErrors).length > 0 && (
                <div className="streaming-error streaming-error--partial">
                    <FiAlertTriangle size={16} />
                    <div>
                        <strong>Some parts could not be generated:</strong>
                        <ul>
                            {Object.entries(partErrors).map(([part, msg]) => (
                                <li key={part}>Part {part}: {String(msg)}</li>
                            ))}
                        </ul>
                    </div>
                </div>
            )}

            {!isActive && onBack && (
                <div className="streaming-footer-actions">
                    <button className="abort-btn abort-btn--secondary" onClick={onBack} type="button">
                        Back to Configure
                    </button>
                </div>
            )}
        </div>
    );
}
