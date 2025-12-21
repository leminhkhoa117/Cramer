/**
 * StepPreview - Step 2: Preview & Edit generated content.
 * 
 * Uses split-pane layout mimicking the actual test-taking UI.
 * - Left Panel: Passage / Transcript / Prompt
 * - Right Panel: Questions (rendered using actual QuestionRenderer)
 * - Top Bar: Collapsible Metadata
 * - Writing: Single panel layout
 * 
 * @updated 2025-12-21 - Split-pane redesign
 */

import React, { useState, useRef } from 'react';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import { FiInfo, FiChevronUp, FiChevronDown, FiClock, FiCpu, FiDollarSign, FiAlignLeft, FiHelpCircle } from 'react-icons/fi';
import useABTSStore from '../../stores/useABTSStore';
import { sanitizeHtml } from '../../utils/htmlSanitizer';
// Use Group Renderer for shared contexts (images, options lists)
import QuestionGroupRenderer from './QuestionGroupRenderer';
import { HighlightProvider } from '../../../contexts/HighlightContext';
import '../../css/common/passage-preview.css';

export default function StepPreview() {
  const {
    formData,
    generationResult,
    generateStreaming,
    isGenerating,
    goToStep,
    closeWizard,
    regenerateQuestions // Assuming this action exists or similar
  } = useABTSStore();

  const [showMetadata, setShowMetadata] = useState(false);
  // User answers state (mock for preview interactivity)
  const [previewAnswers, setPreviewAnswers] = useState({});

  // Track regeneration state
  const [regeneratingQuestionId, setRegeneratingQuestionId] = useState(null);

  if (!generationResult || !generationResult.content) {
    return (
      <div className="step-container empty-state">
        <div className="empty-content">
          <span className="empty-icon">📭</span>
          <h3>No Content Generated</h3>
          <p>Please go back to Configure step and generate content.</p>
          <button className="btn-primary" onClick={() => goToStep(1)}>
            ← Back to Configure
          </button>
        </div>
      </div>
    );
  }

  const { content, metadata } = generationResult;
  const { questions = [], section = {} } = content;
  const isWriting = formData.skill === 'WRITING';

  // Handle answer changes in preview (just local state update)
  const handleAnswerChange = (questionId, value) => {
    setPreviewAnswers(prev => ({
      ...prev,
      [questionId]: value
    }));
  };

  // Handle regeneration request from GroupParser
  const handleRegenerateQuestion = async (questionNum) => {
    if (!regenerateQuestions) return;
    setRegeneratingQuestionId(questionNum);
    try {
      await regenerateQuestions([questionNum]);
    } catch (error) {
      console.error("Failed to regenerate question:", error);
      alert("Failed to regenerate question. See console for details.");
    } finally {
      setRegeneratingQuestionId(null);
    }
  };

  // Group questions logic
  const getGroupedQuestions = () => {
    const grouped = [];
    if (!questions || questions.length === 0) return grouped;

    let currentGroup = {
      type: questions[0].questionType,
      questions: [questions[0]],
      startNum: questions[0].questionNumber || 1
    };

    for (let i = 1; i < questions.length; i++) {
      const q = questions[i];
      if (q.questionType === currentGroup.type) {
        currentGroup.questions.push(q);
      } else {
        currentGroup.endNum = currentGroup.questions[currentGroup.questions.length - 1].questionNumber || i;
        grouped.push(currentGroup);
        currentGroup = {
          type: q.questionType,
          questions: [q],
          startNum: q.questionNumber || i + 1
        };
      }
    }
    currentGroup.endNum = currentGroup.questions[currentGroup.questions.length - 1].questionNumber || questions.length;
    grouped.push(currentGroup);
    return grouped;
  };

  // Metadata Bar Component
  const MetadataBar = () => (
    <div className={`metadata-bar ${showMetadata ? 'expanded' : ''}`}>
      <div
        className="metadata-header"
        onClick={() => setShowMetadata(!showMetadata)}
      >
        <div className="metadata-title">
          <FiInfo size={16} />
          <span>Generation Info</span>
          <span className="metadata-badge">{metadata?.modelUsed || 'AI Model'}</span>
          {metadata?.estimatedCostUsd && (
            <span className="metadata-cost-badge">
              ${metadata.estimatedCostUsd.toFixed(4)}
            </span>
          )}
        </div>
        <button className="metadata-toggle">
          {showMetadata ? <FiChevronUp /> : <FiChevronDown />}
        </button>
      </div>

      {showMetadata && (
        <div className="metadata-details">
          <div className="meta-grid">
            <div className="meta-item">
              <label><FiAlignLeft /> Topic</label>
              <span>{metadata?.topic || formData.topic}</span>
            </div>
            <div className="meta-item">
              <label>📊 Difficulty</label>
              <span>{metadata?.difficulty}</span>
            </div>
            <div className="meta-item">
              <label><FiCpu /> Tokens</label>
              <span>
                T: {(metadata?.promptTokens || 0) + (metadata?.completionTokens || 0)}
                (P: {metadata?.promptTokens} / C: {metadata?.completionTokens})
              </span>
            </div>
            <div className="meta-item">
              <label><FiClock /> Time</label>
              <span>{metadata?.generationTimeSeconds?.toFixed(1)}s</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );

  // Left Panel Content (Passage/Transcript)
  const renderLeftPanelContent = () => {
    const passageText = section.passageText || section.transcript || section.taskText || '';

    // Calculate word count if missing
    const displayWordCount = section.wordCount ||
      (passageText ? passageText.replace(/<[^>]*>/g, '').split(/\s+/).filter(w => w.length > 0).length : 0);

    return (
      <div className="left-panel-content">
        <div className="panel-header">
          <h4>
            {formData.skill === 'LISTENING' ? 'Transcript' :
              formData.skill === 'WRITING' ? 'Task Prompt' : 'Reading Passage'}
          </h4>
          <span className="word-count">
            {displayWordCount > 0 && `${displayWordCount} words`}
          </span>
        </div>

        <div className="panel-scroll-area">
          {formData.skill === 'LISTENING' && section.audio_placeholder && (
            <div className="audio-placeholder-card">
              <div className="audio-icon">🎧</div>
              <div className="audio-info">
                <strong>Audio Specification</strong>
                <p>{section.audio_placeholder.speakers || 'Unknown'} speakers • {section.audio_placeholder.accents || 'Standard'} accent</p>
                <p className="duration">~{section.audio_placeholder.duration || '0:00'}</p>
              </div>
            </div>
          )}

          <div
            className="passage-text-content"
            dangerouslySetInnerHTML={{ __html: sanitizeHtml(passageText) }}
          />
        </div>
      </div>
    );
  };

  // Right Panel Content (Questions)
  const renderRightPanelContent = () => {
    const groupedQuestions = getGroupedQuestions();

    return (
      <div className="right-panel-content">
        <div className="panel-header">
          <h4>Questions ({questions.length})</h4>
          <span className="header-hint">
            <FiHelpCircle size={14} /> Preview Mode
          </span>
        </div>

        <div className="panel-scroll-area questions-list-area">
          {questions.length === 0 ? (
            <div className="no-questions">No questions generated.</div>
          ) : (
            groupedQuestions.map((group, idx) => (
              <QuestionGroupRenderer
                key={idx}
                group={group}
                allQuestions={questions}
                userAnswers={previewAnswers}
                onAnswerChange={handleAnswerChange}
                onRegenerateQuestion={regenerateQuestions ? handleRegenerateQuestion : null}
                isGenerating={isGenerating}
                regeneratingQuestionId={regeneratingQuestionId}
              />
            ))
          )}
        </div>
      </div>
    );
  };

  // Writing Layout (Single Panel)
  if (isWriting) {
    return (
      <div className="step-preview-container writing-mode">
        <MetadataBar />
        <div className="single-panel-scroll">
          {renderLeftPanelContent()}
          {/* For Writing, maybe show Chart Data below if Task 1 */}
          {section.chart_data && (
            <div className="chart-data-wrapper">
              <h4>Chart Data</h4>
              <pre>{JSON.stringify(section.chart_data, null, 2)}</pre>
            </div>
          )}
        </div>
        <StyleSheet />
      </div>
    );
  }

  // Split Layout (Reading / Listening)
  return (
    <HighlightProvider>
      <div className="step-preview-container split-mode">
        <MetadataBar />

        <div className="split-pane-wrapper">
          <PanelGroup direction="horizontal" className="panel-group-inner">
            {/* Left Panel: Content */}
            <Panel defaultSize={50} minSize={30} order={1}>
              {renderLeftPanelContent()}
            </Panel>

            <PanelResizeHandle className="resize-handle">
              <div className="resize-handle-icon-container">
                <span className="resize-handle-icon">↔</span>
              </div>
            </PanelResizeHandle>

            {/* Right Panel: Questions */}
            <Panel defaultSize={50} minSize={30} order={2}>
              {renderRightPanelContent()}
            </Panel>
          </PanelGroup>
        </div>

        <StyleSheet />
      </div>
    </HighlightProvider>
  );
}

const StyleSheet = () => (
  <style>{`
        .step-preview-container {
            display: flex;
            flex-direction: column;
            height: 100%;
            background: transparent;
            overflow: hidden;
        }

        .empty-state {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100%;
            text-align: center;
        }
        .empty-icon { font-size: 3rem; display: block; margin-bottom: 1rem; }

        /* Metadata Bar */
        .metadata-bar {
            background: rgba(0, 0, 0, 0.2);
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
            transition: all 0.3s ease;
            flex-shrink: 0;
        }
        .metadata-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 8px 16px;
            cursor: pointer;
            color: rgba(255, 255, 255, 0.7);
        }
        .metadata-header:hover {
            background: rgba(255, 255, 255, 0.05);
            color: white;
        }
        .metadata-title {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 0.9rem;
            font-weight: 500;
        }
        .metadata-badge {
            background: rgba(168, 85, 247, 0.2);
            color: #d8b4fe;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 0.75rem;
        }
        .metadata-cost-badge {
            background: rgba(34, 197, 94, 0.1);
            color: #4ade80;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 0.75rem;
        }
        .metadata-toggle {
            background: none;
            border: none;
            color: inherit;
            cursor: pointer;
            padding: 4px;
        }
        .metadata-details {
            padding: 12px 16px;
            background: rgba(0, 0, 0, 0.2);
            border-top: 1px solid rgba(255, 255, 255, 0.05);
        }
        .meta-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 16px;
        }
        .meta-item {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }
        .meta-item label {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 0.75rem;
            color: rgba(255, 255, 255, 0.5);
            text-transform: uppercase;
        }
        .meta-item span {
            font-size: 0.9rem;
            color: rgba(255, 255, 255, 0.9);
        }

        /* Split Pane Wrapper */
        .split-pane-wrapper {
            flex: 1;
            position: relative;
            min-height: 0; /* Important for flex child scroll */
        }
        .panel-group-inner {
            height: 100% !important;
        }

        /* Panel Internal Layout */
        .left-panel-content, .right-panel-content {
            display: flex;
            flex-direction: column;
            height: 100%;
            background: rgba(255, 255, 255, 0.02);
        }
        .panel-header {
            padding: 12px 16px;
            background: rgba(255, 255, 255, 0.03);
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-shrink: 0;
        }
        .panel-header h4 {
            margin: 0;
            font-size: 0.95rem;
            color: rgba(255, 255, 255, 0.9);
        }
        .word-count, .header-hint {
            font-size: 0.8rem;
            color: rgba(255, 255, 255, 0.5);
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .panel-scroll-area {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
        }
        
        /* Writing Single Panel */
        .single-panel-scroll {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
        }
        .writing-mode .left-panel-content {
            background: transparent;
        }

        /* Content Styling */
        .passage-text-content {
            line-height: 1.8;
            color: rgba(255, 255, 255, 0.9);
            font-size: 1rem;
        }
        .passage-text-content p { margin-bottom: 1.2em; }

        /* Audio Placeholder */
        .audio-placeholder-card {
            background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(147, 51, 234, 0.1));
            border: 1px solid rgba(59, 130, 246, 0.2);
            border-radius: 8px;
            padding: 16px;
            margin-bottom: 24px;
            display: flex;
            gap: 16px;
            align-items: center;
        }
        .audio-icon { font-size: 2rem; }
        .audio-info strong { display: block; color: #93c5fd; margin-bottom: 4px; }
        .audio-info p { margin: 0; font-size: 0.9rem; color: rgba(255, 255, 255, 0.7); }

        /* Questions Styling */
        .questions-list-area {
            background: rgba(0, 0, 0, 0.1);
        }
        .preview-question-wrapper {
            background: rgba(255, 255, 255, 0.05); /* Slight card bg */
            border-radius: 8px;
            padding: 16px;
            margin-bottom: 20px;
            border: 1px solid rgba(255, 255, 255, 0.05);
        }
        
        /* Admin Footer for Question */
        .admin-question-footer {
            margin-top: 12px;
            padding-top: 12px;
            border-top: 1px solid rgba(255, 255, 255, 0.1);
            font-size: 0.9rem;
        }
        .answer-key {
            color: #86efac;
            margin-bottom: 4px;
        }
        .answer-value { font-weight: bold; margin-left: 6px; }
        .explanation-text {
            color: rgba(255, 255, 255, 0.6);
            line-height: 1.4;
        }
    `}</style>
);
