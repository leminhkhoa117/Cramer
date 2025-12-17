import React, { useState } from 'react';
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
    FiChevronRight,
    FiX
} from 'react-icons/fi';
import StatusBadge from '../../components/StatusBadge';
import { getTestById, getStatusColor } from '../../mock/mockContent';
import './TestEditorPage.css';

export default function TestEditorPage() {
    const { testId } = useParams();
    const navigate = useNavigate();
    const [activeSkill, setActiveSkill] = useState('reading');
    const [activeSection, setActiveSection] = useState(1);

    const test = getTestById(parseInt(testId));

    if (!test) {
        return (
            <div className="admin-page test-editor-page">
                <div className="not-found">
                    <h2>Không tìm thấy đề thi</h2>
                    <p>ID: {testId}</p>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={() => navigate('/admin/content/editor')}
                    >
                        Quay lại danh sách
                    </button>
                </div>
            </div>
        );
    }

    // Mock sections data
    const mockSections = {
        reading: [
            { id: 1, name: 'Passage 1', title: 'The History of Railways', questionRange: '1-13', questionCount: 13 },
            { id: 2, name: 'Passage 2', title: 'Climate Change Effects', questionRange: '14-26', questionCount: 13 },
            { id: 3, name: 'Passage 3', title: 'Modern Architecture', questionRange: '27-40', questionCount: 14 },
        ],
        listening: [
            { id: 4, name: 'Part 1', title: 'Conversation at Reception', questionRange: '1-10', questionCount: 10, hasAudio: true },
            { id: 5, name: 'Part 2', title: 'Museum Tour Guide', questionRange: '11-20', questionCount: 10, hasAudio: true },
            { id: 6, name: 'Part 3', title: 'Student Discussion', questionRange: '21-30', questionCount: 10, hasAudio: true },
            { id: 7, name: 'Part 4', title: 'Lecture on Biology', questionRange: '31-40', questionCount: 10, hasAudio: true },
        ],
        writing: [
            { id: 8, name: 'Task 1', title: 'Bar Chart Description', questionRange: 'Task 1', questionCount: 1 },
            { id: 9, name: 'Task 2', title: 'Essay: Technology in Education', questionRange: 'Task 2', questionCount: 1 },
        ],
        speaking: [
            { id: 10, name: 'Part 1', title: 'Introduction & Interview', questionRange: 'Part 1', questionCount: 4 },
            { id: 11, name: 'Part 2', title: 'Individual Long Turn', questionRange: 'Part 2', questionCount: 1 },
            { id: 12, name: 'Part 3', title: 'Two-way Discussion', questionRange: 'Part 3', questionCount: 4 },
        ],
    };

    // Mock questions for active section
    const mockQuestions = [
        { id: 1, number: 1, type: 'FILL_IN_BLANK', text: 'The ____ of London grew rapidly...', hasAnswer: true },
        { id: 2, number: 2, type: 'FILL_IN_BLANK', text: 'Railway construction required ____ workers...', hasAnswer: true },
        { id: 3, number: 3, type: 'TRUE_FALSE_NOT_GIVEN', text: 'The railway system was expensive to build.', hasAnswer: true },
        { id: 4, number: 4, type: 'TRUE_FALSE_NOT_GIVEN', text: 'All workers received fair wages.', hasAnswer: false },
        { id: 5, number: 5, type: 'MULTIPLE_CHOICE', text: 'What was the main purpose of the railway?', hasAnswer: true },
        { id: 6, number: 6, type: 'MATCHING_HEADINGS', text: 'Match the paragraph with heading', hasAnswer: true },
        { id: 7, number: 7, type: 'MATCHING_HEADINGS', text: 'Match the paragraph with heading', hasAnswer: true },
        { id: 8, number: 8, type: 'SENTENCE_COMPLETION', text: 'Complete the sentence...', hasAnswer: false },
        { id: 9, number: 9, type: 'SENTENCE_COMPLETION', text: 'Complete the sentence...', hasAnswer: true },
        { id: 10, number: 10, type: 'FILL_IN_BLANK', text: 'The engine ran on ____ power...', hasAnswer: true },
        { id: 11, number: 11, type: 'FILL_IN_BLANK', text: 'Workers came from ____ regions...', hasAnswer: true },
        { id: 12, number: 12, type: 'TRUE_FALSE_NOT_GIVEN', text: 'The railway changed society.', hasAnswer: true },
        { id: 13, number: 13, type: 'MULTIPLE_CHOICE', text: 'According to the passage...', hasAnswer: true },
    ];

    const skills = [
        { id: 'reading', label: 'Reading', icon: '📖' },
        { id: 'listening', label: 'Listening', icon: '🎧' },
        { id: 'writing', label: 'Writing', icon: '✍️' },
        { id: 'speaking', label: 'Speaking', icon: '🎤' },
    ];

    const currentSections = mockSections[activeSkill] || [];

    return (
        <div className="admin-page test-editor-page">
            {/* Editor Header */}
            <div className="editor-header">
                <div className="editor-header__left">
                    <button className="back-btn" onClick={() => navigate('/admin/content/editor')}>
                        <FiArrowLeft size={18} />
                    </button>
                    <div className="editor-header__info">
                        <span className="editor-header__breadcrumb">{test.topicName}</span>
                        <h1 className="editor-header__title">{test.name}</h1>
                    </div>
                    <StatusBadge status={test.status} variant={getStatusColor(test.status)} />
                </div>
                <div className="editor-header__actions">
                    <button className="admin-btn admin-btn--secondary">
                        <FiEye size={16} />
                        <span>Xem trước</span>
                    </button>
                    <button className="admin-btn admin-btn--secondary">
                        <FiSave size={16} />
                        <span>Lưu nháp</span>
                    </button>
                    <button className="admin-btn admin-btn--primary">
                        <FiCheck size={16} />
                        <span>Xuất bản</span>
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
                            setActiveSection(mockSections[skill.id]?.[0]?.id || 1);
                        }}
                    >
                        <span className="skill-tab__icon">{skill.icon}</span>
                        <span className="skill-tab__label">{skill.label}</span>
                        <span className={`skill-tab__status skill-tab__status--${test.skills[skill.id]?.status || 'empty'}`} />
                    </button>
                ))}
            </div>

            {/* Editor Content */}
            <div className="editor-content">
                {/* Left Panel - Section Navigator */}
                <div className="editor-sidebar">
                    <div className="editor-sidebar__header">
                        <h3>Sections</h3>
                        <button className="add-section-btn" title="Thêm Section">
                            <FiPlus size={16} />
                        </button>
                    </div>
                    <div className="section-list">
                        {currentSections.map(section => (
                            <div
                                key={section.id}
                                className={`section-item ${activeSection === section.id ? 'section-item--active' : ''}`}
                                onClick={() => setActiveSection(section.id)}
                            >
                                <div className="section-item__info">
                                    <span className="section-item__name">{section.name}</span>
                                    <span className="section-item__title">{section.title}</span>
                                </div>
                                <div className="section-item__meta">
                                    <span className="section-item__questions">
                                        {section.questionCount} câu
                                    </span>
                                    {section.hasAudio && (
                                        <span className="section-item__audio">🎵</span>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Question Navigator */}
                    <div className="question-navigator">
                        <h4>Câu hỏi</h4>
                        <div className="question-grid">
                            {mockQuestions.map(q => (
                                <button
                                    key={q.id}
                                    className={`question-btn ${q.hasAnswer ? 'question-btn--complete' : 'question-btn--incomplete'}`}
                                    title={q.type}
                                >
                                    {q.number}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Main Editor Area */}
                <div className="editor-main">
                    {/* Section Header */}
                    <div className="section-header">
                        <div className="section-header__info">
                            <h2>{currentSections.find(s => s.id === activeSection)?.name}</h2>
                            <p>{currentSections.find(s => s.id === activeSection)?.title}</p>
                        </div>
                        <div className="section-header__actions">
                            <button className="admin-btn admin-btn--secondary">
                                <FiUpload size={16} />
                                <span>Upload Passage</span>
                            </button>
                            <button className="admin-btn admin-btn--secondary">
                                <FiEdit size={16} />
                                <span>Chỉnh sửa</span>
                            </button>
                        </div>
                    </div>

                    {/* Passage Area */}
                    <div className="passage-area">
                        <div className="passage-content">
                            <h3>The History of Railways in Britain</h3>
                            <p>
                                The development of railways in Britain during the 19th century was one of the most significant
                                technological and social changes in the country's history. The first public railway, the Stockton
                                and Darlington Railway, opened in 1825 and marked the beginning of a new era in transportation.
                            </p>
                            <p>
                                The construction of railways required enormous <span className="highlight">[Answer: workforce]</span>
                                and significant capital investment. Workers, known as "navvies," came from various parts of Britain
                                and Ireland, working in difficult and often dangerous conditions.
                            </p>
                            <p className="passage-placeholder">
                                <i>Nội dung passage sẽ hiển thị ở đây...</i>
                            </p>
                        </div>
                    </div>

                    {/* Questions List */}
                    <div className="questions-area">
                        <div className="questions-header">
                            <h3>Danh sách câu hỏi</h3>
                            <button className="admin-btn admin-btn--primary">
                                <FiPlus size={16} />
                                <span>Thêm câu hỏi</span>
                            </button>
                        </div>
                        <div className="questions-list">
                            {mockQuestions.map(question => (
                                <div key={question.id} className="question-item">
                                    <div className="question-item__number">
                                        <span className={question.hasAnswer ? 'complete' : 'incomplete'}>
                                            {question.number}
                                        </span>
                                    </div>
                                    <div className="question-item__content">
                                        <span className="question-item__type">{question.type.replace(/_/g, ' ')}</span>
                                        <p className="question-item__text">{question.text}</p>
                                    </div>
                                    <div className="question-item__status">
                                        {question.hasAnswer ? (
                                            <span className="status-complete"><FiCheck size={14} /> Đã có đáp án</span>
                                        ) : (
                                            <span className="status-incomplete"><FiX size={14} /> Thiếu đáp án</span>
                                        )}
                                    </div>
                                    <div className="question-item__actions">
                                        <button className="icon-btn" title="Chỉnh sửa">
                                            <FiEdit size={16} />
                                        </button>
                                        <button className="icon-btn icon-btn--danger" title="Xóa">
                                            <FiTrash size={16} />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
