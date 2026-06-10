import React from 'react';
import {
    FiArrowLeft, FiZap, FiDatabase, FiAlertTriangle, FiCheck, FiCircle
} from 'react-icons/fi';

/**
 * Single 48px unified top bar.
 * Replaces the old ai-studio__header + AIStudioStepper + AIStudioPreviewToolbar trio.
 *
 * Layout (grid):
 *   [brand] [stepper dots] [summary chips] [primary action]
 */
export default function AIStudioTopBar({
    view,
    steps = [],
    onBack,
    onConfigureStep,
    configReadiness,
    issueCount,
    saveTarget,
    isGenerating,
    isSaving,
    onGenerate,
    onSave,
}) {
    const isConfigure = view === 'config';
    const primaryLabel = isConfigure
        ? (isGenerating ? 'Generating' : 'Generate')
        : (isSaving ? 'Saving' : 'Save');
    const primaryIcon = isConfigure ? <FiZap size={14} /> : <FiDatabase size={14} />;
    const primaryDisabled = isConfigure
        ? !configReadiness?.canGenerate || isGenerating
        : !saveTarget?.isReadyToSave || isGenerating || isSaving;
    const handlePrimary = isConfigure ? onGenerate : onSave;

    return (
        <header className="ai-studio-topbar" role="banner">
            <div className="ai-studio-topbar__brand">
                <button
                    type="button"
                    className="ai-studio-topbar__back"
                    onClick={onBack}
                    title={isConfigure ? 'Back to dashboard' : 'Back to configuration'}
                    aria-label="Back"
                >
                    <FiArrowLeft size={15} />
                </button>
                <span className="ai-studio-topbar__mark"><FiZap size={13} /></span>
                <span className="ai-studio-topbar__title">AI Studio</span>
                <span className="ai-studio-topbar__badge">{isConfigure ? 'Config' : 'Preview'}</span>
            </div>

            <nav className="ai-studio-topbar__steps" aria-label="Workflow steps">
                {steps.map((step, index) => {
                    const clickable = step.id === 'configure' && !isConfigure;
                    const Tag = clickable ? 'button' : 'span';
                    return (
                        <React.Fragment key={step.id}>
                            {index > 0 && <span className="ai-studio-topbar__step-sep" aria-hidden="true" />}
                            <Tag
                                type={clickable ? 'button' : undefined}
                                className={
                                    'ai-studio-topbar__step'
                                    + (step.isActive ? ' is-active' : '')
                                    + (step.isComplete ? ' is-complete' : '')
                                    + (clickable ? ' is-clickable' : '')
                                }
                                onClick={clickable ? onConfigureStep : undefined}
                                aria-current={step.isActive ? 'step' : undefined}
                            >
                                <span className="ai-studio-topbar__step-dot" aria-hidden="true">
                                    {step.isComplete ? <FiCheck size={9} /> : <FiCircle size={6} />}
                                </span>
                                <span className="ai-studio-topbar__step-label">{step.label}</span>
                            </Tag>
                        </React.Fragment>
                    );
                })}
            </nav>

            <div className="ai-studio-topbar__summary">
                <span className="ai-studio-topbar__chip">{saveTarget?.skillLabel || '—'}</span>
                <span className="ai-studio-topbar__chip">{saveTarget?.partsLabel || '—'}</span>
                <span className={
                    'ai-studio-topbar__chip ai-studio-topbar__chip--issues'
                    + (issueCount > 0 ? ' is-warn' : '')
                }>
                    <FiAlertTriangle size={11} />
                    {issueCount} issue{issueCount === 1 ? '' : 's'}
                </span>
            </div>

            <button
                type="button"
                className="ai-studio-topbar__primary"
                onClick={handlePrimary}
                disabled={primaryDisabled}
            >
                {primaryIcon}
                <span>{primaryLabel}</span>
            </button>
        </header>
    );
}
