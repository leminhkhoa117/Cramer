import React, { useRef, useEffect, useImperativeHandle, forwardRef, useState } from 'react';
import '../css/AudioPlayer.css';

const formatTime = (timeInSeconds) => {
    const seconds = Math.floor(timeInSeconds);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
};

const AudioPlayer = forwardRef(({ audioUrl, onPlay, onPause, onEnded, index }, ref) => {
    const audioRef = useRef(null);
    const timelineRef = useRef(null);
    
    const [isPlaying, setIsPlaying] = useState(false);
    const [duration, setDuration] = useState(0);
    const [currentTime, setCurrentTime] = useState(0);
    const [isEnded, setIsEnded] = useState(false);

    useImperativeHandle(ref, () => ({
        play: () => {
            if (audioRef.current) {
                audioRef.current.play().catch(e => console.error("Audio play failed:", e));
            }
        },
        pause: () => {
            if (audioRef.current) {
                audioRef.current.pause();
            }
        },
    }));

    useEffect(() => {
        const audioElement = audioRef.current;
        if (audioElement && audioElement.src !== audioUrl) {
            audioElement.src = audioUrl;
            audioElement.load();
            setIsEnded(false);
            setCurrentTime(0);
            setDuration(0);
        }
    }, [audioUrl]);

    useEffect(() => {
        const audioElement = audioRef.current;
        if (!audioElement) return;

        const handlePlay = () => {
            setIsPlaying(true);
            onPlay && onPlay();
        };
        const handlePause = () => {
            setIsPlaying(false);
            onPause && onPause();
        };
        const handleEnded = () => {
            setIsEnded(true);
            setIsPlaying(false);
            onEnded && onEnded(index); // Pass index back onEnded
        };
        const handleTimeUpdate = () => setCurrentTime(audioElement.currentTime);
        const handleLoadedMetadata = () => setDuration(audioElement.duration);

        audioElement.addEventListener('play', handlePlay);
        audioElement.addEventListener('pause', handlePause);
        audioElement.addEventListener('ended', handleEnded);
        audioElement.addEventListener('timeupdate', handleTimeUpdate);
        audioElement.addEventListener('loadedmetadata', handleLoadedMetadata);

        return () => {
            audioElement.removeEventListener('play', handlePlay);
            audioElement.removeEventListener('pause', handlePause);
            audioElement.removeEventListener('ended', handleEnded);
            audioElement.removeEventListener('timeupdate', handleTimeUpdate);
            audioElement.removeEventListener('loadedmetadata', handleLoadedMetadata);
        };
    }, [onPlay, onPause, onEnded, index]);

    const handlePlayPauseClick = () => {
        if (isPlaying) {
            audioRef.current.pause();
        } else {
            audioRef.current.play();
        }
    };

    const handleTimelineClick = (e) => {
        if (!timelineRef.current || !duration) return;
        const timelineWidth = timelineRef.current.clientWidth;
        const clickPositionX = e.nativeEvent.offsetX;
        const newTime = (clickPositionX / timelineWidth) * duration;
        audioRef.current.currentTime = newTime;
        setCurrentTime(newTime);
    };

    const progressPercentage = duration > 0 ? (currentTime / duration) * 100 : 0;

    return (
        <div className="audio-player-container">
            <audio ref={audioRef} src={audioUrl}>
                Your browser does not support the audio element.
            </audio>
            <div className="audio-controls">
                <button onClick={handlePlayPauseClick} className="play-pause-btn">
                    {isPlaying ? (
                        <svg width="12" height="16" viewBox="0 0 12 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <rect width="4" height="16" rx="1" fill="white"/>
                            <rect x="8" width="4" height="16" rx="1" fill="white"/>
                        </svg>
                    ) : (
                        <svg width="14" height="16" viewBox="0 0 14 16" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ transform: 'translateX(1px)' }}>
                            <path d="M1.5 0.866025C0.833333 0.481125 0 0.943151 0 1.73205V14.2679C0 15.0569 0.833334 15.5189 1.5 15.134L12.75 8.86603C13.4167 8.48113 13.4167 7.51888 12.75 7.13397L1.5 0.866025Z" fill="white"/>
                        </svg>
                    )}
                </button>
                <span className="time-display current-time">{formatTime(currentTime)}</span>
                <div className="timeline-wrapper" ref={timelineRef} onClick={handleTimelineClick}>
                    <div className="timeline-background"></div>
                    <div className="timeline-progress" style={{ width: `${progressPercentage}%` }}></div>
                </div>
                <span className="time-display duration">{formatTime(duration)}</span>
            </div>
        </div>
    );
});

export default AudioPlayer;
