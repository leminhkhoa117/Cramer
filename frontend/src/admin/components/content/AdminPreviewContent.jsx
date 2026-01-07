/**
 * AdminPreviewContent - Reuses test-taking UI for admin preview
 * 
 * Simplified version of TestPageContent for admin preview:
 * - Uses same TestLayout (2-column resizable panels)
 * - Uses same QuestionGroupRenderer
 * - Uses same TestFooter
 * - Removed: Timer, Submit/Exit, stores, modals, audio autoplay
 * - Read-only mode: for display purposes only
 * 
 * Admin Features:
 * - Answer Key Toggle: Show/hide correct answers for all questions
 * - Click-to-Edit: Click on a question to open the question editor
 */

import React, { useMemo, useRef, useCallback, useState } from 'react';
import { motion } from 'framer-motion';
import { FiEye, FiEyeOff, FiEdit3 } from 'react-icons/fi';
import TestLayout from '../../../components/TestLayout';
import QuestionGroupRenderer from '../../../components/QuestionGroupRenderer';
import TestFooter from '../../../components/TestFooter';
import HighlightableText from '../../../components/HighlightableText';
import { HighlightProvider } from '../../../contexts/HighlightContext';

// Import user CSS for base styles (same as TestPageContent)
import '../../../css/test-page.css';
import '../../../css/test-header.css';
import '../../../css/test-footer.css';
import '../../../css/question-group.css';

// Admin override styles
import '../../css/components/admin-preview.css';

const containerVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

/**
 * AdminQuestionWrapper - Wraps each question with click-to-edit and answer display
 */
const AdminQuestionWrapper = ({ question, showAnswer, onEdit, children }) => {
    const handleClick = useCallback((e) => {
        // Don't trigger edit if clicking on interactive elements
        if (e.target.closest('input, select, button, textarea, label')) {
            return;
        }
        onEdit(question);
    }, [question, onEdit]);

    return (
        <div
            className="admin-question-wrapper"
            onClick={handleClick}
            title="Nhấn để chỉnh sửa câu hỏi"
        >
            <div className="admin-question-edit-hint">
                <FiEdit3 size={12} />
                <span>Chỉnh sửa</span>
            </div>
            {children}
            {showAnswer && question.correctAnswer && (
                <div className="admin-answer-key">
                    <span className="admin-answer-label">Đáp án:</span>
                    <span className="admin-answer-value">{question.correctAnswer}</span>
                </div>
            )}
        </div>
    );
};

/**
 * AdminQuestionGroupWrapper - Wraps question groups to add edit functionality to each question
 */
