/**
 * StreamingDisplay - Real-time AI thinking visualization.
 * 
 * Shows the AI's reasoning process as it generates content,
 * creating a terminal-like experience with streaming text.
 * 
 * @since 2025-12-21 - Cat C Feature
 */

import React, { useEffect, useRef, useState } from 'react';
import { FiCpu, FiCheck, FiLoader, FiAlertTriangle, FiStopCircle } from 'react-icons/fi';
import './StreamingDisplay.css';

// Stage definitions for generation process
const STAGES = {
    CONNECTING: { icon: '~', label: 'Connecting...', order: 1 },
    SENDING: { icon: '>', label: 'Sending Request...', order: 2 },
    CONNECTED: { icon: '+', label: 'Connected!', order: 3 },
    STARTED: { icon: '*', label: 'Started', order: 4 },
    PROMPT_BUILT: { icon: '#', label: 'Analyzing Prompt', order: 5 },
    AI_CALLING: { icon: '@', label: 'AI Thinking...', order: 6 },
    AI_THINKING: { icon: '?', label: 'Reasoning', order: 7 },
    AI_CHUNK: { icon: '.', label: 'Receiving Data...', order: 7 },
    AI_COMPLETED: { icon: '=', label: 'Content Generated', order: 8 },
    VALIDATING: { icon: '/', label: 'Validating', order: 9 },
    VALIDATION_RESULT: { icon: '+', label: 'Validated', order: 10 },
    COMPLETED: { icon: '!', label: 'Completed!', order: 11 },
    RETRY: { icon: 'R', label: 'Retrying', order: 0 },
    FAILED: { icon: 'X', label: 'Failed', order: -1 },
    TIMEOUT_WARNING: { icon: 'W', label: 'Waiting...', order: 6 },
    ABORTED: { icon: 'X', label: 'Aborted', order: -1 },
    PROGRESS: { icon: '.', label: 'Processing...', order: 6 }
};

