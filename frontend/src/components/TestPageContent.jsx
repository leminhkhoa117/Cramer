import React, { useMemo, useRef, useCallback, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import TestHeader from './TestHeader';
import HighlightableText from './HighlightableText';
import TestFooter from './TestFooter';
import { motion } from 'framer-motion';
import QuestionGroupRenderer from './QuestionGroupRenderer';
import ConfirmationModal from './ConfirmationModal';
import StartTestModal from './StartTestModal'; // This might not be needed here, TestPage handles it
import AudioPlayer from './AudioPlayer';
import ToggleSwitch from './ToggleSwitch';
import TestLayout from './TestLayout';
import useTextHighlighter from '../hooks/useTextHighlighter';
import HighlightPopup from './HighlightPopup';

import '../css/TestPage.css';
import '../css/TestHeader.css';
import '../css/TestFooter.css';
import '../css/QuestionGroup.css';
import '../css/ToggleSwitch.css';
import '../css/StartTestModal.css';
import '../css/HighlightPopup.css'; // Import the CSS for the popup

const containerVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

// This function should ideally be moved out of TestPageContent if it's not dependent on its state
// For now, keeping it here for simplicity as it was in TestPage
const groupQuestionsFromLayout = (part) => {
    if (!part || !part.questions || part.questions.length === 0) return [];

    const questionsMap = new Map(part.questions.map(q => [q.questionNumber, q]));

    // New logic for Listening tests with sectionLayout
    if (part.sectionLayout && part.sectionLayout.blocks) {
        return part.sectionLayout.blocks.map(block => {
            const blockQuestions = block.question_numbers
                .map(num => questionsMap.get(num))
                .filter(Boolean); // Filter out any undefined questions

            return {
                ...block, // Spread all properties from the block (block_type, content, etc.)
                questions: blockQuestions,
                startNum: blockQuestions[0]?.questionNumber,
            };
        });
    }

    // Fallback logic for Reading tests or old data structure
    const uniqueQuestions = Array.from(new Map(part.questions.map(q => [q.id, q])).values());
    const groups = [];
    if (uniqueQuestions.length === 0) return groups;

    let currentGroup = { 
        type: uniqueQuestions[0].questionType, 
        questions: [uniqueQuestions[0]], 
        startNum: uniqueQuestions[0].questionNumber,
        partNumber: part.partNumber 
    };
    for (let i = 1; i < uniqueQuestions.length; i++) {
        const q = uniqueQuestions[i];
        if (q.questionType === currentGroup.type) {
            currentGroup.questions.push(q);
        } else {
            groups.push(currentGroup);
            currentGroup = { 
                type: q.questionType, 
                questions: [q], 
                startNum: q.questionNumber,
                partNumber: part.partNumber 
            };
        }
    }
    groups.push(currentGroup);
    return groups;
};


const TestPageContent = ({
    testStatus, setTestStatus, testData, attempt, answers, setAnswers,
    loading, error, isConfirmModalOpen, setIsConfirmModalOpen,
    displayPartIndex, setDisplayPartIndex, readingTimeLeft,
    audioPlayerRefs, isAutoplay, setIsAutoplay, setActiveAudioIndex,
    source, testNum, skill, navigate, handleFinalSubmit
}) => {
    const [isExitModalOpen, setIsExitModalOpen] = useState(false);
    // --- Highlighting State ---
    const highlightContainerRef = useRef(null); // Create ref for highlightable area
    const { popupPosition, applyHighlight, clearHighlight } = useTextHighlighter(highlightContainerRef); // Use the hook

    const isListeningTest = skill === 'listening';

    // --- Audio Logic ---
    const handleAudioEnded = useCallback((endedIndex) => {
        if (isAutoplay && endedIndex < testData.length - 1) {
            setActiveAudioIndex(endedIndex + 1);
        }
    }, [isAutoplay, testData.length, setActiveAudioIndex]); // Added setActiveAudioIndex to dependencies

    // --- Memoized Data ---
    const allQuestions = useMemo(() => testData.flatMap(part => part.questions), [testData]);
    const displayedPart = useMemo(() => testData[displayPartIndex] || null, [testData, displayPartIndex]);
    const questionGroups = useMemo(() => displayedPart ? groupQuestionsFromLayout(displayedPart) : [], [displayedPart]);

    // --- Dynamic Layout Logic ---
    const showLeftPanel = useMemo(() => {
        if (!isListeningTest) return true; // Always show for Reading
        if (displayedPart?.displayContentUrl) return true; // Show for listening if URL exists
        return false; // Default for listening is hidden
    }, [isListeningTest, displayedPart]);

    const leftPanelContent = useMemo(() => {
        if (isListeningTest) {
            if (displayedPart?.displayContentUrl) {
                return <img src={displayedPart.displayContentUrl} alt="Test visual aid" className="listening-visual-content" />;
            }
            // The panel is collapsed, so this won't be visible, but it's good practice to have a fallback.
            return null; 
        }
        // Reading test logic
        if (displayedPart) {
            return (
                <>
                    <h2 className="passage-title">{`Reading Passage ${displayedPart.partNumber}`}</h2>
                    <p className="passage-instructions">You should spend about 20 minutes on Questions {displayedPart.questions[0].questionNumber}–{displayedPart.questions[displayedPart.questions.length - 1].questionNumber}, which are based on Reading Passage {displayedPart.partNumber} below.</p>
                    <HighlightableText text={displayedPart.passageText.replace(/\n/g, '<br />')} contentId={`passage-${displayedPart.id}`} />
                </>
            );
        }
        return null;
    }, [isListeningTest, displayedPart]);

    // --- Handlers ---
    const handleAnswerChange = (questionId, answerValue) => {
        setAnswers(prev => ({ ...prev, [questionId]: answerValue }));
    };

    const handleQuestionSelect = (questionNumber) => {
        const question = allQuestions.find(q => q.questionNumber === questionNumber);
        if (!question) return;
        const partIndex = testData.findIndex(part => part.id === question.sectionId);
        if (partIndex !== -1 && partIndex !== displayPartIndex) {
            setDisplayPartIndex(partIndex);
        }
        setTimeout(() => {
            const element = document.getElementById(`q-block-${question.id}`);
            if (element) element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 100);
    };

    // --- Render Logic ---
    if (testStatus === 'pending') {
        return (
            <StartTestModal
                isOpen={true}
                onConfirm={() => setTestStatus('running')}
                onClose={() => navigate('/courses')}
                skill={skill}
            />
        );
    }

    if (loading && !attempt) return <div className="loading-screen">Loading test...</div>;
    if (error) return <div className="error-message">{error}</div>;
    if (!displayedPart) return <div className="loading-screen">No test data found.</div>;

    const handleExitRequest = useCallback(() => setIsExitModalOpen(true), []);
    const handleExitConfirm = useCallback(() => {
        setIsExitModalOpen(false);
        navigate('/courses');
    }, [navigate]);

    return (
        <div className={`test-page-wrapper ${isListeningTest ? 'listening-test-active' : ''}`}>
            <TestHeader 
                testName={`IELTS ${skill.charAt(0).toUpperCase() + skill.slice(1)} Test - ${source.toUpperCase()} Test ${testNum}`}
                timeLeft={isListeningTest ? null : readingTimeLeft}
                onSubmit={() => setIsConfirmModalOpen(true)}
                onExit={handleExitRequest}
            />

            {isListeningTest && (
                <div className="listening-controls-container">
                    <div className="audio-players-wrapper">
                        {testData.map((part, index) => (
                            <AudioPlayer
                                key={part.id}
                                ref={el => audioPlayerRefs.current[index] = el}
                                index={index}
                                audioUrl={part.audioUrl}
                                onEnded={handleAudioEnded}
                            />
                        ))}
                    </div>
                    <ToggleSwitch
                        id="autoplay-toggle"
                        checked={isAutoplay}
                        onChange={setIsAutoplay}
                        label="Tự động phát"
                    />
                </div>
            )}

            <TestLayout showLeftPanel={showLeftPanel} leftPanelContent={leftPanelContent} highlightContainerRef={highlightContainerRef}>
                <div className="questions-column">
                    <motion.div
                        className="questions-container"
                        key={displayPartIndex}
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
            </TestLayout>
            <TestFooter
                testData={testData}
                answers={answers}
                onQuestionSelect={handleQuestionSelect}
                onPartSelect={setDisplayPartIndex}
                currentPartIndex={displayPartIndex}
            />

            <ConfirmationModal
                isOpen={isConfirmModalOpen}
                onClose={() => setIsConfirmModalOpen(false)}
                onConfirm={handleFinalSubmit}
                title="Xác nhận nộp bài"
            >
                <p>Bạn có chắc chắn muốn nộp bài không? Hành động này không thể hoàn tác.</p>
            </ConfirmationModal>

            <ConfirmationModal
                isOpen={isExitModalOpen}
                onClose={() => setIsExitModalOpen(false)}
                onConfirm={handleExitConfirm}
                title="Thoát bài thi"
            >
                <p>Bạn sẽ mất tiến độ hiện tại. Bạn vẫn muốn thoát chứ?</p>
            </ConfirmationModal>

            <HighlightPopup
                x={popupPosition.x}
                y={popupPosition.y}
                visible={popupPosition.visible}
                applyHighlight={applyHighlight}
                clearHighlight={clearHighlight}
            />
        </div>
    );
};

export default TestPageContent;
