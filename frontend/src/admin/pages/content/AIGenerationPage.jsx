import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiArrowLeft, FiZap, FiSave, FiDatabase } from 'react-icons/fi';
import StudioConfigView from '../../components/abts/StudioConfigView';
import StepPreview from '../../components/abts/StepPreview';
import useABTSStore from '../../stores/useABTSStore';
import { saveGeneratedTest } from '../../services/abtsApi';
import { useToast } from '../../components/Toast';
import SaveAIContentModal from '../../components/abts/SaveAIContentModal';
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
        clearResult,
        formData,
        audioUrls,
        setAudioUrl
    } = useABTSStore();
    const toast = useToast();

    // View State: 'config' | 'preview'
    const [view, setView] = useState('config');
    const [isSaving, setIsSaving] = useState(false);
    const [showSaveModal, setShowSaveModal] = useState(false);

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
     * Triggered when user confirms save in the modal.
     */
    const handleModalSave = async (saveConfig) => {
        if (!generationResult?.content) return;

        setShowSaveModal(false); // Close modal
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

            // Construct partsToSave if content has multiple sections
            let partsToSave = null;
            if (generatedContent.sections && generatedContent.sections.length > 0) {
                partsToSave = generatedContent.sections.map(section => {
                    const pn = section.partNumber;
                    const skillUpper = skillLower.toUpperCase();

                    // Filter questions by standard IELTS ranges
                    const filteredQuestions = (generatedContent.questions || []).filter(q => {
                        const qn = q.questionNumber;
                        if (skillUpper === 'READING') {
                            if (pn === 1) return qn >= 1 && qn <= 13;
                            if (pn === 2) return qn >= 14 && qn <= 26;
                            if (pn === 3) return qn >= 27 && qn <= 40;
                        } else if (skillUpper === 'LISTENING') {
                            if (pn === 1) return qn >= 1 && qn <= 10;
                            if (pn === 2) return qn >= 11 && qn <= 20;
                            if (pn === 3) return qn >= 21 && qn <= 30;
                            if (pn === 4) return qn >= 31 && qn <= 40;
                        }
                        return true;
                    });

                    return {
                        partNumber: pn,
                        content: {
                            section: section,
                            questions: filteredQuestions
                        }
                    };
                });
            }

            // Build save request with modal data
            const saveRequest = {
                examSource: 'AI-GEN', // Legacy field
                testNumber: null, // Auto-generate
                skill: skillLower,
                partNumber: formData.partNumber || 1,
                topic: topic,
                content: generatedContent,

                // Use field names expected by abtsApi
                setId: saveConfig.setId,
                setCode: saveConfig.setCode,
                setName: saveConfig.setNameVi, // Map to abtsApi param name
                // Support append mode: use existingTestId if provided
                testId: saveConfig.existingTestId || saveConfig.testId,
                testName: saveConfig.testName, // Already correct name
                difficulty: saveConfig.difficulty,
                hashtagIds: saveConfig.hashtagIds,

                // Multi-part support
                partsToSave: partsToSave
            };

            const result = await saveGeneratedTest(saveRequest);

            if (result.success) {
                toast.success(`✅ Saved! Section ID: ${result.sectionId}, ${result.questionsCreated} questions created.`);

                // Show any warnings
                if (result.warnings?.length > 0) {
                    result.warnings.forEach(w => toast.warning(w));
                }

                // Reset generation result to prevent duplicate save on browser back
                clearResult();

                // Navigate to content list after short delay
                setTimeout(() => {
                    setIsSaving(false);
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
                            onClick={() => setShowSaveModal(true)}
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

            {/* Save Config Modal */}
            <SaveAIContentModal
                isOpen={showSaveModal}
                onClose={() => setShowSaveModal(false)}
                onSave={handleModalSave}
                initialTopic={formData.topic}
                suggestedSkill={formData.skill}
                partNumber={formData.partNumber || 1}
                selectedParts={formData.selectedParts}
                questionCount={generationResult?.content?.questions?.length || 0}
                audioUrls={audioUrls}
                onAudioUrlChange={setAudioUrl}
            />
        </div>
    );
}
