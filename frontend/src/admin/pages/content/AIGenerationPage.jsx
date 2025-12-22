import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiArrowLeft, FiZap, FiSave } from 'react-icons/fi';
import StudioConfigView from '../../components/abts/StudioConfigView';
import StepPreview from '../../components/abts/StepPreview';
import useABTSStore from '../../stores/useABTSStore';
import useAdminContentStore from '../../stores/useAdminContentStore';
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
    const { createTest } = useAdminContentStore();
    const {
        generateStreaming,
        isGenerating,
        generationResult,
        abortGeneration
    } = useABTSStore();
    const toast = useToast();

    // View State: 'config' | 'preview'
    const [view, setView] = useState('config');
    const [isCreating, setIsCreating] = useState(false);

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

    const handleSave = async () => {
        if (!generationResult?.content) return;

        setIsCreating(true);
        const generatedContent = generationResult.content;

        try {
            const timestamp = new Date().getTime();
            const skill = generatedContent.skill || 'reading';
            const topic = generatedContent.topic || 'General';

            const testData = {
                examSource: 'AI-GEN',
                testNumber: timestamp.toString(),
                name: `AI Test: ${topic} (${new Date().toLocaleDateString()})`,
                topicName: topic,
                skills: {
                    [skill.toLowerCase()]: { status: 'draft' }
                }
            };

            const result = await createTest(testData);

            if (result && result.success) {
                toast.success('Test created! Redirecting...');
                navigate(
                    `/admin/content/editor/${result.examSource}/${result.testNumber}`,
                    {
                        state: {
                            generatedContent: generatedContent,
                            autoApply: true
                        }
                    }
                );
            } else {
                toast.error('Failed to create test container.');
                setIsCreating(false);
            }

        } catch (error) {
            console.error('Error creating test:', error);
            toast.error('Error saving content: ' + error.message);
            setIsCreating(false);
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
                            disabled={!generationResult || isCreating || isGenerating}
                        >
                            {isCreating ? 'Saving...' : (
                                <>
                                    <FiSave size={14} /> Save to Editor
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
            {isCreating && (
                <div className="studio-overlay">
                    <div className="studio-overlay__content">
                        <div className="studio-spinner" />
                        <p style={{ marginTop: '16px', color: 'var(--studio-text-secondary)' }}>
                            Finalizing and saving to editor...
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
}
