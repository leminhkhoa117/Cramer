import './../css/test-footer.css';

/**
 * QuestionButton - Button for question navigation
 */
const QuestionButton = ({ question, isAnswered, onSelect }) => (
    <button
        className={`question-nav-btn ${isAnswered ? 'answered' : ''}`}
        onClick={() => onSelect(question.questionNumber)}
    >
        {question.questionNumber}
    </button>
);

/**
 * WordCountBadge - Badge showing word count for Writing test
 */
const WordCountBadge = ({ current, min }) => {
    const isComplete = current >= min;
    return (
        <span className={`word-count-badge ${isComplete ? 'complete' : 'incomplete'}`}>
            {current} / {min} từ
        </span>
    );
};

/**
 * TestFooter - Unified footer for all test types
 * 
 * @param {Object} props
 * @param {Array} props.testData - Array of parts/sections
 * @param {Object} props.answers - User answers (for questions mode)
 * @param {Function} props.onQuestionSelect - Handler for question click (questions mode)
 * @param {Function} props.onPartSelect - Handler for part/task click
 * @param {number} props.currentPartIndex - Currently active part index
 * @param {'questions'|'wordCount'} props.mode - Footer mode (default: 'questions')
 * @param {Object} props.wordCounts - Word counts per part { partNumber: { current, min } }
 * @param {'Part'|'Task'} props.partLabel - Label for parts (default: 'Part')
 */
export default function TestFooter({
    testData,
    answers = {},
    onQuestionSelect,
    onPartSelect,
    currentPartIndex,
    mode = 'questions',
    wordCounts = {},
    partLabel = 'Part'
}) {
    if (!testData || testData.length === 0) {
        return null;
    }

    const isWritingMode = mode === 'wordCount';

    return (
        <footer className={`test-page-footer ${isWritingMode ? 'writing-footer' : ''}`}>
            {testData.map((part, index) => (
                <div
                    key={part.id || part.partNumber}
                    className={`footer-part-section ${index === currentPartIndex ? 'active' : ''}`}
                    onClick={isWritingMode ? () => onPartSelect(index) : undefined}
                >
                    <h3
                        className="footer-part-title"
                        onClick={!isWritingMode ? () => onPartSelect(index) : undefined}
                    >
                        {partLabel} {part.partNumber}
                    </h3>

                    {/* Questions mode: show question buttons */}
                    {mode === 'questions' && part.questions && (
                        <div className="footer-question-grid">
                            {part.questions.map(q => (
                                <QuestionButton
                                    key={q.id}
                                    question={q}
                                    isAnswered={!!answers[q.id]}
                                    onSelect={onQuestionSelect}
                                />
                            ))}
                        </div>
                    )}

                    {/* Word count mode: show word count badge */}
                    {isWritingMode && wordCounts[part.partNumber] && (
                        <div className="writing-task-info">
                            <WordCountBadge
                                current={wordCounts[part.partNumber].current}
                                min={wordCounts[part.partNumber].min}
                            />
                        </div>
                    )}
                </div>
            ))}
        </footer>
    );
}
