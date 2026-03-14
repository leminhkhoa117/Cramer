import React, { useState, useRef, useEffect, useCallback } from 'react';
import { motion, AnimatePresence, useInView } from 'framer-motion';
import {
    FaPlay,
    FaClock,
    FaArrowRight,
    FaBookOpen,
    FaHeadphones,
    FaPen,
    FaMicrophone,
    FaPause,
    FaList,
    FaSignOutAlt,
    FaCheck,
} from 'react-icons/fa';
import { FiMaximize } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';

const WaveformBars = ({ count = 24, active = false, color = 'var(--color-primary)' }) => (
    <div className="demo-waveform">
        {Array.from({ length: count }).map((_, i) => (
            <motion.span
                key={i}
                className="demo-waveform-bar"
                style={{ background: color }}
                animate={active ? {
                    scaleY: [0.3, 0.6 + Math.random() * 0.4, 0.3],
                    opacity: [0.5, 1, 0.5],
                } : { scaleY: 0.15, opacity: 0.3 }}
                transition={active ? {
                    duration: 0.4 + Math.random() * 0.3,
                    repeat: Infinity,
                    repeatType: 'reverse',
                    delay: i * 0.02,
                } : { duration: 0.4 }}
            />
        ))}
    </div>
);

const stagger = {
    hidden: { opacity: 0, y: 12 },
    show: (i) => ({
        opacity: 1, y: 0,
        transition: { delay: i * 0.06, duration: 0.35, ease: 'easeOut' },
    }),
};

const ResizeHandle = ({ onMouseDown, onTouchStart }) => (
    <div className="demo-resize-handle" onMouseDown={onMouseDown} onTouchStart={onTouchStart}>
        <div className="demo-resize-handle-grip">
            <span /><span /><span />
        </div>
    </div>
);

