import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
    FiRefreshCw
} from 'react-icons/fi';
import StatusBadge from '../../components/StatusBadge';
import adminApi from '../../api/adminApi';
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
    const { examSource, testNumber } = useParams();
    const navigate = useNavigate();

    // State
    const [activeSkill, setActiveSkill] = useState('reading');
    const [activeSection, setActiveSection] = useState(null);
    const [test, setTest] = useState(null);
    const [sections, setSections] = useState([]);
    const [questions, setQuestions] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isLoadingSections, setIsLoadingSections] = useState(false);
    const [isLoadingQuestions, setIsLoadingQuestions] = useState(false);
    const [error, setError] = useState(null);
    const [isSaving, setIsSaving] = useState(false);
    const [isPublishing, setIsPublishing] = useState(false);

    // Skills configuration
    const skills = [
        { id: 'reading', label: 'Reading', icon: '📖' },
        { id: 'listening', label: 'Listening', icon: '🎧' },
        { id: 'writing', label: 'Writing', icon: '✍️' },
        { id: 'speaking', label: 'Speaking', icon: '🎤' },
    ];

    // Handler: Preview test
    const handlePreview = () => {
        const previewUrl = `/test/${examSource}/${testNumber}/reading`;
        window.open(previewUrl, '_blank');
    };

    // Handler: Save as draft
    const handleSaveDraft = async () => {
        if (isSaving) return;
        setIsSaving(true);
        try {
            await adminApi.content.updateTestStatus(examSource, parseInt(testNumber), 'DRAFT');
            setTest(prev => ({ ...prev, status: 'DRAFT' }));
            alert('Đã lưu nháp thành công!');
        } catch (err) {
            console.error('Error saving draft:', err);
            alert('Lỗi khi lưu nháp: ' + (err.message || 'Unknown error'));
        } finally {
            setIsSaving(false);
        }
    };

    // Handler: Publish test
    const handlePublish = async () => {
        if (isPublishing) return;
        if (!window.confirm('Bạn có chắc muốn xuất bản đề thi này?')) return;

        setIsPublishing(true);
        try {
            await adminApi.content.updateTestStatus(examSource, parseInt(testNumber), 'PUBLISHED');
            setTest(prev => ({ ...prev, status: 'PUBLISHED' }));
            alert('Đã xuất bản thành công!');
        } catch (err) {
            console.error('Error publishing:', err);
            alert('Lỗi khi xuất bản: ' + (err.message || 'Unknown error'));
        } finally {
            setIsPublishing(false);
        }
    };

    // Handler: Add new section
    const handleAddSection = async () => {
        try {
            const existingParts = sections.map(s => s.partNumber);
            const nextPart = existingParts.length > 0 ? Math.max(...existingParts) + 1 : 1;

            const result = await adminApi.content.createSection({
                examSource,
                testNumber: parseInt(testNumber),
                skill: activeSkill,
                partNumber: nextPart
            });

            if (result.success) {
                // Refresh sections
                const data = await adminApi.content.getSections(examSource, parseInt(testNumber), activeSkill);
                setSections(data || []);
                setActiveSection(result.sectionId);
                alert('Đã thêm section mới!');
            }
        } catch (err) {
            console.error('Error adding section:', err);
            alert('Lỗi khi thêm section: ' + (err.message || 'Unknown error'));
        }
    };

    // Handler: Add new question
    const handleAddQuestion = async () => {
        if (!activeSection) {
            alert('Vui lòng chọn một section trước!');
            return;
        }

        try {
            const result = await adminApi.content.createQuestion(activeSection, {
                questionType: 'FILL_IN_BLANK',
                questionContent: JSON.stringify({ text: '' }),
                correctAnswer: JSON.stringify({ answer: '' })
            });

            if (result.success) {
                // Refresh questions
                const data = await adminApi.content.getQuestions(activeSection);
                setQuestions(data || []);
                alert(`Đã thêm câu hỏi ${result.questionNumber}!`);
            }
        } catch (err) {
            console.error('Error adding question:', err);
            alert('Lỗi khi thêm câu hỏi: ' + (err.message || 'Unknown error'));
        }
    };

    // Handler: Delete question
    const handleDeleteQuestion = async (questionId) => {
        if (!window.confirm('Bạn có chắc muốn xóa câu hỏi này?')) return;

        try {
            await adminApi.content.deleteQuestion(questionId);
            setQuestions(prev => prev.filter(q => q.id !== questionId));
            alert('Đã xóa câu hỏi!');
        } catch (err) {
            console.error('Error deleting question:', err);
            alert('Lỗi khi xóa câu hỏi: ' + (err.message || 'Unknown error'));
        }
    };

    // Fetch test details
    useEffect(() => {
        const fetchTest = async () => {
            setIsLoading(true);
            setError(null);
            try {
                const data = await adminApi.content.getTestDetails(examSource, parseInt(testNumber));
                setTest(data);
            } catch (err) {
                console.error('Error fetching test:', err);
                setError('Không thể tải thông tin đề thi');
            } finally {
                setIsLoading(false);
            }
        };

        if (examSource && testNumber) {
            fetchTest();
        }
    }, [examSource, testNumber]);

    // Fetch sections when skill changes
    useEffect(() => {
        const fetchSections = async () => {
            setIsLoadingSections(true);
            try {
                const data = await adminApi.content.getSections(examSource, parseInt(testNumber), activeSkill);
                setSections(data || []);
                // Auto-select first section
                if (data && data.length > 0) {
                    setActiveSection(data[0].id);
                } else {
                    setActiveSection(null);
                    setQuestions([]);
                }
            } catch (err) {
                console.error('Error fetching sections:', err);
                setSections([]);
            } finally {
                setIsLoadingSections(false);
            }
        };

        if (examSource && testNumber && activeSkill) {
            fetchSections();
        }
    }, [examSource, testNumber, activeSkill]);

    // Fetch questions when section changes
    useEffect(() => {
        const fetchQuestions = async () => {
            if (!activeSection) {
                setQuestions([]);
                return;
            }

            setIsLoadingQuestions(true);
            try {
                const data = await adminApi.content.getQuestions(activeSection);
                setQuestions(data || []);
            } catch (err) {
                console.error('Error fetching questions:', err);
                setQuestions([]);
            } finally {
                setIsLoadingQuestions(false);
            }
        };

        fetchQuestions();
    }, [activeSection]);

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

    // Get section name based on skill
    const getSectionName = (section) => {
        if (activeSkill === 'reading') {
            return `Passage ${section.partNumber}`;
        } else if (activeSkill === 'listening') {
            return `Part ${section.partNumber}`;
        } else if (activeSkill === 'writing') {
            return `Task ${section.partNumber}`;
        } else if (activeSkill === 'speaking') {
            return `Part ${section.partNumber}`;
        }
        return `Section ${section.partNumber}`;
    };

    // Get question range for section
    const getQuestionRange = (section, index) => {
        if (activeSkill === 'writing' || activeSkill === 'speaking') {
            return `${getSectionName(section)}`;
        }
        // Calculate approximate question range
        const questionsPerSection = activeSkill === 'listening' ? 10 : 13;
        const start = index * questionsPerSection + 1;
        const end = Math.min(start + section.questionCount - 1, 40);
        return `Q${start}-${end}`;
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
                        onClick={() => {
                            setActiveSkill(skill.id);
                            setActiveSection(null);
                            setQuestions([]);
                        }}
                    >
                        <span className="skill-tab__icon">{skill.icon}</span>
                        <span className="skill-tab__label">{skill.label}</span>
                        <span className={`skill-tab__status skill-tab__status--${test.skills?.[skill.id]?.status || 'empty'}`} />
                    </button>
                ))}
            </div>

            {/* Editor Content */}
            <div className="editor-content">
                {/* Left Panel - Section Navigator */}
                <div className="editor-sidebar">
                    <div className="editor-sidebar__header">
                        <h3>Sections</h3>
                        <button className="add-section-btn" title="Thêm Section" onClick={handleAddSection}>
                            <FiPlus size={16} />
                        </button>
                    </div>

                    {isLoadingSections ? (
                        <div className="sidebar-loading">
                            <div className="spinner small"></div>
                            <span>Đang tải...</span>
                        </div>
                    ) : sections.length === 0 ? (
                        <div className="sidebar-empty">
                            <p>Chưa có section nào</p>
                            <button className="admin-btn admin-btn--primary admin-btn--small" onClick={handleAddSection}>
                                <FiPlus size={14} />
                                <span>Thêm Section</span>
                            </button>
                        </div>
                    ) : (
                        <div className="section-list">
                            {sections.map((section, index) => (
                                <div
                                    key={section.id}
                                    className={`section-item ${activeSection === section.id ? 'section-item--active' : ''}`}
                                    onClick={() => setActiveSection(section.id)}
                                >
                                    <div className="section-item__info">
                                        <span className="section-item__name">{getSectionName(section)}</span>
                                        <span className="section-item__title">{getQuestionRange(section, index)}</span>
                                    </div>
                                    <div className="section-item__meta">
                                        <span className="section-item__questions">
                                            {section.questionCount} câu
                                        </span>
                                        {section.audioUrl && (
                                            <span className="section-item__audio">🎵</span>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Question Navigator */}
                    {questions.length > 0 && (
                        <div className="question-navigator">
                            <h4>Câu hỏi</h4>
                            <div className="question-grid">
                                {questions.map(q => (
                                    <button
                                        key={q.id}
                                        className={`question-btn ${q.correctAnswer ? 'question-btn--complete' : 'question-btn--incomplete'}`}
                                        title={q.questionType}
                                    >
                                        {q.questionNumber}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}
                </div>

                {/* Main Editor Area */}
                <div className="editor-main">
                    {/* Section Header */}
                    {activeSection ? (
                        <>
                            <div className="section-header">
                                <div className="section-header__info">
                                    <h2>{getSectionName(sections.find(s => s.id === activeSection) || { partNumber: 1 })}</h2>
                                    <p>
                                        {sections.find(s => s.id === activeSection)?.questionCount || 0} câu hỏi
                                    </p>
                                </div>
                                <div className="section-header__actions">
                                    {activeSkill === 'listening' && (
                                        <button className="admin-btn admin-btn--secondary">
                                            <FiUpload size={16} />
                                            <span>Upload Audio</span>
                                        </button>
                                    )}
                                    {activeSkill === 'reading' && (
                                        <button className="admin-btn admin-btn--secondary">
                                            <FiUpload size={16} />
                                            <span>Upload Passage</span>
                                        </button>
                                    )}
                                    <button className="admin-btn admin-btn--secondary">
                                        <FiEdit size={16} />
                                        <span>Chỉnh sửa</span>
                                    </button>
                                </div>
                            </div>

                            {/* Passage/Content Area */}
                            <div className="passage-area">
                                <div className="passage-content">
                                    {activeSkill === 'reading' && (
                                        <p className="passage-placeholder">
                                            <i>Nội dung passage sẽ hiển thị ở đây...</i>
                                        </p>
                                    )}
                                    {activeSkill === 'listening' && (
                                        <div className="audio-placeholder">
                                            <span>🎧</span>
                                            <p>Chưa có file audio</p>
                                            <button className="admin-btn admin-btn--primary admin-btn--small">
                                                <FiUpload size={14} />
                                                <span>Upload Audio</span>
                                            </button>
                                        </div>
                                    )}
                                    {activeSkill === 'writing' && (
                                        <p className="passage-placeholder">
                                            <i>Đề bài Writing Task sẽ hiển thị ở đây...</i>
                                        </p>
                                    )}
                                    {activeSkill === 'speaking' && (
                                        <p className="passage-placeholder">
                                            <i>Câu hỏi Speaking Part sẽ hiển thị ở đây...</i>
                                        </p>
                                    )}
                                </div>
                            </div>

                            {/* Questions List */}
                            <div className="questions-area">
                                <div className="questions-header">
                                    <h3>Danh sách câu hỏi</h3>
                                    <button className="admin-btn admin-btn--primary" onClick={handleAddQuestion}>
                                        <FiPlus size={16} />
                                        <span>Thêm câu hỏi</span>
                                    </button>
                                </div>

                                {isLoadingQuestions ? (
                                    <div className="questions-loading">
                                        <div className="spinner small"></div>
                                        <span>Đang tải câu hỏi...</span>
                                    </div>
                                ) : questions.length === 0 ? (
                                    <div className="questions-empty">
                                        <p>Chưa có câu hỏi nào trong section này</p>
                                        <button className="admin-btn admin-btn--primary" onClick={handleAddQuestion}>
                                            <FiPlus size={16} />
                                            <span>Thêm câu hỏi đầu tiên</span>
                                        </button>
                                    </div>
                                ) : (
                                    <div className="questions-list">
                                        {questions.map(question => (
                                            <div key={question.id} className="question-item">
                                                <div className="question-item__number">
                                                    <span className={question.correctAnswer ? 'complete' : 'incomplete'}>
                                                        {question.questionNumber}
                                                    </span>
                                                </div>
                                                <div className="question-item__content">
                                                    <span className="question-item__type">
                                                        {(question.questionType || '').replace(/_/g, ' ')}
                                                    </span>
                                                    <p className="question-item__text">
                                                        UID: {question.questionUid}
                                                    </p>
                                                </div>
                                                <div className="question-item__status">
                                                    {question.correctAnswer ? (
                                                        <span className="status-complete">
                                                            <FiCheck size={14} /> Đã có đáp án
                                                        </span>
                                                    ) : (
                                                        <span className="status-incomplete">
                                                            <FiX size={14} /> Thiếu đáp án
                                                        </span>
                                                    )}
                                                </div>
                                                <div className="question-item__actions">
                                                    <button className="icon-btn" title="Chỉnh sửa">
                                                        <FiEdit size={16} />
                                                    </button>
                                                    <button
                                                        className="icon-btn icon-btn--danger"
                                                        title="Xóa"
                                                        onClick={() => handleDeleteQuestion(question.id)}
                                                    >
                                                        <FiTrash size={16} />
                                                    </button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </>
                    ) : (
                        <div className="no-section-selected">
                            <p>Chọn một section từ danh sách bên trái để bắt đầu chỉnh sửa</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
