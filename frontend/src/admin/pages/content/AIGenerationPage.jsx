import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiArrowLeft, FiZap, FiSave, FiDatabase } from 'react-icons/fi';
import StudioConfigView from '../../components/abts/StudioConfigView';
import StepPreview from '../../components/abts/StepPreview';
import useABTSStore from '../../stores/useABTSStore';
import { saveGeneratedTest } from '../../services/abtsApi';
import { useToast } from '../../components/Toast';
import '../../components/abts/AIStudio.css';

/**
 * AI Generation Studio - Full-Width Power-User Interface
 * 
 * V5.0 Complete Redesign:
 * - Uses unified AIStudio.css
 * - Full-width layout (no max-width constraints)
 * - Dense, efficient configuration view
 * - Hybrid preview matching test-taking UI
 * 
 * @since 2025-12-22
 */
export default function AIGenerationPage() {
    const navigate = useNavigate();
    const {
        generateStreaming,
        isGenerating,
        generationResult,
        abortGeneration,
        formData
    } = useABTSStore();
    const toast = useToast();

    // View State: 'config' | 'preview'
    const [view, setView] = useState('config');
    const [isSaving, setIsSaving] = useState(false);

    const handleGenerate = async () => {
        setView('preview');
        try {
            await generateStreaming();
        } catch (error) {
            console.error(error);
            setView('config');
        }
    };

    const handleBack = () => {
        if (view === 'preview') {
            // If generation is still running, abort it first
            if (isGenerating) {
                abortGeneration();
            }
            setView('config');
        } else {
            navigate('/admin/content');
        }
    };

    /**
     * Save generated content directly to the database.
     * Creates a new section and all associated questions.
     */
    const handleSave = async () => {
        if (!generationResult?.content) return;

        setIsSaving(true);
        const generatedContent = generationResult.content;

        try {
            // Determine skill from formData or content
            const skill = formData.skill || generatedContent.skill || 'reading';
            const skillLower = String(skill).toLowerCase();

            // Get topic from various sources
            const topic = formData.topic ||
                generatedContent.metadata?.topic ||
                'AI Generated';

            // Build save request
            const saveRequest = {
                examSource: 'AI-GEN',
                testNumber: null, // Auto-generate
                skill: skillLower,
                partNumber: formData.partNumber || 1,
                topic: topic,
                content: generatedContent
            };

            const result = await saveGeneratedTest(saveRequest);

            if (result.success) {
                toast.success(`✅ Saved! Section ID: ${result.sectionId}, ${result.questionsCreated} questions created.`);

                // Show any warnings
                if (result.warnings?.length > 0) {
                    result.warnings.forEach(w => toast.warning(w));
                }

                // Navigate to content list after short delay
                setTimeout(() => {
                    navigate('/admin/content', {
                        state: {
                            refreshList: true,
                            savedSection: {
                                id: result.sectionId,
                                examSource: result.examSource,
                                testNumber: result.testNumber
                            }
                        }
                    });
                }, 1500);
            } else {
                toast.error(`Save failed: ${result.message}`);
                setIsSaving(false);
            }

        } catch (error) {
            console.error('Error saving content:', error);
            toast.error('Error saving content: ' + error.message);
            setIsSaving(false);
        }
    };

    return (
        <div className="ai-studio">
            {/* Header */}
            <header className="ai-studio__header">
                <div className="ai-studio__brand">
                    <button
                        className="studio-btn studio-btn--ghost studio-btn--icon"
                        onClick={handleBack}
                        title={view === 'preview' ? "Back to Configuration" : "Back to Dashboard"}
                    >
                        <FiArrowLeft size={16} />
                    </button>
                    <h1 className="ai-studio__title">
                        <FiZap className="ai-studio__title-icon" />
                        AI Generation Studio
                    </h1>
                    <span className="ai-studio__badge">
                        {view === 'preview' ? 'Preview' : 'Config'}
                    </span>
                </div>

                <div className="ai-studio__actions">
                    {view === 'preview' && (
                        <button
                            className="studio-btn studio-btn--primary"
                            onClick={handleSave}
                            disabled={!generationResult || isSaving || isGenerating}
                        >
                            {isSaving ? 'Saving...' : (
                                <>
                                    <FiDatabase size={14} /> Save to Database
                                </>
                            )}
                        </button>
                    )}
                </div>
            </header>

            {/* Main Content */}
            <div className="ai-studio__viewport">
                {view === 'config' ? (
                    <StudioConfigView onGenerate={handleGenerate} />
                ) : (
                    <StepPreview onBack={handleBack} />
                )}
            </div>

            {/* Saving Overlay */}
            {isSaving && (
                <div className="studio-overlay">
                    <div className="studio-overlay__content">
                        <div className="studio-spinner" />
                        <p style={{ marginTop: '16px', color: 'var(--studio-text-secondary)' }}>
                            Saving to database...
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
}
