import React, { useState, useEffect } from 'react';
import { FiMaximize, FiMinimize } from 'react-icons/fi';
import './../css/TestHeader.css';

const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
};

export default function TestHeader({ testName, timeLeft, onSubmit, onExit }) {
    const [isFullscreen, setIsFullscreen] = useState(false);

    const toggleFullscreen = () => {
        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
            setIsFullscreen(true);
        } else if (document.exitFullscreen) {
            document.exitFullscreen();
            setIsFullscreen(false);
        }
    };

    useEffect(() => {
        const handleFullscreenChange = () => {
            setIsFullscreen(!!document.fullscreenElement);
        };
        document.addEventListener('fullscreenchange', handleFullscreenChange);
        return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
    }, []);

    return (
        <header className="test-page-header">
            <div className="test-header-left">
                <h1 className="test-header-title">{testName}</h1>
            </div>
            <div className="test-header-center">
                {timeLeft != null && (
                    <span className="test-timer">{formatTime(timeLeft)}</span>
                )}
            </div>
            <div className="test-header-right">
                {onExit && (
                    <button onClick={onExit} className="test-header-btn exit-btn">
                        Thoát
                    </button>
                )}
                <button onClick={toggleFullscreen} className="test-header-btn fullscreen-btn">
                    {isFullscreen ? <FiMinimize /> : <FiMaximize />}
                </button>
                <button onClick={onSubmit} className="test-header-btn submit-btn">
                    Nộp bài
                </button>
            </div>
        </header>
    );
}
