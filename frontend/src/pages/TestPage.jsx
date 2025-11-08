import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getFullSection } from '../api/backendApi';
import QuestionRenderer from '../components/QuestionRenderer'; // Import the new component
import '../css/TestPage.css';

// Helper function to group consecutive questions of the same type
const groupQuestions = (questions) => {
    if (!questions || questions.length === 0) {
        return [];
    }

    const groups = [];
    let currentGroup = {
        type: questions[0].questionType,
        questions: [questions[0]],
        startNum: questions[0].questionNumber,
    };

    for (let i = 1; i < questions.length; i++) {
        const question = questions[i];
        // Group SUMMARY_COMPLETION_OPTIONS together regardless of intermittent questions
        if (question.questionType === 'SUMMARY_COMPLETION_OPTIONS' && currentGroup.type === 'SUMMARY_COMPLETION_OPTIONS') {
             currentGroup.questions.push(question);
        } else if (question.questionType === currentGroup.type) {
            currentGroup.questions.push(question);
        } else {
            groups.push(currentGroup);
            currentGroup = {
                type: question.questionType,
                questions: [question],
                startNum: question.questionNumber,
            };
        }
    }
    groups.push(currentGroup);
    return groups;
};


const TestPage = () => {
    const { sectionId } = useParams();
    const [section, setSection] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [questionGroups, setQuestionGroups] = useState([]);

    useEffect(() => {
        const fetchSectionData = async () => {
            try {
                setLoading(true);
                const data = await getFullSection(sectionId);
                setSection(data);
                setQuestionGroups(groupQuestions(data.questions));
                setError(null);
            } catch (err) {
                setError('Failed to load test data. Please try again later.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchSectionData();
    }, [sectionId]);

    const renderGroupInstructions = (group) => {
        const start = group.startNum;
        const end = group.questions[group.questions.length - 1].questionNumber;
        const range = start === end ? start : `${start}-${end}`;

        switch (group.type) {
            case 'FILL_IN_BLANK':
                return <p><strong>Questions {range}:</strong> Complete the notes below. Choose <strong>ONE WORD ONLY</strong> from the passage for each answer.</p>;
            case 'TRUE_FALSE_NOT_GIVEN':
                 return <p><strong>Questions {range}:</strong> Do the following statements agree with the information given in the reading passage?</p>;
            case 'YES_NO_NOT_GIVEN':
                 return <p><strong>Questions {range}:</strong> Do the following statements agree with the views of the writer?</p>;
            case 'SUMMARY_COMPLETION_OPTIONS':
                const options = group.questions[0]?.questionContent?.options || [];
                return (
                    <>
                        <p><strong>Questions {range}:</strong> Complete the summary using the list of words, A-J, below.</p>
                        <div className="options-box">
                            {options.map(opt => <span key={opt}>{opt}</span>)}
                        </div>
                    </>
                );
            case 'MATCHING_INFORMATION':
                return <p><strong>Questions {range}:</strong> The reading passage has several paragraphs, A-H. Which paragraph contains the following information?</p>
            default:
                return <p><strong>Questions {range}</strong></p>;
        }
    };

    if (loading) {
        return <div className="loading-screen">Loading test...</div>;
    }

    if (error) {
        return <div className="error-message">{error}</div>;
    }

    if (!section) {
        return <div className="loading-screen">No section data found.</div>;
    }

    return (
        <div className="test-page-container">
            <div className="passage-container">
                <h1 className="passage-title">{`Reading Passage ${section.partNumber}`}</h1>
                <p className="passage-instructions">You should spend about 20 minutes on the questions below, which are based on this reading passage.</p>
                <div
                    className="passage-content"
                    dangerouslySetInnerHTML={{ __html: section.passageText.replace(/\n/g, '<br />') }}
                />
            </div>
            <div className="questions-container">
                <h1>Questions</h1>
                {questionGroups.map((group, index) => (
                    <div key={index} className="question-group">
                        <div className="group-instructions">
                            {renderGroupInstructions(group)}
                        </div>
                        {group.questions.map(question => (
                            <QuestionRenderer key={question.id} question={question} />
                        ))}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default TestPage;
