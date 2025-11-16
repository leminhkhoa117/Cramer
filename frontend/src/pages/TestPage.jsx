import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { testApi, testAttemptApi } from '../api/backendApi';
import StartTestModal from '../components/StartTestModal'; // Re-import StartTestModal
import { HighlightProvider } from '../contexts/HighlightContext';
import TestPageContent from '../components/TestPageContent'; // Import the new component

import '../css/TestPage.css';

const TestPage = () => {
    const { source, testNum, skill } = useParams();
    const navigate = useNavigate();
    
    // --- Core State ---
    const [testStatus, setTestStatus] = useState('pending'); // pending, running, submitted
    const [testData, setTestData] = useState([]);
    const [attempt, setAttempt] = useState(null);
    const [answers, setAnswers] = useState({});
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

    // --- UI State ---
    const [displayPartIndex, setDisplayPartIndex] = useState(0);
    
    // --- Reading Test State ---
    const [readingTimeLeft, setReadingTimeLeft] = useState(3600); 
    
    // --- Listening Test State ---
    const audioPlayerRefs = useRef([]);
    const [isAutoplay, setIsAutoplay] = useState(true);
    const [activeAudioIndex, setActiveAudioIndex] = useState(-1); // -1: none, 0-3: part 1-4

    // --- Submission Logic ---
    const handleFinalSubmit = useCallback(async () => {
        if (!attempt) return;
        setIsConfirmModalOpen(false);
        try {
            setLoading(true);
            const result = await testAttemptApi.submitAttempt(attempt.id, answers);
            navigate(`/test/review/${result.data.attemptId}`);
        } catch (err) {
            setError('Failed to submit test. Please try again.');
        } finally {
            setLoading(false);
        }
    }, [attempt, answers, navigate]);

    // --- Timer for Reading Test ---
    useEffect(() => {
        if (testStatus !== 'running' || skill === 'listening') return; // Use skill directly
        const timer = setInterval(() => {
            setReadingTimeLeft(prevTime => {
                if (prevTime <= 1) {
                    clearInterval(timer);
                    handleFinalSubmit();
                    return 0;
                }
                return prevTime - 1;
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [testStatus, skill, handleFinalSubmit]); // Update dependencies

    // --- Data Fetching ---
    useEffect(() => {
        if (testStatus !== 'running') return;

        const fetchAndStartTest = async () => {
            try {
                setLoading(true);
                const [data, attemptData] = await Promise.all([
                    testApi.getFullTest(source, testNum, skill),
                    testAttemptApi.startAttempt(source, testNum, skill)
                ]);
                setTestData(data);
                setAttempt(attemptData.data);
                if (skill === 'listening' && data.length > 0) { // Use skill directly
                    audioPlayerRefs.current = data.map((_, i) => audioPlayerRefs.current[i] ?? React.createRef());
                }
                setError(null);
                if (skill === 'listening' && isAutoplay) { // Use skill directly
                    setActiveAudioIndex(0); // Trigger first audio
                }
            } catch (err) {
                setError('Failed to load test data. Please try again later.');
            } finally {
                setLoading(false);
            }
        };
        fetchAndStartTest();
    }, [testStatus, source, testNum, skill, isAutoplay]); // Update dependencies

    // --- Audio Logic ---
    useEffect(() => {
        if (testStatus !== 'running' || skill !== 'listening' || activeAudioIndex === -1) return; // Use skill directly
        
        const player = audioPlayerRefs.current[activeAudioIndex];
        if (player) {
            player.play();
        }
    }, [activeAudioIndex, testStatus, skill]); // Update dependencies

    // --- Render Logic ---

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
    if (testData.length === 0) return <div className="loading-screen">No test data found.</div>; // Changed from !displayedPart

    return (
        <HighlightProvider>
            <TestPageContent
                testStatus={testStatus}
                setTestStatus={setTestStatus}
                testData={testData}
                attempt={attempt}
                answers={answers}
                setAnswers={setAnswers}
                loading={loading}
                error={error}
                isConfirmModalOpen={isConfirmModalOpen}
                setIsConfirmModalOpen={setIsConfirmModalOpen}
                displayPartIndex={displayPartIndex}
                setDisplayPartIndex={setDisplayPartIndex}
                readingTimeLeft={readingTimeLeft}
                audioPlayerRefs={audioPlayerRefs}
                isAutoplay={isAutoplay}
                setIsAutoplay={setIsAutoplay}
                setActiveAudioIndex={setActiveAudioIndex}
                source={source}
                testNum={testNum}
                skill={skill}
                navigate={navigate}
                handleFinalSubmit={handleFinalSubmit}
            />
        </HighlightProvider>
    );
};

export default TestPage;
