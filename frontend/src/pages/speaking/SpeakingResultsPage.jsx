import React, { useEffect, useState, useRef, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { PanelGroup, Panel, PanelResizeHandle } from 'react-resizable-panels';
import {
  FiArrowLeft, FiDownload, FiCheckCircle, FiAlertCircle,
  FiChevronDown, FiChevronRight, FiMic, FiTarget, FiThumbsUp, FiAlertTriangle, FiZap, FiBookOpen, FiLoader
} from 'react-icons/fi';
import { useSpeakingStore } from '../../stores';
import { speakingApi } from '../../api/speakingApi';
import ConversationPlayer from '../../components/speaking/ConversationPlayer';
import '../../css/speaking/speaking-results.css';
import '../../css/speaking/conversation-player.css';
import '../../css/common/panel-resize-handle.css';

/**
 * SpeakingResultsPage - Display detailed evaluation results
 * Updated to match Writing Result Page UI pattern
 */
export default function SpeakingResultsPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  // Loading and error states
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [resultsData, setResultsData] = useState(null);
  const [evaluationPending, setEvaluationPending] = useState(false);

  // Get store data with safe defaults
  const storeEvaluation = useSpeakingStore(state => state.evaluation);
  const storeTranscripts = useSpeakingStore(state => state.transcripts) || [];
  const sourceContext = useSpeakingStore(state => state.sourceContext);
  const fetchResults = useSpeakingStore(state => state.fetchResults);

  // Fetch results from backend on mount
  useEffect(() => {
    const loadResults = async () => {
      if (!sessionId) {
        setIsLoading(false);
        return;
      }

      // If we already have evaluation data in store for this session, use it
      if (storeEvaluation && storeEvaluation.sessionId === parseInt(sessionId)) {
        setResultsData(storeEvaluation);
        setIsLoading(false);
        return;
      }

      try {
        setIsLoading(true);
        setFetchError(null);

        const response = await speakingApi.getResults(sessionId);
        const data = response.data.data;

        // Map backend response to frontend format
        const mappedData = {
          sessionId: data.sessionId,
          overallBand: data.overallBand,
          criteria: {
            fluency: data.fluency ? { band: parseFloat(data.fluency.band), label: data.fluency.label, notes: data.fluency.notes } : null,
            lexical: data.lexical ? { band: parseFloat(data.lexical.band), label: data.lexical.label, notes: data.lexical.notes } : null,
            grammar: data.grammar ? { band: parseFloat(data.grammar.band), label: data.grammar.label, notes: data.grammar.notes } : null,
            pronunciation: data.pronunciation ? { band: parseFloat(data.pronunciation.band), label: data.pronunciation.label, notes: data.pronunciation.notes } : null,
          },
          strengths: data.strengths || [],
          weaknesses: data.weaknesses || [],
          suggestions: data.suggestions || [],
          overallFeedback: data.overallFeedback,
          completedAt: data.completedAt,
          totalDurationSeconds: data.totalDurationSeconds,
          // Map transcripts with proper field names
          transcripts: (data.transcripts || []).map(t => ({
            questionId: t.questionId,
            part: t.part,
            question: t.questionText,
            text: t.transcriptText || '',
            audioUrl: t.audioUrl,
            duration: t.audioDurationSeconds,
            examinerAudioUrl: t.examinerAudioUrl,
            examinerAudioDurationMs: t.examinerAudioDurationMs,
          })),
        };

        setResultsData(mappedData);
      } catch (err) {
        console.error('Failed to fetch results:', err);
        // Don't show error if we have store data, fall back to mock
        if (!storeEvaluation) {
          setFetchError(err.response?.data?.message || 'Không thể tải kết quả. Sử dụng dữ liệu mẫu.');
        }
      } finally {
        setIsLoading(false);
      }
    };

    loadResults();
  }, [sessionId, storeEvaluation]);

  // State for grading failure
  const [gradingFailed, setGradingFailed] = useState(false);
  const [pollCount, setPollCount] = useState(0);
  const MAX_POLL_ATTEMPTS = 60; // 3 minutes max (60 * 3s)

  // Polling for evaluation results when session is completed but not graded yet
  useEffect(() => {
    if (!resultsData || !sessionId) return;

    // If session has results but no overallBand, evaluation is still in progress
    const hasTranscripts = resultsData.transcripts && resultsData.transcripts.length > 0;
    const needsPolling = hasTranscripts && resultsData.overallBand == null && !gradingFailed;

    if (!needsPolling) {
      setEvaluationPending(false);
      return;
    }

    setEvaluationPending(true);
    console.log('Starting evaluation polling for session:', sessionId);

    const pollInterval = setInterval(async () => {
      try {
        const response = await speakingApi.getResults(sessionId);
        const data = response.data.data;

        // Check if grading failed
        if (data.sessionStatus === 'failed') {
          console.log('Grading failed for session:', sessionId);
          setGradingFailed(true);
          setEvaluationPending(false);
          clearInterval(pollInterval);
          return;
        }

        // Check poll count timeout
        setPollCount(prev => {
          const newCount = prev + 1;
          if (newCount >= MAX_POLL_ATTEMPTS) {
            console.log('Polling timeout after', MAX_POLL_ATTEMPTS, 'attempts');
            setGradingFailed(true);
            setEvaluationPending(false);
            clearInterval(pollInterval);
          }
          return newCount;
        });

        if (data.overallBand != null) {
          console.log('Evaluation complete, overallBand:', data.overallBand);

          // Map and update results
          const mappedData = {
            sessionId: data.sessionId,
            overallBand: data.overallBand,
            criteria: {
              fluency: data.fluency ? { band: parseFloat(data.fluency.band), label: data.fluency.label, notes: data.fluency.notes } : null,
              lexical: data.lexical ? { band: parseFloat(data.lexical.band), label: data.lexical.label, notes: data.lexical.notes } : null,
              grammar: data.grammar ? { band: parseFloat(data.grammar.band), label: data.grammar.label, notes: data.grammar.notes } : null,
              pronunciation: data.pronunciation ? { band: parseFloat(data.pronunciation.band), label: data.pronunciation.label, notes: data.pronunciation.notes } : null,
            },
            strengths: data.strengths || [],
            weaknesses: data.weaknesses || [],
            suggestions: data.suggestions || [],
            overallFeedback: data.overallFeedback,
            completedAt: data.completedAt,
            totalDurationSeconds: data.totalDurationSeconds,
            transcripts: (data.transcripts || []).map(t => ({
              questionId: t.questionId,
              part: t.part,
              question: t.questionText,
              text: t.transcriptText || '',
              audioUrl: t.audioUrl,
              duration: t.audioDurationSeconds,
              examinerAudioUrl: t.examinerAudioUrl,
              examinerAudioDurationMs: t.examinerAudioDurationMs,
            })),
          };

          setResultsData(mappedData);
          setEvaluationPending(false);
          clearInterval(pollInterval);
        }
      } catch (err) {
        console.warn('Polling failed:', err.message);
      }
    }, 3000); // Poll every 3 seconds

    return () => {
      clearInterval(pollInterval);
    };
  }, [resultsData?.overallBand, sessionId]);

  // Use fetched data, store data, or fallback to evaluation
  const evaluation = resultsData || storeEvaluation;
  const transcripts = resultsData?.transcripts || storeTranscripts;

  const [selectedQuestionIndex, setSelectedQuestionIndex] = useState(0);
  const [scoresBarCollapsed, setScoresBarCollapsed] = useState(false);
  const [expandedScores, setExpandedScores] = useState({});
  const [activePart, setActivePart] = useState(1);

  // Refs for scrolling
  const responseColumnRef = useRef(null);

  // Default mock data for when no grading has been done yet
  const defaultMockEvaluation = {
    overallBand: 7.0,
    criteria: {
      fluency: { band: 7.0, label: 'Fluency & Coherence' },
      lexical: { band: 6.5, label: 'Lexical Resource' },
      grammar: { band: 6.5, label: 'Grammatical Range & Accuracy' },
      pronunciation: { band: 7.5, label: 'Pronunciation' },
    },
    strengths: [
      'Phát âm rõ ràng với ngữ điệu tự nhiên',
      'Phát triển câu trả lời tốt trong Part 3',
      'Sử dụng liên từ mạch lạc',
    ],
    weaknesses: [
      'Vốn từ vựng còn hạn chế, lặp lại một số từ',
      'Đôi khi mắc lỗi ngữ pháp với cấu trúc phức tạp',
      'Cần mở rộng ý tưởng chi tiết hơn',
    ],
    suggestions: [
      'Học thêm từ đồng nghĩa và cụm từ cố định (collocations)',
      'Luyện tập sử dụng nhiều cấu trúc câu phức tạp hơn',
      'Đọc nhiều hơn để mở rộng vốn từ và cách diễn đạt',
    ],
    improvements: [
      {
        original: 'I really like pop music',
        improved: "I'm quite passionate about contemporary pop music",
        explanation: 'Sử dụng cấu trúc nâng cao hơn và từ vựng học thuật'
      },
      {
        original: 'It was absolutely amazing',
        improved: 'It was an utterly unforgettable experience',
        explanation: 'Tránh từ quá phổ biến, dùng collocation tự nhiên hơn'
      },
    ],
  };

  // Use evaluation data if it has actual grading results, otherwise use mock
  // Check for overallBand to determine if session has been graded
  const hasGradingResults = evaluation && evaluation.overallBand != null;
  const mockEvaluation = hasGradingResults ? evaluation : defaultMockEvaluation;

  // Mock transcripts if not available
  const mockTranscripts = (Array.isArray(transcripts) && transcripts.length > 0) ? transcripts : [
    {
      questionId: 1,
      part: 1,
      question: "What's your full name?",
      text: "My name is Minh Khoa and I'm from Hanoi, Vietnam.",
      feedback: {
        good: ['Clear introduction', 'Natural delivery'],
        improve: [],
      },
      highlights: [
        { text: 'Hanoi', type: 'good', note: 'Good pronunciation' },
      ],
    },
    {
      questionId: 4,
      part: 1,
      question: "Do you like music?",
      text: "I really like pop music, especially K-pop. I listen to it almost every day when I'm studying or relaxing. My favorite artists are BTS and Blackpink.",
      feedback: {
        good: ['Good topic development', 'Personal examples'],
        improve: ['Could use more varied vocabulary'],
      },
      sampleAnswer: "I'm quite passionate about contemporary pop music, particularly K-pop. I find myself listening to it on a daily basis, whether I'm studying or simply unwinding. Among my preferred artists are BTS and Blackpink, whose music I find both catchy and inspiring.",
      highlights: [
        { text: 'really like', type: 'improve', note: 'Consider using stronger expressions like "passionate about"' },
        { text: 'almost every day', type: 'good', note: 'Good frequency expression' },
      ],
    },
    {
      questionId: 10,
      part: 2,
      question: "Describe a memorable concert or event you attended",
      text: "I'd like to talk about a concert I attended last year. It was a BTS concert in Seoul, and it was absolutely amazing. I went there with my best friend, and we had been planning this trip for months. The atmosphere was electric, with thousands of fans singing along. What made it special was not just the music, but also the sense of community and shared passion among everyone there.",
      feedback: {
        good: ['Well-structured response', 'Good use of descriptive language', 'Personal connection established'],
        improve: ['Could expand on emotional impact', 'Add more specific details'],
      },
      sampleAnswer: "I'd like to describe an unforgettable concert experience I had last year when I attended a BTS performance in Seoul. My best friend and I had meticulously planned this trip for several months, and the anticipation only heightened the eventual experience. The venue was filled with an electric atmosphere, with thousands of devoted fans singing in unison. What truly distinguished this experience was the profound sense of community and shared enthusiasm that permeated the entire event, creating a collective energy that was both overwhelming and deeply moving.",
      highlights: [
        { text: 'absolutely amazing', type: 'improve', note: 'Try more sophisticated expressions' },
        { text: 'electric atmosphere', type: 'good', note: 'Excellent vocabulary choice' },
        { text: 'sense of community', type: 'good', note: 'Good abstract concept' },
      ],
    },
    {
      questionId: 20,
      part: 3,
      question: "Do you think live music events will become more or less popular in the future?",
      text: "I think live music events will become more popular in the future. Even though we have streaming services and can listen to music anywhere, there's something special about being at a live concert. You can't replicate that feeling at home. Also, artists are creating more interactive experiences now, with better technology like AR and VR, which makes concerts even more exciting.",
      feedback: {
        good: ['Clear opinion stated', 'Good use of comparison', 'Relevant examples with technology'],
        improve: ['Could explore counter-arguments', 'Develop points with more depth'],
      },
      highlights: [
        { text: 'replicate', type: 'good', note: 'Excellent academic vocabulary' },
        { text: 'interactive experiences', type: 'good', note: 'Good collocation' },
      ],
    },
  ];

  const currentTranscript = mockTranscripts[selectedQuestionIndex] || mockTranscripts[0];

  /**
   * Build conversation data for ConversationPlayer
   * Combines examiner questions (with TTS audio) and user responses
   */
  const conversationData = useMemo(() => {
    const history = [];
    mockTranscripts.forEach(t => {
      // Add examiner question
      history.push({
        type: 'examiner',
        audioUrl: t.examinerAudioUrl || null,
        text: t.question,
        duration: t.examinerAudioDurationMs || null,
        questionId: t.questionId,
      });
      // Add user response
      history.push({
        type: 'user',
        audioUrl: t.audioUrl || null,
        text: t.text,
        duration: t.duration || null,
        questionId: t.questionId,
      });
    });
    return history;
  }, [mockTranscripts]);

  /**
   * Group transcripts by part
   */
  const transcriptsByPart = mockTranscripts.reduce((acc, transcript, index) => {
    const part = transcript.part || 1;
    if (!acc[part]) acc[part] = [];
    acc[part].push({ ...transcript, index });
    return acc;
  }, {});

  // Get available parts
  const availableParts = Object.keys(transcriptsByPart).map(Number).sort();

  // When activePart changes, select first question of that part
  useEffect(() => {
    const partTranscripts = transcriptsByPart[activePart];
    if (partTranscripts && partTranscripts.length > 0) {
      setSelectedQuestionIndex(partTranscripts[0].index);
    }
  }, [activePart]);

  /**
   * Toggle score detail
   */
  const toggleScoreDetail = (criterionKey) => {
    setExpandedScores(prev => ({ ...prev, [criterionKey]: !prev[criterionKey] }));
  };

  /**
   * Get score level description
   */
  const getScoreLevel = (score) => {
    if (score >= 8) return { label: 'Xuất sắc', color: '#16a34a' };
    if (score >= 7) return { label: 'Rất tốt', color: '#22c55e' };
    if (score >= 6) return { label: 'Tốt', color: '#ca8a04' };
    if (score >= 5) return { label: 'Khá', color: '#ea580c' };
    return { label: 'Cần cải thiện', color: '#dc2626' };
  };

  /**
   * Render score bar component (similar to Writing)
   */
  const renderScoreBar = (label, key, score, comment) => {
    const level = getScoreLevel(score);
    const isExpanded = expandedScores[key];
    const widthPercent = (score / 9) * 100;

    return (
      <div className={`score-criterion ${isExpanded ? 'expanded' : ''}`} key={key}>
        <button
          className="score-criterion-header"
          onClick={() => toggleScoreDetail(key)}
        >
          <div className="criterion-info">
            <span className="criterion-label">{label}</span>
            <div className="criterion-bar-container">
              <div
                className="criterion-bar"
                style={{ width: `${widthPercent}%`, backgroundColor: level.color }}
              />
            </div>
          </div>
          <div className="criterion-score">
            <span className="score-value" style={{ color: level.color }}>{score ? score.toFixed(1) : 'N/A'}</span>
            <span className="expand-icon">{isExpanded ? '▼' : '▶'}</span>
          </div>
        </button>
        {isExpanded && (
          <div className="criterion-comment">
            <p>{comment || 'Không có nhận xét chi tiết.'}</p>
          </div>
        )}
      </div>
    );
  };

  /**
   * Format course name for display
   */
  const formatSourceContext = () => {
    if (!sourceContext?.courseName) return '';
    const { courseName, testNumber } = sourceContext;
    const match = courseName.match(/^([a-zA-Z]+)(\d+)$/);
    const formattedName = match ? `${match[1].toUpperCase()} ${match[2]}` : courseName.toUpperCase();
    return testNumber ? `${formattedName} · Test ${testNumber}` : formattedName;
  };

  /**
   * Render highlighted transcript text
   */
  const renderHighlightedText = () => {
    if (!currentTranscript.highlights || currentTranscript.highlights.length === 0) {
      return <p>{currentTranscript.text}</p>;
    }

    let text = currentTranscript.text;
    const parts = [];
    let lastIndex = 0;

    const sortedHighlights = [...currentTranscript.highlights]
      .map(h => ({ ...h, startIndex: text.indexOf(h.text) }))
      .filter(h => h.startIndex !== -1)
      .sort((a, b) => a.startIndex - b.startIndex);

    sortedHighlights.forEach((highlight, idx) => {
      const startIndex = text.indexOf(highlight.text, lastIndex);
      if (startIndex === -1) return;

      if (startIndex > lastIndex) {
        parts.push(
          <span key={`text-${idx}`}>{text.slice(lastIndex, startIndex)}</span>
        );
      }

      parts.push(
        <span
          key={`highlight-${idx}`}
          className={`speaking-highlight speaking-highlight--${highlight.type}`}
          title={highlight.note}
        >
          {highlight.text}
        </span>
      );

      lastIndex = startIndex + highlight.text.length;
    });

    if (lastIndex < text.length) {
      parts.push(<span key="text-end">{text.slice(lastIndex)}</span>);
    }

    return <p>{parts}</p>;
  };

  /**
   * Handle question selection
   */
  const handleSelectQuestion = (index) => {
    setSelectedQuestionIndex(index);
  };

  /**
   * Handle navigation back
   */
  const handleBack = () => {
    if (sourceContext?.courseName) {
      navigate(`/courses/${sourceContext.courseName}`);
    } else {
      navigate('/courses');
    }
  };

  // Get band class for color coding
  const getBandClass = (band) => `band-${Math.floor(band || 0)}`;

  // Show loading state
  if (isLoading) {
    return (
      <div className="speaking-results speaking-results--loading">
        <div className="speaking-results__loading-container">
          <FiLoader className="speaking-results__loading-spinner" />
          <p>Đang tải kết quả...</p>
        </div>
      </div>
    );
  }

  // Show AI grading in progress screen (similar to Writing)
  // Display when session has data but evaluation not complete yet
  if (evaluationPending) {
    return (
      <div className="speaking-results speaking-results--grading">
        <div className="speaking-results__grading-container">
          <motion.div
            className="speaking-results__grading-icon"
            animate={{ rotate: 360 }}
            transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
          >
            <FiMic size={48} />
          </motion.div>
          <h2 className="speaking-results__grading-title">Đang chấm điểm bài nói...</h2>
          <p className="speaking-results__grading-subtitle">
            AI đang phân tích bài nói của bạn. Quá trình này có thể mất vài giây.
          </p>
          <div className="speaking-results__grading-steps">
            <motion.div
              className="speaking-results__grading-step active"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
            >
              <FiCheckCircle size={16} />
              <span>Đã nhận bài nói</span>
            </motion.div>
            <motion.div
              className="speaking-results__grading-step active"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4 }}
            >
              <FiLoader className="speaking-results__step-spinner" size={16} />
              <span>Đang phân tích Fluency & Coherence</span>
            </motion.div>
            <motion.div
              className="speaking-results__grading-step"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 0.5, x: 0 }}
              transition={{ delay: 0.6 }}
            >
              <span className="speaking-results__step-dot" />
              <span>Đang phân tích Lexical Resource</span>
            </motion.div>
            <motion.div
              className="speaking-results__grading-step"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 0.5, x: 0 }}
              transition={{ delay: 0.8 }}
            >
              <span className="speaking-results__step-dot" />
              <span>Đang phân tích Grammar & Pronunciation</span>
            </motion.div>
            <motion.div
              className="speaking-results__grading-step"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 0.5, x: 0 }}
              transition={{ delay: 1.0 }}
            >
              <span className="speaking-results__step-dot" />
              <span>Tổng hợp kết quả</span>
            </motion.div>
          </div>
        </div>
      </div>
    );
  }

  // Show grading failed screen
  if (gradingFailed) {
    return (
      <div className="speaking-results speaking-results--grading">
        <div className="speaking-results__grading-container">
          <div className="speaking-results__grading-icon speaking-results__grading-icon--failed">
            <FiAlertCircle size={48} />
          </div>
          <h2 className="speaking-results__grading-title">Chấm điểm không thành công</h2>
          <p className="speaking-results__grading-subtitle">
            Hệ thống AI không thể hoàn thành việc chấm điểm. Điều này có thể do:
          </p>
          <ul className="speaking-results__error-list">
            <li>Chất lượng audio không đủ tốt để phân tích</li>
            <li>Kết nối mạng không ổn định</li>
            <li>Hệ thống AI đang quá tải</li>
          </ul>
          <div className="speaking-results__error-actions">
            <button className="speaking-results__retry-btn" onClick={handleBack}>
              <FiArrowLeft size={14} /> Quay lại làm bài mới
            </button>
          </div>
          <p className="speaking-results__grading-subtitle" style={{ marginTop: '1rem', fontSize: '0.875rem' }}>
            💡 Mẹo: Sử dụng tai nghe để tránh tiếng vọng và đảm bảo nói rõ ràng vào microphone.
          </p>
        </div>
      </div>
    );
  }

  // Show fetch error with fallback data notice
  const showErrorBanner = fetchError && !evaluation;

  return (
    <div className="speaking-results">
      {/* Evaluation Pending Banner */}
      {evaluationPending && (
        <div className="speaking-results__pending-banner">
          <FiLoader className="speaking-results__pending-spinner" />
          <span>Đang chấm điểm bài nói... Vui lòng đợi trong giây lát.</span>
        </div>
      )}

      {/* Purple Header - Unified Design (matching Writing) */}
      <header className="speaking-results__header-unified">
        <div className="speaking-results__header-top">
          <div className="speaking-results__header-left">
            <button className="speaking-results__back-btn-unified" onClick={handleBack}>
              <FiArrowLeft size={14} /> Quay lại
            </button>
            <h1 className="speaking-results__title">
              {formatSourceContext() || 'Speaking Test'} · Speaking
            </h1>
          </div>
          <div className="speaking-results__header-center">
            <div className="speaking-results__summary-item">
              <span className="speaking-results__summary-label">THỜI GIAN LÀM</span>
              <span className="speaking-results__summary-value">
                {new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>
            <div className="speaking-results__summary-item">
              <span className="speaking-results__summary-label">NGÀY LÀM</span>
              <span className="speaking-results__summary-value">
                {new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })}
              </span>
            </div>
          </div>
          <div className="speaking-results__header-right">
            <div className="speaking-results__header-actions">
              <button className="speaking-results__btn speaking-results__btn-download">
                <FiDownload size={14} /> Tải PDF
              </button>
              <Link to="/dashboard" className="speaking-results__btn speaking-results__btn-secondary">
                Dashboard
              </Link>
            </div>
            <div className="speaking-results__band-badge-unified">
              <span className="speaking-results__badge-label">BAND</span>
              <span className={`speaking-results__badge-value ${getBandClass(mockEvaluation.overallBand || 0)}`}>
                {(mockEvaluation.overallBand ?? 0).toFixed(1)}
              </span>
            </div>
          </div>
        </div>
      </header>

      {/* Part Tabs (similar to Task Tabs in Writing) */}
      <div className="speaking-results__part-tabs">
        {availableParts.map(partNum => (
          <button
            key={partNum}
            className={`speaking-results__part-tab ${activePart === partNum ? 'active' : ''}`}
            onClick={() => setActivePart(partNum)}
          >
            <span className="speaking-results__part-name">Part {partNum}</span>
            <span className={`speaking-results__part-count`}>
              {transcriptsByPart[partNum]?.length || 0} câu
            </span>
          </button>
        ))}
      </div>

      {/* Collapsible Score Bar (matching Writing) */}
      <div className={`speaking-results__scores-bar-wrapper ${scoresBarCollapsed ? 'collapsed' : ''}`}>
        <button
          className="speaking-results__scores-bar-toggle"
          onClick={() => setScoresBarCollapsed(!scoresBarCollapsed)}
        >
          <span className="speaking-results__toggle-label">
            {scoresBarCollapsed ? <><FiChevronRight size={14} /> Hiển thị điểm thành phần</> : <><FiChevronDown size={14} /> Ẩn điểm thành phần</>}
          </span>
          <div className="speaking-results__band-mini">
            <span className="speaking-results__mini-label">Overall</span>
            <span className={`speaking-results__mini-value ${getBandClass(mockEvaluation.overallBand || 0)}`}>
              {(mockEvaluation.overallBand ?? 0).toFixed(1)}
            </span>
          </div>
        </button>
        {!scoresBarCollapsed && (
          <div className="speaking-results__scores-bar">
            <div className="speaking-results__scores-grid">
              {renderScoreBar('Fluency & Coherence', 'fluency', mockEvaluation.criteria?.fluency?.band ?? 0, mockEvaluation.criteria?.fluency?.notes || 'Nói trôi chảy với ít do dự, sử dụng liên từ tự nhiên.')}
              {renderScoreBar('Lexical Resource', 'lexical', mockEvaluation.criteria?.lexical?.band ?? 0, mockEvaluation.criteria?.lexical?.notes || 'Vốn từ vựng khá, cần mở rộng thêm collocations.')}
              {renderScoreBar('Grammatical Range & Accuracy', 'grammar', mockEvaluation.criteria?.grammar?.band ?? 0, mockEvaluation.criteria?.grammar?.notes || 'Sử dụng được nhiều cấu trúc, đôi khi còn lỗi nhỏ.')}
              {renderScoreBar('Pronunciation', 'pronunciation', mockEvaluation.criteria?.pronunciation?.band ?? 0, mockEvaluation.criteria?.pronunciation?.notes || 'Phát âm rõ ràng, ngữ điệu tự nhiên.')}
            </div>
            <div className="speaking-results__band-summary">
              <span className="speaking-results__summary-label-alt">Overall</span>
              <span className={`speaking-results__summary-value-alt ${getBandClass(mockEvaluation.overallBand || 0)}`}>
                {(mockEvaluation.overallBand ?? 0).toFixed(1)}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Main Content - Three Column Resizable Layout */}
      <div className="speaking-results__main">
        <PanelGroup direction="horizontal" className="speaking-results__panel-group">
          {/* Left Panel: Transcript Navigation */}
          <Panel defaultSize={25} minSize={15} maxSize={35}>
            <div className="speaking-results__panel speaking-results__panel--transcript">
              <div className="speaking-results__panel-header">
                <h2 className="speaking-results__panel-title">📝 Transcript</h2>
              </div>
              <div className="speaking-results__panel-content">
                <div className="speaking-results__transcript-nav">
                  {/* Only show questions for active part */}
                  <div className="speaking-results__question-list">
                    {(transcriptsByPart[activePart] || []).map((transcript) => (
                      <button
                        key={transcript.index}
                        className={`speaking-results__question-item ${
                          selectedQuestionIndex === transcript.index ? 'active' : ''
                        }`}
                        onClick={() => handleSelectQuestion(transcript.index)}
                      >
                        <span className="speaking-results__question-num">Q{transcript.index + 1}</span>
                        <span className="speaking-results__question-preview">
                          {transcript.text.slice(0, 40)}...
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </Panel>

          <PanelResizeHandle className="resize-handle">
            <div className="resize-handle-icon-container">
              <span className="resize-handle-icon">↔</span>
            </div>
          </PanelResizeHandle>

          {/* Middle Panel: User Response */}
          <Panel defaultSize={40} minSize={30}>
            <div className="speaking-results__panel speaking-results__panel--response" ref={responseColumnRef}>
              <div className="speaking-results__panel-header">
                <h2 className="speaking-results__panel-title">🎤 Bài nói của bạn</h2>
              </div>
              <div className="speaking-results__panel-content">
                <div className="speaking-results__response-content">
                  {/* Question Display */}
                  {currentTranscript.question && (
                    <div className="speaking-results__question-display">
                      <span className="speaking-results__question-label">QUESTION:</span>
                      <p className="speaking-results__question-text">{currentTranscript.question}</p>
                    </div>
                  )}

                  {/* Audio Player - Full Conversation Replay */}
                  <ConversationPlayer
                    conversation={conversationData}
                    highlightedQuestionId={currentTranscript?.questionId}
                  />

                  {/* Highlight Legend */}
                  <div className="speaking-results__highlight-legend">
                    <span className="speaking-results__legend-item good">
                      <span className="speaking-results__legend-dot" /> Từ vựng/diễn đạt tốt
                    </span>
                    <span className="speaking-results__legend-item improve">
                      <span className="speaking-results__legend-dot" /> Cần cải thiện
                    </span>
                  </div>

                  {/* Transcript Text with Highlights */}
                  <div className="speaking-results__transcript-text">
                    {renderHighlightedText()}
                  </div>
                </div>
              </div>
            </div>
          </Panel>

          <PanelResizeHandle className="resize-handle">
            <div className="resize-handle-icon-container">
              <span className="resize-handle-icon">↔</span>
            </div>
          </PanelResizeHandle>

          {/* Right Panel: Analysis (reorganized like Writing) */}
          <Panel defaultSize={35} minSize={25}>
            <div className="speaking-results__panel speaking-results__panel--analysis">
              <div className="speaking-results__panel-header">
                <h2 className="speaking-results__panel-title">📊 Phân tích chi tiết</h2>
              </div>
              <div className="speaking-results__panel-content">
                <div className="speaking-results__analysis-content">
                  {/* Grading Quota Info */}
                  <div className="speaking-results__quota-info">
                    <FiCheckCircle className="speaking-results__quota-icon" />
                    <div className="speaking-results__quota-text">
                      <span className="speaking-results__quota-title">Lượt chấm AI trong tháng</span>
                      <span className="speaking-results__quota-count">
                        <strong>20</strong> / 20 lượt còn lại
                      </span>
                    </div>
                    <div className="speaking-results__quota-bar">
                      <div className="speaking-results__quota-fill" style={{ width: '100%' }}></div>
                    </div>
                  </div>

                  {/* Feedback Summary Cards (similar to Writing) */}
                  <div className="speaking-results__feedback-cards">
                    {/* Điểm mạnh */}
                    {mockEvaluation.strengths?.length > 0 && (
                      <div className="speaking-results__feedback-card strengths">
                        <h4><FiThumbsUp size={14} /> Điểm mạnh</h4>
                        <ul>
                          {mockEvaluation.strengths.map((s, i) => (
                            <li key={i}>{s}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Điểm yếu */}
                    {mockEvaluation.weaknesses?.length > 0 && (
                      <div className="speaking-results__feedback-card weaknesses">
                        <h4><FiAlertTriangle size={14} /> Điểm yếu</h4>
                        <ul>
                          {mockEvaluation.weaknesses.map((w, i) => (
                            <li key={i}>{w}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Hướng dẫn cải thiện */}
                    {mockEvaluation.suggestions?.length > 0 && (
                      <div className="speaking-results__feedback-card tips">
                        <h4><FiZap size={14} /> Hướng dẫn cải thiện</h4>
                        <ul>
                          {mockEvaluation.suggestions.map((tip, i) => (
                            <li key={i}>{tip}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Ví dụ cải thiện */}
                    {mockEvaluation.improvements?.length > 0 && (
                      <div className="speaking-results__feedback-card improvements">
                        <h4><FiBookOpen size={14} /> Ví dụ cải thiện</h4>
                        <div className="speaking-results__improvements-list">
                          {mockEvaluation.improvements.map((imp, i) => (
                            <div key={i} className="speaking-results__improvement-item">
                              <div className="speaking-results__improvement-original">
                                <span className="speaking-results__imp-label">Bản gốc:</span>
                                <span className="speaking-results__imp-text">{imp.original}</span>
                              </div>
                              <div className="speaking-results__improvement-improved">
                                <span className="speaking-results__imp-label">Cải thiện:</span>
                                <span className="speaking-results__imp-text">{imp.improved}</span>
                              </div>
                              {imp.explanation && (
                                <div className="speaking-results__improvement-explanation">
                                  <FiTarget size={12} /> {imp.explanation}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Sample Answer (if available for current question) */}
                  {currentTranscript.sampleAnswer && (
                    <div className="speaking-results__sample-answer">
                      <h4 className="speaking-results__sample-title">📝 Câu trả lời mẫu</h4>
                      <p className="speaking-results__sample-text">
                        {currentTranscript.sampleAnswer}
                      </p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </Panel>
        </PanelGroup>
      </div>
    </div>
  );
}