const AdminQuestionGroupWrapper = ({ group, showAnswers, onQuestionEdit, onAnswerChange, answers, skill }) => {
    // Extract questions from the group
    const questionsWithWrapper = useMemo(() => {
        return group.questions || [];
    }, [group.questions]);

    return (
        <div className="admin-question-group-wrapper">
            <QuestionGroupRenderer
                group={group}
                onAnswerChange={onAnswerChange}
                answers={answers}
                skill={skill}
            />
            {/* Overlay answer keys if showAnswers is true */}
            {showAnswers && (
                <div className="admin-answers-overlay">
                    {questionsWithWrapper.map(q => (
                        <div
                            key={q.id}
                            className="admin-answer-item"
                            data-question-id={q.id}
                            onClick={() => onQuestionEdit(q)}
                        >
                            <div className="admin-answer-header">
                                <span className="admin-answer-qnum">Q{q.questionNumber}</span>
                                <span className="admin-answer-value">{q.correctAnswer || '—'}</span>
                                <button
                                    className="admin-answer-edit-btn"
                                    title="Chỉnh sửa câu hỏi"
                                    onClick={(e) => { e.stopPropagation(); onQuestionEdit(q); }}
                                >
                                    <FiEdit3 size={12} />
                                </button>
                            </div>
                            {q.explanation && (() => {
                                // Parse explanation if it's a JSON string
                                let exp = q.explanation;
                                if (typeof exp === 'string') {
                                    try {
                                        exp = JSON.parse(exp);
                                    } catch {
                                        // Not valid JSON, render as plain text
                                        return <div className="admin-answer-explanation">{exp}</div>;
                                    }
                                }
                                // Now exp is an object
                                return (
                                    <div className="admin-answer-explanation">
                                        {exp.quote && (
                                            <div className="explanation-quote">
                                                <strong>Trích dẫn:</strong> "{exp.quote}"
                                            </div>
                                        )}
                                        {exp.detail && (
                                            <div className="explanation-detail">
                                                <strong>Giải thích:</strong> {exp.detail}
                                            </div>
                                        )}
                                        {exp.strategy && (
                                            <div className="explanation-strategy">
                                                <strong>Chiến lược:</strong> {exp.strategy}
                                            </div>
                                        )}
                                        {exp.contributor && (
                                            <div className="explanation-contributor">
                                                <strong>Nguồn:</strong> {exp.contributor}
                                            </div>
                                        )}
                                    </div>
                                );
                            })()}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

/**
 * Group questions by type - EXACT COPY from TestPageContent
 */
const groupQuestionsFromLayout = (part) => {
    if (!part || !part.questions || part.questions.length === 0) return [];

    const questionsMap = new Map(part.questions.map(q => [q.questionNumber, q]));

    // New logic for Listening tests with sectionLayout
    if (part.sectionLayout && part.sectionLayout.blocks && Array.isArray(part.sectionLayout.blocks)) {
        const groups = part.sectionLayout.blocks
            .map((block, blockIndex) => {
                // Safely get question_numbers array, fallback to empty array
                const questionNumbers = block.question_numbers || [];
                const blockQuestions = questionNumbers
                    .map(num => questionsMap.get(num))
                    .filter(Boolean);

                // Skip blocks with no valid questions
                if (blockQuestions.length === 0) {
                    console.warn('[AdminPreviewContent] Skipping block with no matching questions:', block);
                    return null;
                }

                return {
                    ...block,
                    questions: blockQuestions,
                    startNum: blockQuestions[0]?.questionNumber,
                    uniqueGroupId: `part${part.id}-block${blockIndex}-${block.id || blockIndex}`,
                    partId: part.id,
                };
            })
            .filter(Boolean); // Remove null entries

        // If we got valid groups from sectionLayout, use them
        // Otherwise, fall through to fallback grouping
        if (groups.length > 0) {
            return groups;
        }
        console.warn('[AdminPreviewContent] All sectionLayout blocks were invalid, using fallback grouping');
    }

    // Fallback logic for Reading tests or old data structure
    const uniqueQuestions = Array.from(new Map(part.questions.map(q => [q.id, q])).values());
    const groups = [];
    if (uniqueQuestions.length === 0) return groups;

    let groupIndex = 0;
    let currentGroup = {
        type: uniqueQuestions[0].questionType,
        questions: [uniqueQuestions[0]],
        startNum: uniqueQuestions[0].questionNumber,
        partNumber: part.partNumber,
        uniqueGroupId: `part${part.id}-group${groupIndex}`,
        partId: part.id,
    };

    for (let i = 1; i < uniqueQuestions.length; i++) {
        const q = uniqueQuestions[i];
        if (q.questionType === currentGroup.type) {
            currentGroup.questions.push(q);
        } else {
            groups.push(currentGroup);
            groupIndex++;
            currentGroup = {
                type: q.questionType,
                questions: [q],
                startNum: q.questionNumber,
                partNumber: part.partNumber,
                uniqueGroupId: `part${part.id}-group${groupIndex}`,
                partId: part.id,
            };
        }
    }
    groups.push(currentGroup);
    return groups;
};

export default function AdminPreviewContent({
    sections = [],
    questions = [],
    activePartIndex = 0,
    skill = 'reading',
    onPartSelect = () => { },
    onQuestionSelect = () => { },
    onQuestionEdit = () => { },     // Called when question is clicked for editing
    showAnswers = false,             // Controlled answer key state
    onToggleAnswers = () => { },     // Toggle answer key callback
    sectionName = '',                // Section name for toolbar
    toolbarActions = {},             // { onPassageClick, onAudioClick, onAssetClick, onLayoutClick }
}) {
    const highlightContainerRef = useRef(null);
    const isListeningTest = skill === 'listening';
    const isWritingTest = skill === 'writing';
    const [showTranscript, setShowTranscript] = useState(false);
    // Internal state for answer toggle if not controlled externally
    const [internalShowAnswers, setInternalShowAnswers] = useState(false);

    // Support both controlled (parent passes showAnswers) and uncontrolled (internal state) modes
    // Use internal state when showAnswers is falsy (default value)
    const answersVisible = showAnswers || internalShowAnswers;
    const handleToggleAnswers = useCallback(() => {
        // If parent provides toggle callback, call it; otherwise toggle internal state
        if (showAnswers !== undefined && showAnswers !== false) {
            onToggleAnswers();
        } else {
            setInternalShowAnswers(prev => !prev);
        }
    }, [showAnswers, onToggleAnswers]);

    // Build testData format (same as TestPageContent expects)
    // Each part has questions array attached, sorted by partNumber
    const testData = useMemo(() => {
        return [...sections]
            .sort((a, b) => a.partNumber - b.partNumber)
            .map(section => ({
                ...section,
                questions: questions
                    .filter(q => q.sectionId === section.id)
                    .sort((a, b) => a.questionNumber - b.questionNumber)
            }));
    }, [sections, questions]);

    // Current displayed part
    const displayedPart = useMemo(() => testData[activePartIndex] || null, [testData, activePartIndex]);

    // Question groups for rendering
    const questionGroups = useMemo(() => displayedPart ? groupQuestionsFromLayout(displayedPart) : [], [displayedPart]);

    // All questions (for footer navigation)
    const allQuestions = useMemo(() => testData.flatMap(part => part.questions), [testData]);

    // Dynamic layout - EXACT logic from TestPageContent
    const showLeftPanel = useMemo(() => {
        if (!isListeningTest) return true; // Always show for Reading
        if (displayedPart?.displayContentUrl) return true;
        if (showTranscript && displayedPart?.passageText) return true;
        return false;
    }, [isListeningTest, displayedPart, showTranscript]);

    const leftPanelContent = useMemo(() => {
        if (isListeningTest) {
            if (showTranscript && displayedPart?.passageText) {
                return (
                    <>
                        {displayedPart?.displayContentUrl && (
                            <img src={displayedPart.displayContentUrl} alt="Test visual aid" className="listening-visual-content" />
                        )}
                        <h2 className="passage-title">Listening Transcript</h2>
                        <HighlightableText
                            text={displayedPart.passageText.replace(/\n/g, '<br />')}
                            contentId={`transcript-${displayedPart.id}`}
                        />
                    </>
                );
            }
            if (displayedPart?.displayContentUrl) {
                return <img src={displayedPart.displayContentUrl} alt="Test visual aid" className="listening-visual-content" />;
            }
            return null;
        }

        if (isWritingTest) {
            return (
                <>
                    <h2 className="passage-title">{`Writing Task ${displayedPart?.partNumber || 1}`}</h2>
                    <p className="passage-instructions">
                        {displayedPart?.partNumber === 2
                            ? 'You should spend about 40 minutes on this task.'
                            : 'You should spend about 20 minutes on this task.'}
                    </p>
                    {displayedPart?.passageText ? (
                        <HighlightableText
                            text={displayedPart.passageText.replace(/\n/g, '<br />')}
                            contentId={`writing-${displayedPart.id}`}
                        />
                    ) : (
                        <div className="no-passage">Chưa có nội dung đề bài</div>
                    )}
                    {displayedPart?.partNumber === 1 && displayedPart?.displayContentUrl && (
                        <div className="writing-task-image">
                            <img src={displayedPart.displayContentUrl} alt="Task 1 Figure" />
                        </div>
                    )}
                    {displayedPart?.partNumber === 1 && displayedPart?.imageDescription && (
                        <div className="writing-task-description">{displayedPart.imageDescription}</div>
                    )}
                </>
            );
        }

        // Reading test logic - EXACT from TestPageContent
        if (displayedPart && displayedPart.passageText) {
            return (
                <>
                    <h2 className="passage-title">{`Reading Passage ${displayedPart.partNumber}`}</h2>
                    {displayedPart.questions && displayedPart.questions.length > 0 && (
                        <p className="passage-instructions">
                            You should spend about 20 minutes on Questions {displayedPart.questions[0].questionNumber}–{displayedPart.questions[displayedPart.questions.length - 1].questionNumber}, which are based on Reading Passage {displayedPart.partNumber} below.
                        </p>
                    )}
                    <HighlightableText
                        text={displayedPart.passageText.replace(/\n/g, '<br />')}
                        contentId={`passage-${displayedPart.id}`}
                    />
                </>
            );
        }
        return <div className="no-passage">Chưa có nội dung passage</div>;
    }, [isListeningTest, isWritingTest, displayedPart, showTranscript]);

    const writingWordCounts = useMemo(() => {
        if (!isWritingTest) return {};
        const counts = {};
        testData.forEach(part => {
            const min = part.partNumber === 1 ? 150 : 250;
            counts[part.partNumber] = { current: 0, min };
        });
        return counts;
    }, [isWritingTest, testData]);

    // No-op handlers (preview mode)
    const handleAnswerChange = useCallback(() => { }, []);
    const answers = {};

    // Question select handler - EXACT from TestPageContent
    const handleQuestionSelect = useCallback((questionNumber) => {
        const question = allQuestions.find(q => q.questionNumber === questionNumber);
        if (!question) return;
        const partIndex = testData.findIndex(part => part.id === question.sectionId);
        if (partIndex !== -1 && partIndex !== activePartIndex) {
            onPartSelect(partIndex);
        }
        setTimeout(() => {
            const element = document.getElementById(`q-block-${question.id}`);
            if (element) element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 100);
    }, [allQuestions, testData, activePartIndex, onPartSelect]);

    if (!displayedPart) {
        return (
            <div className="admin-preview-wrapper admin-preview-empty">
                <p>Chọn một section để xem preview</p>
            </div>
        );
    }

    return (
        <HighlightProvider>
            <div className={`admin-preview-wrapper ${isListeningTest ? 'listening-test-active' : ''}`}>
                {/* Listening audio player */}
                {isListeningTest && displayedPart?.audioUrl && (
                    <div className="admin-preview-audio">
                        <audio controls src={displayedPart.audioUrl} />
                    </div>
                )}

                {/* Unified Admin Toolbar - Section name + Edit actions + View toggles */}
                <div className="admin-preview-toolbar">
                    {/* Left: Section name and edit actions */}
                    <div className="admin-toolbar__left">
                        {sectionName && <span className="admin-toolbar__section-name">{sectionName}</span>}
                        {toolbarActions.onPassageClick && (
                            <button
                                className="admin-toolbar__btn"
                                onClick={toolbarActions.onPassageClick}
                                title="Nội dung"
                            >
                                Nội dung
                            </button>
                        )}
                        {toolbarActions.onAudioClick && (
                            <button
                                className="admin-toolbar__btn"
                                onClick={toolbarActions.onAudioClick}
                                title="Audio"
                            >
                                Audio
                            </button>
                        )}
                        {toolbarActions.onAssetClick && (
                            <button
                                className="admin-toolbar__btn"
                                onClick={toolbarActions.onAssetClick}
                                title="Asset"
                            >
                                Asset
                            </button>
                        )}
                        {toolbarActions.onLayoutClick && (
                            <button
                                className="admin-toolbar__btn"
                                onClick={toolbarActions.onLayoutClick}
                                title="Layout"
                            >
                                Layout
                            </button>
                        )}
                    </div>

                    {/* Right: View toggles */}
                    <div className="admin-toolbar__right">
                        {/* Answer Key Toggle - available for all skills except writing */}
                        {!isWritingTest && (
                            <button
                                className={`admin-preview-toggle ${answersVisible ? 'active' : ''}`}
                                onClick={handleToggleAnswers}
                                type="button"
                                title={answersVisible ? 'Ẩn đáp án' : 'Hiện đáp án'}
                            >
                                {answersVisible ? <FiEyeOff size={14} /> : <FiEye size={14} />}
                                <span>Đáp án</span>
                            </button>
                        )}

                        {/* Transcript Toggle - only for listening */}
                        {isListeningTest && (
                            <button
                                className={`admin-preview-toggle ${showTranscript ? 'active' : ''}`}
                                onClick={() => setShowTranscript(prev => !prev)}
                                type="button"
                            >
                                Transcript
                            </button>
                        )}
                    </div>
                </div>

                {/* Main 2-column layout - EXACT structure from TestPageContent */}
                <TestLayout
                    showLeftPanel={showLeftPanel}
                    leftPanelContent={leftPanelContent}
                    highlightContainerRef={highlightContainerRef}
                >
                    {isWritingTest ? (
                        <div className="questions-column writing-editor-panel">
                            <div className="writing-editor-header">
                                <h3>Your Response</h3>
                                <div className="writing-word-counter">
                                    <span className="count">0</span>
                                    <span className="separator">/</span>
                                    <span className="min">{displayedPart?.partNumber === 1 ? 150 : 250} words</span>
                                </div>
                            </div>
                            <textarea
                                className="writing-textarea"
                                placeholder="Preview mode: writing response area"
                                readOnly
                            />
                        </div>
                    ) : (
                        <div className="questions-column">
                            <motion.div
                                className="questions-container"
                                key={activePartIndex}
                                variants={containerVariants}
                                initial="hidden"
                                animate="visible"
                            >
                                {questionGroups.map((group, index) => (
                                    <AdminQuestionGroupWrapper
                                        key={index}
                                        group={group}
                                        showAnswers={answersVisible}
                                        onQuestionEdit={onQuestionEdit}
                                        onAnswerChange={handleAnswerChange}
                                        answers={answers}
                                        skill={skill}
                                    />
                                ))}
                            </motion.div>
                        </div>
                    )}
                </TestLayout>

                {/* Footer - EXACT same component as TestPageContent */}
                <TestFooter
                    testData={testData}
                    answers={answers}
                    onQuestionSelect={handleQuestionSelect}
                    onPartSelect={onPartSelect}
                    currentPartIndex={activePartIndex}
                    mode={isWritingTest ? 'wordCount' : 'questions'}
                    wordCounts={writingWordCounts}
                    partLabel={isWritingTest ? 'Task' : 'Part'}
                />
            </div>
        </HighlightProvider>
    );
}
