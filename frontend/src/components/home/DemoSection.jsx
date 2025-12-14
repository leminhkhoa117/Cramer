import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence, useInView } from 'framer-motion';
import {
    FaPlay,
    FaClock,
    FaCheckCircle,
    FaArrowRight,
    FaArrowLeft,
    FaBookOpen,
    FaHeadphones,
    FaPen,
    FaMicrophone,
    FaVolumeUp,
    FaPause,
    FaHighlighter,
    FaStickyNote,
    FaList,
    FaExpand,
    FaTimes
} from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

const DemoSection = () => {
    const navigate = useNavigate();
    const sectionRef = useRef(null);
    const isInView = useInView(sectionRef, { once: true, margin: "-100px" });
    const [activeTab, setActiveTab] = useState('reading');
    const [isTyping, setIsTyping] = useState(false);
    const [typedText, setTypedText] = useState('');
    const [audioPlaying, setAudioPlaying] = useState(true);
    const [highlightMode, setHighlightMode] = useState(false);
    const [wordCount, setWordCount] = useState(247);

    // Auto-cycle through tabs
    useEffect(() => {
        if (!isInView) return;

        const tabs = ['reading', 'listening', 'writing', 'speaking'];
        let currentIndex = 0;

        const interval = setInterval(() => {
            currentIndex = (currentIndex + 1) % tabs.length;
            setActiveTab(tabs[currentIndex]);
        }, 3000);

        return () => clearInterval(interval);
    }, [isInView]);

    // Typing animation for writing demo
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
                // Simulate word count increasing
                if (index % 6 === 0 && wordCount < 252) {
                    setWordCount(prev => prev + 1);
                }
            } else {
                setIsTyping(false);
                clearInterval(typeInterval);
            }
        }, 25);

        return () => clearInterval(typeInterval);
    }, [activeTab, isInView]);

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

    // Question navigator data
    const questionNumbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14];
    const answeredQuestions = [1, 2, 3, 4, 5];

    return (
        <section ref={sectionRef} className="demo-section">
            <div className="demo-bg-gradient" />

            <div className="demo-container">
                {/* Header */}
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

                {/* Demo Window - Realistic Test Interface */}
                <motion.div
                    className="demo-window demo-window--enhanced"
                    initial={{ opacity: 0, y: 50, scale: 0.95 }}
                    animate={isInView ? { opacity: 1, y: 0, scale: 1 } : {}}
                    transition={{ duration: 0.8, delay: 0.2 }}
                >
                    {/* Top Header Bar - Like real test */}
                    <div className="demo-test-header">
                        <div className="demo-test-header-left">
                            <div className="demo-logo-mini">C</div>
                            <span className="demo-test-title">IELTS Practice Test</span>
                            <span className="demo-test-badge">{activeTab.charAt(0).toUpperCase() + activeTab.slice(1)}</span>
                        </div>
                        <div className="demo-test-header-center">
                            <div className="demo-timer-box">
                                <FaClock className="demo-timer-icon" />
                                <span className="demo-timer-value">45:32</span>
                            </div>
                        </div>
                        <div className="demo-test-header-right">
                            <button className="demo-header-btn">
                                <FaHighlighter />
                            </button>
                            <button className="demo-header-btn">
                                <FaStickyNote />
                            </button>
                            <button className="demo-header-btn demo-header-btn--primary">
                                Nộp bài
                            </button>
                        </div>
                    </div>

                    {/* Tab navigation with icons */}
                    <div className="demo-tabs demo-tabs--enhanced">
                        {tabs.map((tab) => (
                            <button
                                key={tab.id}
                                className={`demo-tab demo-tab--enhanced ${activeTab === tab.id ? 'active' : ''}`}
                                onClick={() => setActiveTab(tab.id)}
                                style={{ '--tab-color': tab.color }}
                            >
                                <tab.icon className="demo-tab-icon" />
                                <span>{tab.label}</span>
                                {activeTab === tab.id && (
                                    <motion.div
                                        className="demo-tab-indicator"
                                        layoutId="tabIndicator"
                                        style={{ background: tab.color }}
                                    />
                                )}
                            </button>
                        ))}
                    </div>

                    {/* Content area - Split panel layout like real test */}
                    <div className="demo-content demo-content--split">
                        <AnimatePresence mode="wait">
                            {/* ===== READING DEMO ===== */}
                            {activeTab === 'reading' && (
                                <motion.div
                                    key="reading"
                                    className="demo-split-layout"
                                    initial={{ opacity: 0 }}
                                    animate={{ opacity: 1 }}
                                    exit={{ opacity: 0 }}
                                    transition={{ duration: 0.3 }}
                                >
                                    {/* Left Panel - Passage */}
                                    <div className="demo-panel demo-panel--left">
                                        <div className="demo-panel-header">
                                            <h4>READING PASSAGE 1</h4>
                                            <div className="demo-panel-tools">
                                                <button className={`demo-tool-btn ${highlightMode ? 'active' : ''}`}>
                                                    <FaHighlighter />
                                                </button>
                                                <button className="demo-tool-btn">
                                                    <FaExpand />
                                                </button>
                                            </div>
                                        </div>
                                        <div className="demo-passage-content">
                                            <h3 className="demo-passage-title">The History of Writing</h3>
                                            <p className="demo-passage-text">
                                                <span className="demo-highlight">The development of writing</span> was one of the most significant advances in human history. It allowed for the recording of laws, the documentation of commerce, and the preservation of cultural knowledge across generations.
                                            </p>
                                            <p className="demo-passage-text">
                                                Early writing systems emerged independently in several regions of the world. In Mesopotamia, cuneiform script developed around 3400 BCE, while in Egypt, hieroglyphics appeared shortly thereafter...
                                            </p>
                                            <p className="demo-passage-more">
                                                <span>Scroll to read more</span>
                                            </p>
                                        </div>
                                    </div>

                                    {/* Resize Handle */}
                                    <div className="demo-resize-handle">
                                        <div className="demo-resize-dots">
                                            <span></span><span></span><span></span>
                                        </div>
                                    </div>

                                    {/* Right Panel - Questions */}
                                    <div className="demo-panel demo-panel--right">
                                        <div className="demo-questions-header">
                                            <span>Questions 1-14</span>
                                            <div className="demo-progress-indicator">
                                                <span className="demo-progress-text">5 / 14 answered</span>
                                                <div className="demo-mini-progress">
                                                    <div className="demo-mini-progress-fill" style={{ width: '35%' }}></div>
                                                </div>
                                            </div>
                                        </div>

                                        <div className="demo-question-card">
                                            <div className="demo-question-number">1</div>
                                            <div className="demo-question-content">
                                                <p className="demo-question-instruction">Choose the correct letter, A, B, C or D.</p>
                                                <p className="demo-question-text-enhanced">What does the author suggest about early writing systems?</p>

                                                <div className="demo-options-enhanced">
                                                    {[
                                                        'They were primarily used for religious purposes',
                                                        'They emerged independently in different regions',
                                                        'They were developed by merchants for trade',
                                                        'They replaced spoken language entirely'
                                                    ].map((option, index) => (
                                                        <motion.label
                                                            key={index}
                                                            className={`demo-option-enhanced ${index === 1 ? 'selected' : ''}`}
                                                            whileHover={{ scale: 1.01 }}
                                                            whileTap={{ scale: 0.99 }}
                                                        >
                                                            <div className="demo-option-radio">
                                                                {index === 1 && <div className="demo-option-radio-fill"></div>}
                                                            </div>
                                                            <span className="demo-option-letter-enhanced">{String.fromCharCode(65 + index)}</span>
                                                            <span className="demo-option-text">{option}</span>
                                                        </motion.label>
                                                    ))}
                                                </div>
                                            </div>
                                        </div>

                                        {/* Question Navigator */}
                                        <div className="demo-question-nav">
                                            <div className="demo-nav-label">Question Navigator</div>
                                            <div className="demo-nav-grid">
                                                {questionNumbers.map((num) => (
                                                    <div
                                                        key={num}
                                                        className={`demo-nav-item ${answeredQuestions.includes(num) ? 'answered' : ''} ${num === 1 ? 'current' : ''}`}
                                                    >
                                                        {num}
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            )}

                            {/* ===== LISTENING DEMO ===== */}
                            {activeTab === 'listening' && (
                                <motion.div
                                    key="listening"
                                    className="demo-listening-layout"
                                    initial={{ opacity: 0 }}
                                    animate={{ opacity: 1 }}
                                    exit={{ opacity: 0 }}
                                    transition={{ duration: 0.3 }}
                                >
                                    {/* Audio Controls Bar */}
                                    <div className="demo-audio-bar">
                                        <div className="demo-audio-section">
                                            <span className="demo-audio-label">Section 1</span>
                                            <div className="demo-audio-controls">
                                                <button
                                                    className="demo-play-btn"
                                                    onClick={() => setAudioPlaying(!audioPlaying)}
                                                >
                                                    {audioPlaying ? <FaPause /> : <FaPlay />}
                                                </button>
                                                <div className="demo-audio-waveform">
                                                    {[...Array(30)].map((_, i) => (
                                                        <motion.div
                                                            key={i}
                                                            className="demo-wave-bar"
                                                            animate={audioPlaying ? {
                                                                height: [12, 20 + Math.random() * 20, 12],
                                                            } : { height: 12 }}
                                                            transition={{
                                                                duration: 0.4 + Math.random() * 0.3,
                                                                repeat: audioPlaying ? Infinity : 0,
                                                                delay: i * 0.02
                                                            }}
                                                        />
                                                    ))}
                                                </div>
                                                <span className="demo-audio-time">02:34 / 05:00</span>
                                            </div>
                                        </div>
                                        <div className="demo-volume-control">
                                            <FaVolumeUp />
                                            <div className="demo-volume-slider">
                                                <div className="demo-volume-fill" style={{ width: '70%' }}></div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Listening Content */}
                                    <div className="demo-listening-content">
                                        <div className="demo-listening-instruction">
                                            <h4>Questions 1-5</h4>
                                            <p>Complete the notes below. Write <strong>NO MORE THAN TWO WORDS</strong> for each answer.</p>
                                        </div>

                                        <div className="demo-notes-card">
                                            <h5>University Enrollment Notes</h5>
                                            <div className="demo-note-line">
                                                <span>• Student's main interest is in </span>
                                                <span className="demo-blank-filled">marine biology</span>
                                            </div>
                                            <div className="demo-note-line">
                                                <span>• The course starts on </span>
                                                <span className="demo-blank-empty">
                                                    <input type="text" placeholder="..." disabled />
                                                </span>
                                            </div>
                                            <div className="demo-note-line">
                                                <span>• Students need to bring their own </span>
                                                <span className="demo-blank-empty">
                                                    <input type="text" placeholder="..." disabled />
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            )}

                            {/* ===== WRITING DEMO ===== */}
                            {activeTab === 'writing' && (
                                <motion.div
                                    key="writing"
                                    className="demo-split-layout demo-writing-layout"
                                    initial={{ opacity: 0 }}
                                    animate={{ opacity: 1 }}
                                    exit={{ opacity: 0 }}
                                    transition={{ duration: 0.3 }}
                                >
                                    {/* Left Panel - Task */}
                                    <div className="demo-panel demo-panel--left demo-writing-prompt">
                                        <div className="demo-task-header">
                                            <span className="demo-task-type">WRITING TASK 2</span>
                                            <span className="demo-task-time">You should spend about 40 minutes on this task.</span>
                                        </div>
                                        <div className="demo-task-content">
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
                                        </div>
                                    </div>

                                    {/* Resize Handle */}
                                    <div className="demo-resize-handle">
                                        <div className="demo-resize-dots">
                                            <span></span><span></span><span></span>
                                        </div>
                                    </div>

                                    {/* Right Panel - Editor */}
                                    <div className="demo-panel demo-panel--right demo-writing-editor">
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
                                            <div className="demo-ai-grading">
                                                <span className="demo-ai-icon">🤖</span>
                                                <span>AI sẽ chấm điểm và phân tích bài viết của bạn</span>
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            )}

                            {/* ===== SPEAKING DEMO ===== */}
                            {activeTab === 'speaking' && (
                                <motion.div
                                    key="speaking"
                                    className="demo-speaking-layout"
                                    initial={{ opacity: 0 }}
                                    animate={{ opacity: 1 }}
                                    exit={{ opacity: 0 }}
                                    transition={{ duration: 0.3 }}
                                >
                                    <div className="demo-speaking-container">
                                        <div className="demo-speaking-badge">PART 2 - Long Turn</div>
                                        <div className="demo-speaking-timer-box">
                                            <span className="demo-speaking-timer-label">Preparation Time</span>
                                            <span className="demo-speaking-timer-value">0:45</span>
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

                                        <div className="demo-speaking-controls">
                                            <motion.div
                                                className="demo-mic-button"
                                                animate={{
                                                    boxShadow: [
                                                        '0 0 0 0 rgba(239, 68, 68, 0.4)',
                                                        '0 0 0 20px rgba(239, 68, 68, 0)',
                                                        '0 0 0 0 rgba(239, 68, 68, 0)'
                                                    ]
                                                }}
                                                transition={{ duration: 1.5, repeat: Infinity }}
                                            >
                                                <FaMicrophone />
                                            </motion.div>
                                            <div className="demo-recording-status">
                                                <span className="demo-rec-dot"></span>
                                                <span>Recording...</span>
                                            </div>
                                            <div className="demo-recording-time">01:23</div>
                                        </div>

                                        <div className="demo-speaking-footer">
                                            <span className="demo-ai-note">🤖 AI Examiner sẽ đánh giá phát âm, ngữ pháp và từ vựng</span>
                                        </div>
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>

                    {/* Footer with navigation */}
                    <div className="demo-test-footer">
                        <button className="demo-footer-btn">
                            <FaArrowLeft />
                            <span>Previous</span>
                        </button>
                        <div className="demo-footer-info">
                            <span>Question 1 of 14</span>
                        </div>
                        <button className="demo-footer-btn demo-footer-btn--primary">
                            <span>Next</span>
                            <FaArrowRight />
                        </button>
                    </div>
                </motion.div>

                {/* CTA */}
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
