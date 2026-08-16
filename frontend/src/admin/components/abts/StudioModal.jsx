import { useState, useEffect } from 'react';
import { FiX, FiZap, FiArrowLeft, FiCheck } from 'react-icons/fi';
import StudioConfigView from './StudioConfigView';
import StepPreview from './StepPreview';
import useABTSStore from '../../stores/useABTSStore';
import './AIStudio.css';

/**
 * StudioModal - Embedded AI Generation in Modal.
 * 
 * Used in TestEditorPage and other places where we need AI generation
 * inside a modal rather than a full page.
 * 
 * V5.0: Uses unified AIStudio.css
 * 
 * @param {boolean} isOpen
 * @param {function} onClose
 * @param {function} onComplete - Callback with generated content
 * @param {string} initialSkill - Optional pre-selected skill
 */
export default function StudioModal({ isOpen, onClose, onComplete, initialSkill, mode, context }) {
    const [view, setView] = useState('config');
    const {
        resetForm,
        clearResult,
        setFormField,
        updateFormData,
        generateStreaming,
        isGenerating,
        generationResult
    } = useABTSStore();

    // Reset and initialize when opening
    useEffect(() => {
        if (isOpen) {
            resetForm();
            clearResult();
            setView('config');
            if (initialSkill) setFormField('skill', initialSkill.toUpperCase());
            if (mode) updateFormData({ generationMode: mode });
        }
    }, [isOpen, initialSkill, mode]);

    const handleGenerate = async () => {
        setView('preview');
        try {
            await generateStreaming();
        } catch (error) {
            console.error(error);
            setView('config');
        }
    };

    const handleApply = () => {
        if (generationResult?.content) {
            onComplete(generationResult.content);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="studio-overlay" onClick={onClose}>
            <div
                className="ai-studio ai-studio--modal"
                onClick={(e) => e.stopPropagation()}
                style={{
                    width: '90vw',
                    maxWidth: '1400px',
                    height: '85vh',
                    borderRadius: 'var(--studio-radius-lg)',
                    border: '1px solid var(--studio-border)'
                }}
            >
                {/* Header */}
                <header className="ai-studio__header">
                    <div className="ai-studio__brand">
                        {view === 'preview' && (
                            <button
                                className="studio-btn studio-btn--ghost studio-btn--icon"
                                onClick={() => setView('config')}
                                disabled={isGenerating}
                            >
                                <FiArrowLeft size={16} />
                            </button>
                        )}
                        <h1 className="ai-studio__title">
                            <FiZap className="ai-studio__title-icon" />
                            AI Assistant
                        </h1>
                        <span className="ai-studio__badge">
                            {view === 'preview' ? 'Preview' : 'Config'}
                        </span>
                    </div>

                    <div className="ai-studio__actions">
                        {view === 'preview' && (
                            <button
                                className="studio-btn studio-btn--primary"
                                onClick={handleApply}
                                disabled={isGenerating || !generationResult}
                            >
                                <FiCheck size={14} /> Insert into Editor
                            </button>
                        )}
                        <button
                            className="studio-btn studio-btn--ghost studio-btn--icon"
                            onClick={onClose}
                            title="Close"
                        >
                            <FiX size={18} />
                        </button>
                    </div>
                </header>

                {/* Content */}
                <div className="ai-studio__viewport">
                    {view === 'config' ? (
                        <StudioConfigView onGenerate={handleGenerate} />
                    ) : (
                        <StepPreview />
                    )}
                </div>
            </div>
        </div>
    );
}