const DemoSection = () => {
    const navigate = useNavigate();
    const sectionRef = useRef(null);
    const windowRef = useRef(null);
    const isInView = useInView(sectionRef, { once: true, margin: "-100px" });
    const [activeTab, setActiveTab] = useState('reading');
    const [isTyping, setIsTyping] = useState(false);
    const [typedText, setTypedText] = useState('');
    const [audioPlaying, setAudioPlaying] = useState(true);
    const [wordCount, setWordCount] = useState(247);
    const [tabProgress, setTabProgress] = useState(0);
    const [tilt, setTilt] = useState({ x: 0, y: 0 });
    const [splitRatio, setSplitRatio] = useState(0.5);
    const isDragging = useRef(false);
    const contentRef = useRef(null);

    const CYCLE_MS = 5200;

    useEffect(() => {
        if (!isInView) return;

        const tabs = ['reading', 'listening', 'writing', 'speaking'];
        let currentIndex = 0;
        let start = Date.now();

        const frame = () => {
            const elapsed = Date.now() - start;
            const pct = Math.min(elapsed / CYCLE_MS, 1);
            setTabProgress(pct);
            if (pct >= 1) {
                currentIndex = (currentIndex + 1) % tabs.length;
                setActiveTab(tabs[currentIndex]);
                start = Date.now();
                setTabProgress(0);
            }
            rafId = requestAnimationFrame(frame);
        };
        let rafId = requestAnimationFrame(frame);

        return () => cancelAnimationFrame(rafId);
    }, [isInView]);

    useEffect(() => {
        if (activeTab !== 'writing' || !isInView) return;

        const fullText = "In my opinion, while technology has certainly added complexity to certain aspects of our daily lives, the overall benefits far outweigh the drawbacks. Modern innovations have revolutionized the way we communicate, work, and access information...";
        let index = 0;
        setTypedText('');
        setIsTyping(true);
        setWordCount(247);

        const typeInterval = setInterval(() => {
            if (index < fullText.length) {
                setTypedText(fullText.slice(0, index + 1));
                index++;
                if (index % 6 === 0) setWordCount(prev => Math.min(prev + 1, 252));
            } else {
                setIsTyping(false);
                clearInterval(typeInterval);
            }
        }, 20);

        return () => clearInterval(typeInterval);
    }, [activeTab, isInView]);

    const handleMouseMove = useCallback((e) => {
        if (!windowRef.current) return;
        const rect = windowRef.current.getBoundingClientRect();
        const x = (e.clientX - rect.left) / rect.width;
        const y = (e.clientY - rect.top) / rect.height;
        const tiltX = (y - 0.5) * -12;
        const tiltY = (x - 0.5) * 12;
        setTilt({ x: tiltX, y: tiltY });
    }, []);

    const handleMouseLeave = useCallback(() => {
        setTilt({ x: 0, y: 0 });
    }, []);

    const startResize = useCallback((e) => {
        e.preventDefault();
        isDragging.current = true;

        const onMove = (moveEvent) => {
            if (!isDragging.current || !contentRef.current) return;
            const rect = contentRef.current.getBoundingClientRect();
            const clientX = moveEvent.touches ? moveEvent.touches[0].clientX : moveEvent.clientX;
            const ratio = (clientX - rect.left) / rect.width;
            setSplitRatio(Math.max(0.25, Math.min(0.75, ratio)));
        };

        const onUp = () => {
            isDragging.current = false;
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
            document.removeEventListener('touchmove', onMove);
            document.removeEventListener('touchend', onUp);
        };

        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
        document.addEventListener('touchmove', onMove);
        document.addEventListener('touchend', onUp);
    }, []);

    const handleTryNow = () => {
        navigate('/courses');
        window.scrollTo(0, 0);
    };

    const tabs = [
        { id: 'reading', label: 'Reading', icon: FaBookOpen, color: '#7c3aed' },
        { id: 'listening', label: 'Listening', icon: FaHeadphones, color: '#6366f1' },
        { id: 'writing', label: 'Writing', icon: FaPen, color: '#8b5cf6' },
        { id: 'speaking', label: 'Speaking', icon: FaMicrophone, color: '#a855f7' },
    ];

    const readingParts = [
        { label: 'Part 1', range: '1–13', total: 13, answered: 5 },
        { label: 'Part 2', range: '14–26', total: 13, answered: 2 },
        { label: 'Part 3', range: '27–40', total: 14, answered: 0 },
    ];
    const listeningParts = [
        { label: 'Part 1', range: '1–10', total: 10, answered: 7 },
        { label: 'Part 2', range: '11–20', total: 10, answered: 3 },
        { label: 'Part 3', range: '21–30', total: 10, answered: 0 },
        { label: 'Part 4', range: '31–40', total: 10, answered: 0 },
    ];

    const timerValues = {
        reading: '45:32',
        listening: null,
        writing: '38:15',
        speaking: null,
    };

    const testNames = {
        reading: 'IELTS Academic Reading — Cambridge 17',
        listening: 'IELTS Listening — Cambridge 17',
        writing: 'IELTS Academic Writing — Cambridge 17',
        speaking: 'IELTS Speaking Practice',
    };

    const readingFocusTags = ['Keyword match 82%', 'Time pressure medium', 'Accuracy +6%'];
    const readingNavigator = Array.from({ length: 13 }, (_, index) => index + 1);
    const listeningCues = [
        { label: 'Signal phrase', value: 'however' },
        { label: 'Number trap', value: '13 / 30' },
        { label: 'Accent', value: 'Australian' },
    ];
    const writingSignals = [
        { label: 'Lexical range', value: 'B2+' },
        { label: 'Coherence', value: '7.0' },
        { label: 'Grammar alerts', value: '4' },
    ];
    const speakingCriteria = [
        { label: 'Pronunciation', score: 0.76 },
        { label: 'Fluency', score: 0.71 },
        { label: 'Vocabulary', score: 0.78 },
    ];

    return (
        <section ref={sectionRef} className="demo-section">
            <div className="demo-container">
                <motion.div
                    className="demo-header"
                    initial={{ opacity: 0, y: 30 }}
                    animate={isInView ? { opacity: 1, y: 0 } : {}}
                    transition={{ duration: 0.6 }}
                >
                    <span className="demo-label">Trải nghiệm ngay</span>
                    <h2 className="demo-title">
                        Giao diện luyện thi
                        <br />
                        <span className="text-gradient">giống phòng thi thật</span>
                    </h2>
                    <p className="demo-subtitle">
                        Môi trường thi thử chuyên nghiệp, giúp bạn tự tin bước vào kỳ thi IELTS
                    </p>
                </motion.div>

                <div
                    ref={windowRef}
                    className="demo-window--3d"
                    onMouseMove={handleMouseMove}
                    onMouseLeave={handleMouseLeave}
                    style={{
                        transform: `perspective(1200px) rotateX(${tilt.x}deg) rotateY(${tilt.y}deg)`,
                    }}
                >
                    <motion.div
                        className="demo-window demo-window--enhanced"
                        initial={{ opacity: 0, y: 50, scale: 0.96 }}
                        animate={isInView ? { opacity: 1, y: 0, scale: 1 } : {}}
                        transition={{ duration: 0.8, delay: 0.2 }}
                    >
                        <div className="demo-test-header demo-test-header--real">
                            <div className="demo-test-header-left">
                                <span className="demo-header-title">{testNames[activeTab]}</span>
                            </div>
                            <div className="demo-test-header-center">
                                {timerValues[activeTab] && (
                                    <motion.span
                                        className="demo-timer-real"
                                        key={timerValues[activeTab]}
                                        initial={{ scale: 0.9, opacity: 0 }}
                                        animate={{ scale: 1, opacity: 1 }}
                                        transition={{ type: 'spring', stiffness: 300, damping: 20 }}
                                    >
                                        <FaClock className="demo-timer-icon" />
                                        {timerValues[activeTab]}
                                    </motion.span>
                                )}
                            </div>
                            <div className="demo-test-header-right">
                                <button className="demo-hdr-btn demo-hdr-btn--exit"><FaSignOutAlt /> Thoát</button>
                                <button className="demo-hdr-btn demo-hdr-btn--fs"><FiMaximize /></button>
                                <button className="demo-hdr-btn demo-hdr-btn--submit">Nộp bài</button>
                            </div>
                        </div>

                        <div className="demo-tabs demo-tabs--enhanced">
                            {tabs.map((tab) => {
                                const isActive = activeTab === tab.id;
                                return (
                                    <button
                                        key={tab.id}
                                        className={`demo-tab demo-tab--enhanced ${isActive ? 'active' : ''}`}
                                        onClick={() => setActiveTab(tab.id)}
                                        style={{ '--tab-color': tab.color }}
                                    >
                                        <tab.icon className="demo-tab-icon" />
                                        <span>{tab.label}</span>
                                        {isActive && (
                                            <motion.div
                                                className="demo-tab-indicator"
                                                layoutId="tabIndicator"
                                                style={{ background: tab.color }}
                                            />
                                        )}
                                        {isActive && (
                                            <div
                                                className="demo-tab-progress"
                                                style={{
                                                    '--progress': `${tabProgress * 100}%`,
                                                    background: `linear-gradient(90deg, ${tab.color}22 0%, ${tab.color}22 var(--progress), transparent var(--progress))`,
                                                }}
                                            />
                                        )}
                                    </button>
                                );
                            })}
                        </div>

                        <div className="demo-content demo-content--split" ref={contentRef}>
                            <AnimatePresence mode="wait">
                                {activeTab === 'reading' && (
                                    <motion.div
                                        key="reading"
                                        className="demo-split-layout"
                                        initial={{ opacity: 0, x: -18 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: 18 }}
                                        transition={{ duration: 0.4, ease: 'easeOut' }}
                                    >
                                        <div className="demo-panel demo-panel--passage" style={{ flex: `0 0 ${splitRatio * 100}%` }}>
                                            <motion.h2 className="demo-passage-title-real" variants={stagger} custom={0} initial="hidden" animate="show">
                                                Reading Passage 1
                                            </motion.h2>
                                            <motion.p className="demo-passage-instructions" variants={stagger} custom={1} initial="hidden" animate="show">
                                                You should spend about 20 minutes on Questions 1–13, which are based on Reading Passage 1 below.
                                            </motion.p>
                                            <motion.div className="demo-passage-content" variants={stagger} custom={2} initial="hidden" animate="show">
                                                <p className="demo-passage-text">
                                                    <span className="demo-highlight">The development of writing</span> was one of the most significant advances in human history. It allowed for the recording of laws, the documentation of commerce, and the preservation of cultural knowledge across generations.
                                                </p>
                                                <p className="demo-passage-text">
                                                    Early writing systems emerged independently in several regions of the world. In Mesopotamia, cuneiform script developed around 3400 BCE, while in Egypt, hieroglyphics appeared shortly thereafter...
                                                </p>
                                            </motion.div>

                                            <motion.div className="demo-reading-insights" variants={stagger} custom={3} initial="hidden" animate="show">
                                                {readingFocusTags.map((tag) => (
                                                    <span key={tag}>{tag}</span>
                                                ))}
                                            </motion.div>
                                        </div>

                                        <ResizeHandle onMouseDown={startResize} onTouchStart={startResize} />

                                        <div className="demo-panel demo-panel--questions" style={{ flex: `0 0 ${(1 - splitRatio) * 100}%` }}>
                                            <motion.div className="demo-question-card" variants={stagger} custom={1} initial="hidden" animate="show">
                                                <div className="demo-q-group-header">
                                                    <span className="demo-q-group-type">Choose the correct letter, A, B, C or D.</span>
                                                </div>
                                                <div className="demo-q-block">
                                                    <span className="demo-q-num">1</span>
                                                    <div className="demo-q-body">
                                                        <p className="demo-question-text-real">What does the author suggest about early writing systems?</p>
                                                        <div className="demo-options-real">
                                                            {[
                                                                'They were primarily used for religious purposes',
                                                                'They emerged independently in different regions',
                                                                'They were developed by merchants for trade',
                                                                'They replaced spoken language entirely'
                                                            ].map((option, index) => (
                                                                <motion.label
                                                                    key={index}
                                                                    className={`demo-option-real ${index === 1 ? 'selected' : ''}`}
                                                                    variants={stagger}
                                                                    custom={index + 2}
                                                                    initial="hidden"
                                                                    animate="show"
                                                                    whileHover={{ x: 2 }}
                                                                    whileTap={{ scale: 0.99 }}
                                                                >
                                                                    <div className="demo-radio-real">
                                                                        {index === 1 && <motion.div className="demo-radio-fill" layoutId="radioFill" />}
                                                                    </div>
                                                                    <span className="demo-option-letter-real">{String.fromCharCode(65 + index)}</span>
                                                                    <span className="demo-option-text">{option}</span>
                                                                </motion.label>
                                                            ))}
                                                        </div>

                                                        <div className="demo-question-footnote">
                                                            Evidence line hint: paragraph 2, sentence 1
                                                        </div>
                                                    </div>
                                                </div>
                                            </motion.div>

                                            <motion.div className="demo-question-card demo-question-card--compact" variants={stagger} custom={2} initial="hidden" animate="show">
                                                <div className="demo-q-group-header">
                                                    <span className="demo-q-group-type">Question navigator</span>
                                                </div>
                                                <div className="demo-question-nav-grid">
                                                    {readingNavigator.map((question) => (
                                                        <span key={question} className={question < 6 ? 'answered' : ''}>
                                                            {question}
                                                        </span>
                                                    ))}
                                                </div>
                                            </motion.div>
                                        </div>
                                    </motion.div>
                                )}

                                {activeTab === 'listening' && (
                                    <motion.div
                                        key="listening"
                                        className="demo-listening-layout-real"
                                        initial={{ opacity: 0, x: -18 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: 18 }}
                                        transition={{ duration: 0.4, ease: 'easeOut' }}
                                    >
                                        <motion.div className="demo-audio-hero" variants={stagger} custom={0} initial="hidden" animate="show">
                                            <div className="demo-audio-hero-left">
                                                <button
                                                    className="demo-audio-hero-play"
                                                    onClick={() => setAudioPlaying(!audioPlaying)}
                                                >
                                                    {audioPlaying ? <FaPause /> : <FaPlay />}
                                                </button>
                                                <div className="demo-audio-hero-meta">
                                                    <span className="demo-audio-hero-title">Part 1 — Section 1</span>
                                                    <span className="demo-audio-hero-time">02:34 / 05:47</span>
                                                </div>
                                            </div>
                                            <div className="demo-audio-waveform-container">
                                                <div className="demo-audio-progress-track">
                                                    <div className="demo-audio-progress-fill" style={{ width: '43%' }} />
                                                </div>
                                                <WaveformBars count={40} active={audioPlaying} color="var(--color-primary)" />
                                            </div>
                                            <div className="demo-audio-parts-pills">
                                                {['1', '2', '3', '4'].map((p, idx) => (
                                                    <span key={idx} className={`demo-audio-part-pill ${idx === 0 ? 'active' : ''}`}>
                                                        Part {p}
                                                    </span>
                                                ))}
                                            </div>
                                        </motion.div>

                                        <div className="demo-listening-questions">
                                            <motion.div className="demo-question-card" variants={stagger} custom={1} initial="hidden" animate="show">
                                                <div className="demo-q-group-header">
                                                    <span className="demo-q-group-type">Complete the notes below. Write <strong>NO MORE THAN TWO WORDS</strong> for each answer.</span>
                                                </div>
                                                <div className="demo-notes-card-real">
                                                    <h5>University Enrollment Notes</h5>
                                                    {[
                                                        { text: "Student's main interest is in", answer: 'marine biology', filled: true },
                                                        { text: 'The course starts on', answer: null, filled: false },
                                                        { text: 'Students need to bring their own', answer: null, filled: false },
                                                    ].map((line, idx) => (
                                                        <motion.div key={idx} className="demo-note-line" variants={stagger} custom={idx + 2} initial="hidden" animate="show">
                                                            <span>{'\u2022'} {line.text} </span>
                                                            {line.filled ? (
                                                                <span className="demo-blank-filled">{line.answer}</span>
                                                            ) : (
                                                                <span className="demo-blank-input"><input type="text" placeholder="..." disabled /></span>
                                                            )}
                                                        </motion.div>
                                                    ))}
                                                </div>

                                                <div className="demo-listening-cues">
                                                    {listeningCues.map((cue) => (
                                                        <span key={cue.label}>
                                                            <strong>{cue.value}</strong>
                                                            <em>{cue.label}</em>
                                                        </span>
                                                    ))}
                                                </div>
                                            </motion.div>
                                        </div>
                                    </motion.div>
                                )}

                                {activeTab === 'writing' && (
                                    <motion.div
                                        key="writing"
                                        className="demo-split-layout demo-writing-layout"
                                        initial={{ opacity: 0, x: -18 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: 18 }}
                                        transition={{ duration: 0.4, ease: 'easeOut' }}
                                    >
                                        <div className="demo-panel demo-panel--passage demo-writing-prompt" style={{ flex: `0 0 ${splitRatio * 100}%` }}>
                                            <motion.div className="demo-task-header" variants={stagger} custom={0} initial="hidden" animate="show">
                                                <span className="demo-task-type">WRITING TASK 2</span>
                                                <span className="demo-task-time">~40 minutes</span>
                                            </motion.div>
                                            <motion.div className="demo-task-content" variants={stagger} custom={1} initial="hidden" animate="show">
                                                <p className="demo-task-text">
                                                    <em>Some people believe that technology has made our lives too complex. Others argue that it has made things simpler.</em>
                                                </p>
                                                <p className="demo-task-text">
                                                    <strong>Discuss both views and give your own opinion.</strong>
                                                </p>
                                                <div className="demo-task-requirement">
                                                    <FaList className="demo-req-icon" />
                                                    <span>Write at least <strong>250 words</strong></span>
                                                </div>
                                            </motion.div>
                                        </div>

                                        <ResizeHandle onMouseDown={startResize} onTouchStart={startResize} />

                                        <div className="demo-panel demo-panel--questions demo-writing-editor" style={{ flex: `0 0 ${(1 - splitRatio) * 100}%` }}>
                                            <div className="demo-editor-header">
                                                <h4>Your Essay</h4>
                                                <div className={`demo-word-counter ${wordCount >= 250 ? 'success' : 'warning'}`}>
                                                    <span className="demo-wc-count">{wordCount}</span>
                                                    <span className="demo-wc-sep">/</span>
                                                    <span className="demo-wc-min">250</span>
                                                    <span className="demo-wc-label">words</span>
                                                </div>
                                            </div>
                                            <div className="demo-editor-area">
                                                <p className="demo-editor-text">
                                                    {typedText}
                                                    {isTyping && <span className="demo-cursor-blink">|</span>}
                                                </p>
                                            </div>
                                            <div className="demo-editor-footer">
                                                <div className="demo-editor-insights">
                                                    <div className="demo-ai-grading">
                                                        <span className="demo-ai-icon">🤖</span>
                                                        <span>AI sẽ chấm điểm và phân tích bài viết của bạn</span>
                                                    </div>
                                                    <div className="demo-writing-signals">
                                                        {writingSignals.map((signal) => (
                                                            <span key={signal.label}>
                                                                <strong>{signal.value}</strong>
                                                                <em>{signal.label}</em>
                                                            </span>
                                                        ))}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </motion.div>
                                )}

                                {activeTab === 'speaking' && (
                                    <motion.div
                                        key="speaking"
                                        className="demo-speaking-layout"
                                        initial={{ opacity: 0, x: -18 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: 18 }}
                                        transition={{ duration: 0.4, ease: 'easeOut' }}
                                    >
                                        <div className="demo-speaking-container">
                                            <motion.div className="demo-speaking-left" variants={stagger} custom={0} initial="hidden" animate="show">
                                                <div className="demo-speaking-meta">
                                                    <span className="demo-speaking-badge">PART 2</span>
                                                    <span className="demo-speaking-subtitle">Long Turn</span>
                                                </div>
                                                <div className="demo-topic-card">
                                                    <div className="demo-topic-header">
                                                        <span>Describe a memorable journey you have taken</span>
                                                    </div>
                                                    <div className="demo-topic-points">
                                                        <p>You should say:</p>
                                                        <ul>
                                                            <li>where you went</li>
                                                            <li>who you went with</li>
                                                            <li>what you did there</li>
                                                        </ul>
                                                        <p className="demo-topic-explain">and explain why this journey was memorable.</p>
                                                    </div>
                                                </div>
                                            </motion.div>

                                            <motion.div className="demo-speaking-right" variants={stagger} custom={1} initial="hidden" animate="show">
                                                <div className="demo-speaking-timer-box">
                                                    <span className="demo-speaking-timer-label">Preparation Time</span>
                                                    <span className="demo-speaking-timer-value">0:45</span>
                                                </div>

                                                <motion.div
                                                    className="demo-mic-button"
                                                    animate={{
                                                        boxShadow: [
                                                            '0 0 0 0 rgba(239, 68, 68, 0.4)',
                                                            '0 0 0 24px rgba(239, 68, 68, 0)',
                                                            '0 0 0 0 rgba(239, 68, 68, 0)'
                                                        ]
                                                    }}
                                                    transition={{ duration: 1.5, repeat: Infinity }}
                                                >
                                                    <FaMicrophone />
                                                </motion.div>

                                                <WaveformBars count={20} active={true} color="#ef4444" />

                                                <div className="demo-recording-status">
                                                    <span className="demo-rec-dot" />
                                                    <span>Recording...</span>
                                                    <span className="demo-recording-time">01:23</span>
                                                </div>

                                                <div className="demo-speaking-ai-note">
                                                    <span>🤖</span>
                                                    <span>AI Examiner đánh giá phát âm, ngữ pháp và từ vựng</span>
                                                </div>

                                                <div className="demo-speaking-criteria">
                                                    {speakingCriteria.map((criterion) => (
                                                        <div key={criterion.label} className="demo-speaking-criterion">
                                                            <span>{criterion.label}</span>
                                                            <div className="demo-speaking-criterion-bar">
                                                                <span style={{ '--criterion-progress': criterion.score }} />
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            </motion.div>
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </div>

                        <AnimatePresence mode="wait">
                            <motion.div
                                key={activeTab}
                                className="demo-test-footer demo-test-footer--real"
                                initial={{ opacity: 0, y: 4 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0, y: -4 }}
                                transition={{ duration: 0.28 }}
                            >
                                {(activeTab === 'reading' || activeTab === 'listening') &&
                                    (activeTab === 'reading' ? readingParts : listeningParts).map((part, idx) => {
                                        const isActive = idx === 0;
                                        return (
                                            <div key={idx} className={`demo-footer-part ${isActive ? 'active' : ''}`}>
                                                <div className="demo-footer-part-head">
                                                    <h3 className="demo-footer-part-title">{part.label}</h3>
                                                    <span className="demo-footer-part-meta">
                                                        {part.answered}/{part.total}
                                                    </span>
                                                </div>
                                                <div className="demo-footer-progress-track">
                                                    <motion.div
                                                        className="demo-footer-progress-fill"
                                                        initial={{ width: 0 }}
                                                        animate={{ width: `${(part.answered / part.total) * 100}%` }}
                                                        transition={{ duration: 0.6, delay: idx * 0.1 }}
                                                    />
                                                </div>
                                                <span className="demo-footer-range">Q {part.range}</span>
                                            </div>
                                        );
                                    })
                                }
                                {activeTab === 'writing' && (
                                    <>
                                        <div className="demo-footer-part">
                                            <div className="demo-footer-part-head">
                                                <h3 className="demo-footer-part-title">Task 1</h3>
                                                <span className="demo-wc-badge demo-wc-badge--done"><FaCheck /> 187 / 150</span>
                                            </div>
                                        </div>
                                        <div className="demo-footer-part active">
                                            <div className="demo-footer-part-head">
                                                <h3 className="demo-footer-part-title">Task 2</h3>
                                                <span className="demo-wc-badge demo-wc-badge--warn">{wordCount} / 250</span>
                                            </div>
                                        </div>
                                    </>
                                )}
                                {activeTab === 'speaking' && (
                                    <>
                                        {['Part 1', 'Part 2', 'Part 3'].map((p, idx) => (
                                            <div key={idx} className={`demo-footer-part demo-footer-part--tab ${idx === 1 ? 'active' : ''}`}>
                                                <span className="demo-footer-tab-pill">{p}</span>
                                            </div>
                                        ))}
                                    </>
                                )}
                            </motion.div>
                        </AnimatePresence>
                    </motion.div>
                </div>

                <motion.div
                    className="demo-cta"
                    initial={{ opacity: 0, y: 20 }}
                    animate={isInView ? { opacity: 1, y: 0 } : {}}
                    transition={{ duration: 0.6, delay: 0.5 }}
                >
                    <button className="demo-cta-btn" onClick={handleTryNow}>
                        <span>Thử ngay miễn phí</span>
                        <FaArrowRight />
                    </button>
                </motion.div>
            </div>
        </section>
    );
};

export default DemoSection;