export default function StreamingDisplay({
    events = [],
    progress = 0,
    isActive = false,
    error = null,
    reasoning = '', // AI reasoning/thinking tokens
    onAbort,
    onBack  // Called when user wants to go back to config after abort/error
}) {
    const logRef = useRef(null);
    const [expandedReasoning, setExpandedReasoning] = useState(false);
    const [displayedReasoning, setDisplayedReasoning] = useState('');

    // Auto-scroll to bottom when new events arrive
    useEffect(() => {
        const timeoutId = setTimeout(() => {
            if (logRef.current) {
                const element = logRef.current;
                element.scrollTop = element.scrollHeight;
            }
        }, 50);
        return () => clearTimeout(timeoutId);
    }, [events, displayedReasoning]);

    // Typewriter effect for reasoning
    useEffect(() => {
        if (reasoning && reasoning.length > displayedReasoning.length) {
            const timeout = setTimeout(() => {
                setDisplayedReasoning(reasoning.slice(0, displayedReasoning.length + 5)); // Speed up typing slightly
            }, 5);
            return () => clearTimeout(timeout);
        }
    }, [reasoning, displayedReasoning]);

    // Get current stage from events
    const getCurrentStage = () => {
        if (events.length === 0) return null;
        const lastEvent = events[events.length - 1];
        return STAGES[lastEvent.type] || { icon: '*', label: lastEvent.type, order: 0 };
    };

    const currentStage = getCurrentStage();

    // Deduplicate events to prevent overlapping log entries
    // Only show first and last of consecutive PROGRESS events with same message
    const deduplicatedEvents = React.useMemo(() => {
        if (events.length <= 1) return events;

        const result = [];
        let i = 0;

        while (i < events.length) {
            const event = events[i];

            // For PROGRESS type events, skip duplicates with same message
            if (event.type === 'PROGRESS' || event.type === 'AI_CHUNK') {
                // Find all consecutive events with same type and message
                let j = i;
                while (j < events.length - 1 &&
                    events[j + 1].type === event.type &&
                    events[j + 1].message === event.message) {
                    j++;
                }

                // Only add the first one if there are duplicates
                if (j > i) {
                    // Add first with updated progress from last
                    result.push({
                        ...event,
                        progress: events[j].progress // Use latest progress
                    });
                    i = j + 1;
                } else {
                    result.push(event);
                    i++;
                }
            } else {
                result.push(event);
                i++;
            }
        }

        return result;
    }, [events]);

    // Format reasoning for display (extract from <think> tags if present)
    const formatReasoning = (text) => {
        if (!text) return '';
        const thinkMatch = text.match(/<think>([\s\S]*?)<\/think>/);
        if (thinkMatch) return thinkMatch[1].trim();
        const reasonMatch = text.match(/Reasoning:([\s\S]*?)(?=Output:|$)/i);
        if (reasonMatch) return reasonMatch[1].trim();
        return text;
    };

    return (
        <div className={`streaming-display ${isActive ? 'active' : ''} ${error ? 'error' : ''}`}>
            {/* Header with current stage */}
            <div className="streaming-header">
                <div className="streaming-status">
                    {isActive ? (
                        <FiLoader className="spin" size={20} />
                    ) : error ? (
                        <FiAlertTriangle size={20} />
                    ) : progress >= 100 ? (
                        <FiCheck size={20} />
                    ) : (
                        <FiCpu size={20} />
                    )}
                    <span>
                        {error ? 'Generation Error' :
                            currentStage ? currentStage.label :
                                isActive ? 'Initializing...' : 'Ready'}
                    </span>
                    {isActive && <span className="progress-text-header">{Math.round(progress)}%</span>}
                </div>

                {onAbort && isActive && (
                    <button className="abort-btn" onClick={onAbort} title="Stop Generation">
                        <FiStopCircle style={{ marginRight: '6px' }} /> Stop
                    </button>
                )}
            </div>

            {/* Progress bar */}
            <div className="streaming-progress">
                <div
                    className="progress-fill"
                    style={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
                />
                <span className="progress-text">{Math.round(progress)}%</span>
            </div>

            {/* AI Reasoning section - POSITIONED AT TOP */}
            {displayedReasoning && displayedReasoning !== 'null' && (
                <div className="reasoning-section" style={{ marginBottom: '12px', flexShrink: 0 }}>
                    <div
                        className="reasoning-header"
                        onClick={() => setExpandedReasoning(!expandedReasoning)}
                    >
                        <span className="reasoning-icon">?</span>
                        <span>AI Reasoning Process</span>
                        <span style={{ marginLeft: 'auto', fontSize: '0.8rem', opacity: 0.7 }}>
                            {expandedReasoning ? 'Hide' : 'Show'}
                        </span>
                    </div>
                    <div className={`reasoning-content ${expandedReasoning ? 'expanded' : ''}`}>
                        <pre>
                            {formatReasoning(displayedReasoning)}
                            {isActive && <span className="typing-cursor"></span>}
                        </pre>
                    </div>
                </div>
            )}

            {/* Event log */}
            <div className="streaming-log" ref={logRef}>
                {deduplicatedEvents.map((event, index) => {
                    const stage = STAGES[event.type] || { icon: '*' };
                    // Namespaced class to avoid global .progress conflicts
                    const typeClass = `type-${(event.type || 'unknown').toLowerCase()}`;

                    // Check if event has validation errors
                    const hasErrors = event.type === 'VALIDATION_RESULT' && event.data && Array.isArray(event.data) && event.data.length > 0;

                    return (
                        <div
                            key={index}
                            className={`log-entry ${typeClass}`}
                        >
                            <span className="log-icon">{stage.icon}</span>
                            <span className="log-message">{event.message}</span>
                            {event.progress != null && (
                                <span className="log-progress">{event.progress}%</span>
                            )}
                            {/* Show validation errors inline */}
                            {hasErrors && (
                                <div className="log-errors" style={{
                                    marginTop: '4px',
                                    marginLeft: '24px',
                                    fontSize: '0.85rem',
                                    color: 'rgba(239, 68, 68, 0.9)'
                                }}>
                                    {event.data.slice(0, 3).map((err, i) => (
                                        <div key={i}>- {err}</div>
                                    ))}
                                    {event.data.length > 3 && (
                                        <div style={{ opacity: 0.7 }}>...and {event.data.length - 3} more</div>
                                    )}
                                </div>
                            )}
                        </div>
                    );
                })}

                {/* Initial connection state */}
                {isActive && events.length === 0 && (
                    <div className="log-entry type-connecting">
                        <span className="log-icon">~</span>
                        <span className="log-message">Establishing connection to AI...</span>
                        <span className="typing-dots">
                            <span>.</span><span>.</span><span>.</span>
                        </span>
                    </div>
                )}
            </div>

            {/* Error display */}
            {error && (
                <div className="log-entry type-error" style={{ marginTop: 'auto', borderTop: '1px solid rgba(239, 68, 68, 0.2)', padding: '12px' }}>
                    <FiAlertTriangle size={16} color="#ef4444" style={{ flexShrink: 0 }} />
                    <span style={{ color: '#ef4444' }}>{error}</span>
                </div>
            )}

            {/* Back button when not active (aborted or error) */}
            {!isActive && onBack && (
                <button
                    className="abort-btn"
                    style={{ marginTop: '12px', background: 'rgba(139, 92, 246, 0.2)' }}
                    onClick={onBack}
                    title="Go back to config"
                >
                    Back to Configure
                </button>
            )}
        </div>
    );
}
