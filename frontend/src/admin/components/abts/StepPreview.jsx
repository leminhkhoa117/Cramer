/**
 * StepPreview - Preview & Edit generated content
 * 
 * V5.0: Uses unified AIStudio.css, no inline styles
 * - Split-pane layout mimicking test-taking UI
 * - Dark purple admin theme
 * - Hybrid approach: test-taking structure with admin styling
 * 
 * @since 2025-12-22
 */

import React, { useState } from 'react';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import { FiInfo, FiChevronUp, FiChevronDown, FiClock, FiCpu, FiFileText, FiHelpCircle } from 'react-icons/fi';
import useABTSStore from '../../stores/useABTSStore';
import { sanitizeHtml } from '../../utils/htmlSanitizer';
import QuestionGroupRenderer from './QuestionGroupRenderer';
import { HighlightProvider } from '../../../contexts/HighlightContext';
import StreamingDisplay from './StreamingDisplay';
import './AIStudio.css';

export default function StepPreview({ onBack }) {
  const {
    formData,
    generationResult,
    isGenerating,
    goToStep,
    regenerateQuestions,
    streamEvents,
    generationProgress,
    generationError,
    reasoning,
    abortGeneration
  } = useABTSStore();

  const [showMetadata, setShowMetadata] = useState(false);
  const [previewAnswers, setPreviewAnswers] = useState({});
  const [regeneratingQuestionId, setRegeneratingQuestionId] = useState(null);
  const [showReasoningPanel, setShowReasoningPanel] = useState(false);

  // Use onBack if provided, otherwise fallback to store's goToStep
  const handleGoBack = onBack || (() => goToStep(1));

  // 1. Loading / Streaming State
  if (isGenerating) {
    return (
      <div className="studio-preview">
        <StreamingDisplay
          isActive={isGenerating}
          events={streamEvents}
          progress={generationProgress}
          error={generationError}
          reasoning={reasoning}
          onAbort={abortGeneration}
          onBack={handleGoBack}
        />
      </div>
    );
  }

  // 2. Empty State
  if (!generationResult || !generationResult.content) {
    return (
      <div className="studio-empty">
        <div className="studio-empty__icon">
          <FiFileText size={48} />
        </div>
        <h3 className="studio-empty__title">No Content Generated</h3>
        <p className="studio-empty__text">
          Please go back to Configure step and generate content.
        </p>
        <button className="studio-btn studio-btn--primary" onClick={handleGoBack}>
          Back to Configure
        </button>
      </div>
    );
  }

  const { content, metadata } = generationResult;
  const { questions = [], section = {} } = content;
  const isWriting = formData.skill === 'WRITING';

  const handleAnswerChange = (questionId, value) => {
    setPreviewAnswers(prev => ({
      ...prev,
      [questionId]: value
    }));
  };

  const handleRegenerateQuestion = async (questionNum) => {
    if (!regenerateQuestions) return;
    setRegeneratingQuestionId(questionNum);
    try {
      await regenerateQuestions([questionNum]);
    } catch (error) {
      console.error("Failed to regenerate question:", error);
    } finally {
      setRegeneratingQuestionId(null);
    }
  };

  // Group questions by type
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

  // Metadata Bar
  const MetadataBar = () => (
    <div className="studio-meta">
      <div
        className="studio-meta__header"
        onClick={() => setShowMetadata(!showMetadata)}
      >
        <div className="studio-meta__title">
          <FiInfo size={14} />
          <span>Generation Info</span>
          <div className="studio-meta__badges">
            <span className="studio-meta__badge studio-meta__badge--model">
              {metadata?.modelUsed || 'AI Model'}
            </span>
            {metadata?.estimatedCostUsd && (
              <span className="studio-meta__badge studio-meta__badge--cost">
                ${metadata.estimatedCostUsd.toFixed(4)}
              </span>
            )}
          </div>
        </div>
        <button className="studio-meta__toggle">
          {showMetadata ? <FiChevronUp size={16} /> : <FiChevronDown size={16} />}
        </button>
      </div>

      {showMetadata && (
        <div className="studio-meta__details">
          <div className="studio-meta__grid">
            <div className="studio-meta__item">
              <span className="studio-meta__item-label">
                <FiFileText size={12} /> Topic
              </span>
              <span className="studio-meta__item-value">
                {metadata?.topic || formData.topic}
              </span>
            </div>
            <div className="studio-meta__item">
              <span className="studio-meta__item-label">Difficulty</span>
              <span className="studio-meta__item-value">{metadata?.difficulty}</span>
            </div>
            <div className="studio-meta__item">
              <span className="studio-meta__item-label">
                <FiCpu size={12} /> Tokens
              </span>
              <span className="studio-meta__item-value">
                {(metadata?.promptTokens || 0) + (metadata?.completionTokens || 0)}
                {' '}(P: {metadata?.promptTokens} / C: {metadata?.completionTokens})
              </span>
            </div>
            <div className="studio-meta__item">
              <span className="studio-meta__item-label">
                <FiClock size={12} /> Time
              </span>
              <span className="studio-meta__item-value">
                {metadata?.generationTimeSeconds?.toFixed(1)}s
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );

  // Left Panel Content (Passage/Transcript)
  const renderLeftPanel = () => {
    const passageText = section.passageText || section.transcript || section.taskText || '';
    const displayWordCount = section.wordCount ||
      (passageText ? passageText.replace(/<[^>]*>/g, '').split(/\s+/).filter(w => w.length > 0).length : 0);

    return (
      <div className="studio-split__panel studio-split__panel--left">
        <div className="studio-panel__header">
          <h4 className="studio-panel__title">
            {formData.skill === 'LISTENING' ? 'Transcript' :
              formData.skill === 'WRITING' ? 'Task Prompt' : 'Reading Passage'}
          </h4>
          {displayWordCount > 0 && (
            <span className="studio-panel__meta">
              {displayWordCount} words
            </span>
          )}
        </div>

        <div className="studio-panel__content">
          {formData.skill === 'LISTENING' && section.audio_placeholder && (
            <div className="studio-audio-card">
              <div className="studio-audio-card__icon">
                <FiCpu size={24} />
              </div>
              <div className="studio-audio-card__info">
                <strong>Audio Specification</strong>
                <p>
                  {section.audio_placeholder.speakers || 'Unknown'} speakers |
                  {section.audio_placeholder.accents || 'Standard'} accent |
                  ~{section.audio_placeholder.duration || '0:00'}
                </p>
              </div>
            </div>
          )}

          <div
            className="studio-passage"
            dangerouslySetInnerHTML={{ __html: sanitizeHtml(passageText) }}
          />
        </div>
      </div>
    );
  };

  // Right Panel Content (Questions)
  const renderRightPanel = () => {
    const groupedQuestions = getGroupedQuestions();

    return (
      <div className="studio-split__panel">
        <div className="studio-panel__header">
          <h4 className="studio-panel__title">Questions ({questions.length})</h4>
          <span className="studio-panel__meta">
            <FiHelpCircle size={12} /> Preview Mode
          </span>
        </div>

        <div className="studio-panel__content">
          {questions.length === 0 ? (
            <div className="studio-empty">
              <p className="studio-empty__text">No questions generated.</p>
            </div>
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
      <div className="studio-preview">
        <MetadataBar />
        <div className="studio-panel__content" style={{ flex: 1, overflow: 'auto' }}>
          {renderLeftPanel()}
          {section.chart_data && (
            <div className="studio-json-panel" style={{ margin: '16px' }}>
              <div className="studio-json-header">
                <span className="studio-json-header__title">Chart Data</span>
              </div>
              <div className="studio-json-content">
                <pre>{JSON.stringify(section.chart_data, null, 2)}</pre>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  // Split Layout (Reading / Listening)
  return (
    <HighlightProvider>
      <div className="studio-preview">
        <MetadataBar />

        <div className="studio-split">
          <PanelGroup direction="horizontal">
            <Panel defaultSize={50} minSize={30} order={1}>
              {renderLeftPanel()}
            </Panel>

            <PanelResizeHandle className="studio-resize-handle">
              <span className="studio-resize-handle__icon">||</span>
            </PanelResizeHandle>

            <Panel defaultSize={50} minSize={30} order={2}>
              {renderRightPanel()}
            </Panel>
          </PanelGroup>
        </div>
      </div>
    </HighlightProvider>
  );
}
