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

  const content = generationResult?.content || {};
  const warnings = generationResult?.warnings || [];
  const validation = generationResult?.validation || null;
  const metadata = generationResult?.metadata || null;
  const questions = content.questions || [];
  const section = content.section || {};
  const isWriting = formData.skill === 'WRITING';
  const reasoningText = generationResult?.reasoning || reasoning;

  const questionIssues = new Map();
  const addIssue = (message) => {
    if (!message) return;
    const match = message.match(/Question\s+(\d+)/i);
    if (!match) return;
    const num = parseInt(match[1], 10);
    if (!questionIssues.has(num)) questionIssues.set(num, []);
    questionIssues.get(num).push(message);
  };

  (warnings || []).forEach(addIssue);
  if (validation) {
    (validation.schemaErrors || []).forEach(addIssue);
    (validation.contentErrors || []).forEach(addIssue);
    (validation.businessRuleErrors || []).forEach(addIssue);
  }

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

    // Additional safety: filter out invalid questions
    const validQuestions = questions.filter(q => q && typeof q === 'object');
    if (validQuestions.length === 0) return grouped;

    const layoutBlocks = section.sectionLayout?.blocks
      || (Array.isArray(section.sectionLayout) ? section.sectionLayout : null);

    if (layoutBlocks && Array.isArray(layoutBlocks)) {
      return layoutBlocks.map((block, idx) => {
        const numbers = block.question_numbers || block.questionNumbers || [];
        const blockQuestions = numbers.length > 0
          ? numbers.map(num => validQuestions.find(q => q.questionNumber === num)).filter(Boolean)
          : validQuestions;

        // Handle empty block questions
        if (blockQuestions.length === 0) {
          return {
            type: block.block_type || 'UNKNOWN',
            blockType: block.block_type,
            blockContent: block.content || {},
            questions: [],
            startNum: 1,
            endNum: 1,
            blockIndex: idx
          };
        }

        const startNum = blockQuestions[0]?.questionNumber || 1;
        const endNum = blockQuestions[blockQuestions.length - 1]?.questionNumber || startNum;

        return {
          type: block.block_type || blockQuestions[0]?.questionType || 'UNKNOWN',
          blockType: block.block_type,
          blockContent: block.content || {},
          questions: blockQuestions,
          startNum,
          endNum,
          blockIndex: idx
        };
      }).filter(g => g.questions.length > 0);  // Filter out empty groups
    }

    // No layout blocks - group by question type
    const firstQuestion = validQuestions[0];
    let currentGroup = {
      type: firstQuestion?.questionType || 'UNKNOWN',
      questions: [firstQuestion],
      startNum: firstQuestion?.questionNumber || 1
    };

    for (let i = 1; i < validQuestions.length; i++) {
      const q = validQuestions[i];
      if (!q) continue;  // Skip null/undefined questions

      if (q.questionType === currentGroup.type) {
        currentGroup.questions.push(q);
      } else {
        currentGroup.endNum = currentGroup.questions[currentGroup.questions.length - 1]?.questionNumber || i;
        grouped.push(currentGroup);
        currentGroup = {
          type: q.questionType || 'UNKNOWN',
          questions: [q],
          startNum: q.questionNumber || i + 1
        };
      }
    }
    currentGroup.endNum = currentGroup.questions[currentGroup.questions.length - 1]?.questionNumber || validQuestions.length;
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
    const passageText = section.taskText || section.passageText || section.transcript || '';
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
          {formData.skill === 'LISTENING' && content.audioPlaceholder && (
            <div className="studio-audio-card">
              <div className="studio-audio-card__icon">
                <FiCpu size={24} />
              </div>
              <div className="studio-audio-card__info">
                <strong>Audio Specification</strong>
                <p>
                  {content.audioPlaceholder.speakerCount || 'Unknown'} speakers |
                  {content.audioPlaceholder.accentRecommendation || 'Standard'} accent |
                  ~{content.audioPlaceholder.durationEstimate || '0:00'}
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
                questionIssues={questionIssues}
              />
            ))
          )}
        </div>
      </div>
    );
  };

  const renderValidationPanel = () => {
    const schemaErrors = validation?.schemaErrors || [];
    const contentErrors = validation?.contentErrors || [];
    const businessErrors = validation?.businessRuleErrors || [];
    const allWarnings = warnings || [];

    if (schemaErrors.length === 0 && contentErrors.length === 0
      && businessErrors.length === 0 && allWarnings.length === 0) {
      return null;
    }

    return (
      <div className="studio-alerts">
        {schemaErrors.length > 0 && (
          <div className="studio-alerts__section">
            <h4 className="studio-alerts__title">Schema Errors</h4>
            <ul>
              {schemaErrors.map((err, idx) => <li key={`schema-${idx}`}>{err}</li>)}
            </ul>
          </div>
        )}
        {contentErrors.length > 0 && (
          <div className="studio-alerts__section">
            <h4 className="studio-alerts__title">Content Errors</h4>
            <ul>
              {contentErrors.map((err, idx) => <li key={`content-${idx}`}>{err}</li>)}
            </ul>
          </div>
        )}
        {businessErrors.length > 0 && (
          <div className="studio-alerts__section">
            <h4 className="studio-alerts__title">Business Rule Errors</h4>
            <ul>
              {businessErrors.map((err, idx) => <li key={`business-${idx}`}>{err}</li>)}
            </ul>
          </div>
        )}
        {allWarnings.length > 0 && (
          <div className="studio-alerts__section studio-alerts__section--warning">
            <h4 className="studio-alerts__title">Warnings</h4>
            <ul>
              {allWarnings.map((warn, idx) => <li key={`warn-${idx}`}>{warn}</li>)}
            </ul>
          </div>
        )}
      </div>
    );
  };

  const renderReasoningPanel = () => {
    if (!reasoningText) return null;

    return (
      <div className="studio-reasoning">
        <div className="studio-reasoning__header">
          <span>AI Reasoning</span>
          <button
            className="studio-reasoning__toggle"
            onClick={() => setShowReasoningPanel(!showReasoningPanel)}
          >
            {showReasoningPanel ? 'Hide' : 'Show'}
          </button>
        </div>
        {showReasoningPanel && (
          <pre className="studio-reasoning__content">{reasoningText}</pre>
        )}
      </div>
    );
  };

  // Writing Layout (Single Panel)
  if (isWriting) {
    return (
      <div className="studio-preview">
        <MetadataBar />
        {renderValidationPanel()}
        {renderReasoningPanel()}
        <div className="studio-panel__content" style={{ flex: 1, overflow: 'auto' }}>
          {renderLeftPanel()}
          {(content.chartData || content.figureDescription || content.letterContext || content.essayMetadata) && (
            <div className="studio-json-panel" style={{ margin: '16px' }}>
              <div className="studio-json-header">
                <span className="studio-json-header__title">Writing Details</span>
              </div>
              <div className="studio-json-content">
                {content.taskType && <pre>task_type: {content.taskType}</pre>}
                {content.wordRequirement && <pre>word_requirement: {content.wordRequirement}</pre>}
                {content.chartData && <pre>{JSON.stringify(content.chartData, null, 2)}</pre>}
                {content.figureDescription && <pre>{JSON.stringify(content.figureDescription, null, 2)}</pre>}
                {content.letterContext && <pre>{JSON.stringify(content.letterContext, null, 2)}</pre>}
                {content.essayMetadata && <pre>{JSON.stringify(content.essayMetadata, null, 2)}</pre>}
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
        {renderValidationPanel()}
        {renderReasoningPanel()}

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
