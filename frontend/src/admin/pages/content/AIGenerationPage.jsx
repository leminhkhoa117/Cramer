import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import StudioConfigView from '../../components/abts/StudioConfigView';
import StepPreview from '../../components/abts/StepPreview';
import AIStudioTopBar from '../../components/abts/AIStudioTopBar';
import AIStudioIssueRail from '../../components/abts/AIStudioIssueRail';
import useABTSStore from '../../stores/useABTSStore';
import { useToast } from '../../components/Toast';
import SaveAIContentModal from '../../components/abts/SaveAIContentModal';

import {
    getAIStudioConfigReadiness,
    getAIStudioIssueCounts,
    getAIStudioSaveTargetSummary,
    getAIStudioStepState,
} from '../../components/abts/aiStudioStatus';
import '../../components/abts/AIStudio.css';

export default function AIGenerationPage() {
    const navigate = useNavigate();
    const {
        generateStreaming,
        isGenerating,
        generationResult,
        abortGeneration,
        clearResult,
        saveGeneratedContent,
        formData,
        audioUrls,
        setAudioUrl
    } = useABTSStore();
    const toast = useToast();

    const [view, setView] = useState('config');
    const [isSaving, setIsSaving] = useState(false);
    const [showSaveModal, setShowSaveModal] = useState(false);
    const [isRailCollapsed, setIsRailCollapsed] = useState(true);
    const navigateTimeoutRef = useRef(null);

    // Cancel the post-save navigate timeout if the user leaves early.
    useEffect(() => () => {
        if (navigateTimeoutRef.current) clearTimeout(navigateTimeoutRef.current);
    }, []);

    const configReadiness = useMemo(() => getAIStudioConfigReadiness(formData), [formData]);
    const issueCounts = useMemo(() => getAIStudioIssueCounts(generationResult), [generationResult]);
    const saveTarget = useMemo(
        () => getAIStudioSaveTargetSummary({ formData, generationResult }),
        [formData, generationResult]
    );
    const stepState = useMemo(() => getAIStudioStepState({
        view,
        isGenerating,
        generationResult,
        isSaving,
        isSaveModalOpen: showSaveModal,
        canGenerate: configReadiness.canGenerate,
    }), [view, isGenerating, generationResult, isSaving, showSaveModal, configReadiness.canGenerate]);

    // Auto-collapse rail on Configure step, auto-expand when issues exist after Generate.
    useEffect(() => {
        if (view === 'config') {
            setIsRailCollapsed(true);
        } else if (issueCounts.total > 0 && !isGenerating) {
            setIsRailCollapsed(false);
        }
    }, [view, issueCounts.total, isGenerating]);

    const handleGenerate = () => {
        setView('preview');
        generateStreaming().catch((error) => {
            console.error(error);
            setView('config');
        });
    };

    const handleBack = () => {
        if (view === 'preview') {
            if (isGenerating) abortGeneration();
            setView('config');
        } else {
            navigate('/admin/content');
        }
    };

    const handleConfigureStep = () => {
        if (isGenerating) abortGeneration();
        setView('config');
    };

    const handleOpenSaveModal = () => {
        if (!generationResult?.content || isSaving || isGenerating) return;
        setShowSaveModal(true);
    };

    const handleModalSave = async (saveConfig) => {
        if (!generationResult?.content) return;
        setShowSaveModal(false);
        setIsSaving(true);

        try {
            const result = await saveGeneratedContent(saveConfig);

            if (result?.success) {
                const sectionCount = result.sectionIds?.length ?? 0;
                toast.success(`Saved! ${sectionCount} section(s), ${result.questionCount ?? 0} questions created.`);
                clearResult();
                navigateTimeoutRef.current = setTimeout(() => {
                    setIsSaving(false);
                    navigate('/admin/content', {
                        state: {
                            refreshList: true,
                            savedSection: {
                                testId: result.testId,
                                setCode: result.setCode,
                                testNumber: result.testNumber
                            }
                        }
                    });
                }, 1500);
            } else {
                toast.error(`Save failed: ${result?.message || 'unknown error'}`);
                setIsSaving(false);
            }
        } catch (error) {
            console.error('Error saving content:', error);
            toast.error('Error saving content: ' + (error?.response?.data?.message || error.message));
            setIsSaving(false);
        }
    };

    return (
        <div className="ai-studio">
            <AIStudioTopBar
                view={view}
                steps={stepState}
                onBack={handleBack}
                onConfigureStep={handleConfigureStep}
                configReadiness={configReadiness}
                issueCount={issueCounts.total}
                saveTarget={saveTarget}
                isGenerating={isGenerating}
                isSaving={isSaving}
                onGenerate={handleGenerate}
                onSave={handleOpenSaveModal}
            />

            <div
                className="ai-studio__main"
                style={{ '--ai-studio-issue-rail-width': isRailCollapsed ? '40px' : '320px' }}
            >
                <div className="ai-studio__viewport">
                    {view === 'config' ? (
                        <StudioConfigView />
                    ) : (
                        <StepPreview onBack={handleBack} />
                    )}
                </div>

                <div className={`ai-studio__aside ${isRailCollapsed ? 'ai-studio__aside--collapsed' : ''}`}>
                    <AIStudioIssueRail
                        generationResult={generationResult}
                        isGenerating={isGenerating}
                        isCollapsed={isRailCollapsed}
                        onCollapsedChange={setIsRailCollapsed}
                    />
                </div>
            </div>

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

            <SaveAIContentModal
                isOpen={showSaveModal}
                onClose={() => setShowSaveModal(false)}
                onSave={handleModalSave}
                initialTopic={formData.topic}
                suggestedSkill={formData.skill}
                partNumber={formData.partNumber || 1}
                selectedParts={formData.selectedParts}
                questionCount={
                    generationResult?.content?.questions?.length
                    ?? (Array.isArray(generationResult?.content?.sections)
                        ? generationResult.content.sections.reduce((sum, sec) => sum + (sec.questions?.length || 0), 0)
                        : 0)
                }
                audioUrls={audioUrls}
                onAudioUrlChange={setAudioUrl}
            />
        </div>
    );
}
