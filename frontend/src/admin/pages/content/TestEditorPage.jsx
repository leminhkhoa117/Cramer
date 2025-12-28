import React, { useEffect, useCallback, useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import {
    FiArrowLeft,
    FiSave,
    FiEye,
    FiUpload,
    FiCheck,
    FiPlus,
    FiTrash,
    FiEdit,
    FiX,
    FiZap,
    FiRefreshCw,
    FiCopy,
    FiSettings
} from 'react-icons/fi';
import StatusBadge from '../../components/StatusBadge';
import StudioModal from '../../components/abts/StudioModal';
import PassageInputModal from '../../components/content/PassageInputModal';
import AudioUploadModal from '../../components/content/AudioUploadModal';
import { useTestEditorStore } from '../../stores';
import useTestEditorStoreRaw from '../../stores/useTestEditorStore';
import adminApi from '../../api/adminApi';
import { sanitizeHtml } from '../../utils/htmlSanitizer';
import AdminPreviewContent from '../../components/content/AdminPreviewContent';
import '../../css/pages/content/TestEditorPage.css';

// Test status configurations
const testStatuses = [
    { value: 'DRAFT', label: 'Nháp', color: 'neutral' },
    { value: 'REVIEW', label: 'Đang duyệt', color: 'warning' },
    { value: 'PUBLISHED', label: 'Đã xuất bản', color: 'success' },
    { value: 'ARCHIVED', label: 'Lưu trữ', color: 'info' },
];

const getStatusColor = (status) => {
    const statusObj = testStatuses.find(s => s.value === status);
    return statusObj ? statusObj.color : 'neutral';
};

// Question types for different skills
const QUESTION_TYPES = {
    reading: [
        { value: 'TRUE_FALSE_NOT_GIVEN', label: 'True/False/Not Given' },
        { value: 'YES_NO_NOT_GIVEN', label: 'Yes/No/Not Given' },
        { value: 'MATCHING_HEADINGS', label: 'Matching Headings' },
        { value: 'MATCHING_INFORMATION', label: 'Matching Information' },
        { value: 'MATCHING_FEATURES', label: 'Matching Features' },
        { value: 'FILL_IN_BLANK', label: 'Fill in the Blank' },
        { value: 'SENTENCE_COMPLETION', label: 'Sentence Completion' },
        { value: 'SUMMARY_COMPLETION', label: 'Summary Completion' },
        { value: 'MULTIPLE_CHOICE_SINGLE', label: 'Multiple Choice (Single)' },
        { value: 'MULTIPLE_CHOICE_MULTIPLE', label: 'Multiple Choice (Multiple)' },
    ],
    listening: [
        { value: 'FILL_IN_BLANK', label: 'Note Completion' },
        { value: 'MULTIPLE_CHOICE_SINGLE', label: 'Multiple Choice (Single)' },
        { value: 'MULTIPLE_CHOICE_MULTIPLE', label: 'Multiple Choice (Multiple)' },
        { value: 'MATCHING', label: 'Matching / Map / Plan' },
    ],
    writing: [
        { value: 'TASK_1', label: 'Task 1 - Charts/Graphs' },
        { value: 'TASK_2', label: 'Task 2 - Essay' },
    ],
    speaking: [
        { value: 'PART_1', label: 'Part 1 - Introduction' },
        { value: 'PART_2', label: 'Part 2 - Cue Card' },
        { value: 'PART_3', label: 'Part 3 - Discussion' },
    ],
};

// Format display name
const formatDisplayName = (examSource) => {
    if (!examSource) return 'Unknown';
    if (examSource.toLowerCase().startsWith('cam')) {
        const number = examSource.substring(3);
        return 'Cambridge IELTS ' + number;
    }
    if (examSource.toLowerCase().startsWith('real')) {
        return 'Real Tests';
    }
    return examSource.charAt(0).toUpperCase() + examSource.slice(1);
};

export default function TestEditorPage() {
    const { examSource: paramSource, testNumber: paramNumber, testId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    // Store state and actions
    const {
        test,
        sections,
        questions,
        allQuestions,
        activeSkill,
        activeSection,
        isLoading,
        isLoadingSections,
        isLoadingQuestions,
        isSaving,
        isPublishing,
        showAIGenerationModal,
        error,
        initializeEditor,
        initializeEditorByTestId,
        setActiveSkill,
        setActiveSection,
        saveDraft,
        publishTest,
        addSection,
        addQuestion,
        deleteQuestion,
        openAIGeneration,
        closeAIGeneration,
        generationMode,
        generationContext,
        getSectionName,
        reset,
        applyGeneratedContent
    } = useTestEditorStore();

    // Derived values
    const examSource = paramSource || test?.examSource;
    const testNumber = paramNumber || test?.testNumber;

    // Modal state for uploading content
    const [showPassageModal, setShowPassageModal] = useState(false);
    const [showAudioModal, setShowAudioModal] = useState(false);

    // Skills configuration
    const skills = [
        { id: 'reading', label: 'Reading', icon: '📖' },
        { id: 'listening', label: 'Listening', icon: '🎧' },
        { id: 'writing', label: 'Writing', icon: '✍️' },
        { id: 'speaking', label: 'Speaking', icon: '🎤' },
    ];

    // Initialize on mount
    useEffect(() => {
        const init = async () => {
            if (testId) {
                await initializeEditorByTestId(testId);
            } else if (examSource && testNumber) {
                await initializeEditor(examSource, testNumber);
            }

            // Auto-apply generated content if passed from AI Wizard
            if (location.state?.generatedContent && (testId || (examSource && testNumber))) {
                const { applyGeneratedContent } = useTestEditorStoreRaw.getState();
                await applyGeneratedContent(location.state.generatedContent, examSource, testNumber);
                window.history.replaceState({}, document.title);
            }
        };
        init();
        return () => reset();
    }, [testId, paramSource, paramNumber, initializeEditor, initializeEditorByTestId, reset]);

    // Handler: Change skill
    const handleSkillChange = useCallback((skillId) => {
        setActiveSkill(skillId, examSource, testNumber);
    }, [setActiveSkill, examSource, testNumber]);

    // Handler: Preview test
    const handlePreview = useCallback(() => {
        const previewUrl = `/test/${examSource}/${testNumber}/reading`;
        window.open(previewUrl, '_blank');
    }, [examSource, testNumber]);

    // Handler: Save as draft
    const handleSaveDraft = useCallback(async () => {
        const success = await saveDraft(examSource, testNumber);
        if (success) {
            alert('Đã lưu nháp thành công!');
        } else {
            alert('Lỗi khi lưu nháp');
        }
    }, [saveDraft, examSource, testNumber]);

    // Handler: Publish test
    const handlePublish = useCallback(async () => {
        if (!window.confirm('Bạn có chắc muốn xuất bản đề thi này?')) return;

        const success = await publishTest(examSource, testNumber);
        if (success) {
            alert('Đã xuất bản thành công!');
        } else {
            alert('Lỗi khi xuất bản');
        }
    }, [publishTest, examSource, testNumber]);

    // Handler: Add new section
    const handleAddSection = useCallback(async () => {
        const sectionId = await addSection(examSource, testNumber);
        if (sectionId) {
            alert('Đã thêm section mới!');
        }
    }, [addSection, examSource, testNumber]);

    // Handler: Add new question
    const handleAddQuestion = useCallback(async () => {
        if (!activeSection) {
            alert('Vui lòng chọn một section trước!');
            return;
        }

        const questionNumber = await addQuestion();
        if (questionNumber) {
            alert(`Đã thêm câu hỏi ${questionNumber}!`);
        }
    }, [addQuestion, activeSection]);

    // Handler: Delete question
    const handleDeleteQuestion = useCallback(async (questionId) => {
        if (!window.confirm('Bạn có chắc muốn xóa câu hỏi này?')) return;

        const success = await deleteQuestion(questionId);
        if (success) {
            alert('Đã xóa câu hỏi!');
        }
    }, [deleteQuestion]);

    // Handler: Save passage text
    const handleSavePassage = useCallback(async (passageText) => {
        if (!activeSection || !passageText) return;

        try {
            await adminApi.content.updateSection(activeSection, { passageText });

            // Refresh sections to show new data
            const { fetchSections } = useTestEditorStoreRaw.getState();
            await fetchSections(examSource, testNumber, activeSkill);

            alert('✅ Đã lưu nội dung passage!');
        } catch (error) {
            console.error('Failed to save passage:', error);
            alert('❌ Không thể lưu passage. Vui lòng thử lại.');
        }
    }, [activeSection, examSource, testNumber, activeSkill]);

    // Handler: Save audio file
    const handleSaveAudio = useCallback(async (audioData) => {
        if (!activeSection) return;

        try {
            // For now, save the URL directly
            // In production, you would upload to Supabase Storage first
            const audioUrl = audioData.url;

            await adminApi.content.updateSection(activeSection, { audioUrl });

            // Refresh sections to show new data
            const { fetchSections } = useTestEditorStoreRaw.getState();
            await fetchSections(examSource, testNumber, activeSkill);

            alert('✅ Đã lưu file audio!');
        } catch (error) {
            console.error('Failed to save audio:', error);
            alert('❌ Không thể lưu audio. Vui lòng thử lại.');
        }
    }, [activeSection, examSource, testNumber, activeSkill]);

    // Loading state
    if (isLoading) {
        return (
            <div className="admin-page test-editor-page">
                <div className="content-loading">
                    <div className="spinner"></div>
                    <p>Đang tải đề thi...</p>
                </div>
            </div>
        );
    }

    // Error state or not found
    if (error || !test) {
        return (
            <div className="admin-page test-editor-page">
                <div className="not-found">
                    <h2>{error || 'Không tìm thấy đề thi'}</h2>
                    <p>Exam Source: {examSource}, Test Number: {testNumber}</p>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={() => navigate('/admin/content')}
                    >
                        Quay lại danh sách
                    </button>
                </div>
            </div>
        );
    }

    // Get active section data
    const activeSectionData = sections.find(s => s.id === activeSection);

    // Get question range for section based on actual data
    const getQuestionRange = (section, index) => {
        if (activeSkill === 'writing' || activeSkill === 'speaking') {
            return getSectionName(section);
        }

        // Calculate start based on previous sections' question counts
        let start = 1;
        for (let i = 0; i < index; i++) {
            start += sections[i]?.questionCount || 0;
        }

        const count = section.questionCount || 0;
        if (count === 0) {
            return `Q${start}+`;
        }

        const end = start + count - 1;
        return `Q${start}-${end}`;
    };

    // Extract displayable text from question content
    const getQuestionText = (question) => {
        const content = question.questionContent;

        // If no content, show UID as fallback
        if (!content) {
            return question.questionUid ? `UID: ${question.questionUid}` : 'Chưa có nội dung';
        }

        // Try to parse if it's a JSON string
        let parsed = content;
        if (typeof content === 'string') {
            try {
                parsed = JSON.parse(content);
            } catch {
                // If it's not valid JSON, use it as-is (could be plain text)
                return content.length > 80 ? content.substring(0, 80) + '...' : content;
            }
        }

        // Extract text from common question content structures
        if (typeof parsed === 'object') {
            const textFields = ['statement', 'question', 'sentence', 'incomplete_sentence', 'text', 'prompt', 'heading'];
            for (const field of textFields) {
                if (parsed[field]) {
                    const text = parsed[field];
                    return text.length > 80 ? text.substring(0, 80) + '...' : text;
                }
            }
            // If it has options, try to show them
            if (parsed.options && Array.isArray(parsed.options)) {
                return `[${parsed.options.length} options]`;
            }
        }

        // Fallback: show UID
        return question.questionUid ? `UID: ${question.questionUid}` : 'Chưa có nội dung';
    };

    return (
        <div className="admin-page test-editor-page">
            {/* Editor Header */}
            <div className="editor-header">
                <div className="editor-header__left">
                    <button className="back-btn" onClick={() => navigate('/admin/content')}>
                        <FiArrowLeft size={18} />
                    </button>
                    <div className="editor-header__info">
                        <span className="editor-header__breadcrumb">{formatDisplayName(test.examSource)}</span>
                        <h1 className="editor-header__title">{test.name}</h1>
                    </div>
                    <StatusBadge status={test.status} variant={getStatusColor(test.status)} />
                </div>
                <div className="editor-header__actions">
                    {/* AI Generation Button */}
                    <button
                        className="admin-btn admin-btn--ai"
                        onClick={openAIGeneration}
                        title="Tạo nội dung bằng AI"
                    >
                        <FiZap size={16} />
                        <span>Tạo bằng AI</span>
                    </button>
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={handlePreview}
                    >
                        <FiEye size={16} />
                        <span>Xem trước</span>
                    </button>
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={handleSaveDraft}
                        disabled={isSaving}
                    >
                        <FiSave size={16} />
                        <span>{isSaving ? 'Đang lưu...' : 'Lưu nháp'}</span>
                    </button>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={handlePublish}
                        disabled={isPublishing}
                    >
                        <FiCheck size={16} />
                        <span>{isPublishing ? 'Đang xuất bản...' : 'Xuất bản'}</span>
                    </button>
                </div>
            </div>

            {/* Skill Tabs */}
            <div className="skill-tabs">
                {skills.map(skill => (
                    <button
                        key={skill.id}
                        className={`skill-tab ${activeSkill === skill.id ? 'skill-tab--active' : ''}`}
                        onClick={() => handleSkillChange(skill.id)}
                    >
                        <span className="skill-tab__icon">{skill.icon}</span>
                        <span className="skill-tab__label">{skill.label}</span>
                        <span className={`skill-tab__status skill-tab__status--${test.skills?.[skill.id]?.status || 'empty'}`} />
                    </button>
                ))}
            </div>

            {/* Editor Content */}
            <div className="editor-content">
                {/* Main Editor Area */}
                <div className="editor-main editor-main--full">
                    {activeSection ? (
                        <AdminPreviewContent
                            sections={sections}
                            questions={allQuestions.length > 0 ? allQuestions : questions}
                            activePartIndex={sections.findIndex(s => s.id === activeSection)}
                            skill={activeSkill}
                            onPartSelect={(index) => setActiveSection(sections[index]?.id)}
                            onQuestionSelect={() => { }}
                        />
                    ) : (
                        <div className="no-section-selected">
                            <FiSettings size={48} />
                            <h3>Chọn một section để bắt đầu</h3>
                            <p>Chọn một section từ danh sách bên trái, hoặc tạo section mới</p>
                            <div className="no-section-actions">
                                <button
                                    className="admin-btn admin-btn--primary"
                                    onClick={handleAddSection}
                                >
                                    <FiPlus size={16} />
                                    <span>Thêm Section mới</span>
                                </button>
                                <button
                                    className="admin-btn admin-btn--ai"
                                    onClick={openAIGeneration}
                                >
                                    <FiZap size={16} />
                                    <span>Tạo nội dung bằng AI</span>
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* AI Generation Modal */}
            {/* AI Generation Studio Modal (Embedded) */}
            {showAIGenerationModal && (
                <StudioModal
                    isOpen={showAIGenerationModal}
                    onClose={closeAIGeneration}
                    initialSkill={activeSkill?.toUpperCase()}
                    mode={generationMode}
                    context={generationContext}
                    onComplete={async (generatedContent) => {
                        // Save generated content to database
                        console.log('Generated content received:', generatedContent);

                        if (examSource && testNumber && generatedContent) {
                            const { applyGeneratedContent } = useTestEditorStoreRaw.getState();
                            const success = await applyGeneratedContent(
                                generatedContent,
                                examSource,
                                testNumber
                            );

                            if (success) {
                                alert('✅ Nội dung đã được lưu vào đề thi!');
                            } else {
                                alert('❌ Không thể lưu nội dung. Vui lòng thử lại.');
                            }
                        }

                        closeAIGeneration();
                    }}
                />
            )}

            {/* Passage Input Modal */}
            <PassageInputModal
                isOpen={showPassageModal}
                onClose={() => setShowPassageModal(false)}
                onSave={handleSavePassage}
                initialText={sections.find(s => s.id === activeSection)?.passageText || ''}
                title="Thêm/Chỉnh sửa Passage"
            />

            {/* Audio Upload Modal */}
            <AudioUploadModal
                isOpen={showAudioModal}
                onClose={() => setShowAudioModal(false)}
                onSave={handleSaveAudio}
                initialAudioUrl={sections.find(s => s.id === activeSection)?.audioUrl || ''}
                title="Upload Audio"
            />
        </div>
    );
}
