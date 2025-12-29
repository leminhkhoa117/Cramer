/**
 * AdminPreviewContent - Reuses test-taking UI for admin preview
 * 
 * Simplified version of TestPageContent for admin preview:
 * - Uses same TestLayout (2-column resizable panels)
 * - Uses same QuestionGroupRenderer
 * - Uses same TestFooter
 * - Removed: Timer, Submit/Exit, stores, modals, audio autoplay
 * - Read-only mode: for display purposes only
 */

import React, { useMemo, useRef, useCallback, useState } from 'react';
import { motion } from 'framer-motion';
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
 * Group questions by type - EXACT COPY from TestPageContent
 */
const groupQuestionsFromLayout = (part) => {
    if (!part || !part.questions || part.questions.length === 0) return [];

    const questionsMap = new Map(part.questions.map(q => [q.questionNumber, q]));

    // New logic for Listening tests with sectionLayout
    if (part.sectionLayout && part.sectionLayout.blocks) {
        return part.sectionLayout.blocks.map((block, blockIndex) => {
            const blockQuestions = block.question_numbers
                .map(num => questionsMap.get(num))
                .filter(Boolean);

            return {
                ...block,
                questions: blockQuestions,
                startNum: blockQuestions[0]?.questionNumber,
                uniqueGroupId: `part${part.id}-block${blockIndex}-${block.id || blockIndex}`,
                partId: part.id,
            };
        });
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
}) {
    const highlightContainerRef = useRef(null);
    const isListeningTest = skill === 'listening';
    const isWritingTest = skill === 'writing';
    const [showTranscript, setShowTranscript] = useState(false);

    // Build testData format (same as TestPageContent expects)
    // Each part has questions array attached
    const testData = useMemo(() => {
        return sections.map(section => ({
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

                {isListeningTest && (
                    <div className="admin-preview-toolbar">
                        <button
                            className={`admin-preview-toggle ${showTranscript ? 'active' : ''}`}
                            onClick={() => setShowTranscript(prev => !prev)}
                            type="button"
                        >
                            Transcript
                        </button>
                    </div>
                )}

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
                                    <QuestionGroupRenderer
                                        key={index}
                                        group={group}
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
