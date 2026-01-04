/**
 * StepPreview - Preview & Edit generated content
 * 
 * V6.0: Refactored to reuse AdminPreviewContent for consistent UI
 * - Uses same test-taking UI as admin editor preview
 * - Removes ~200 lines of duplicate rendering code
 * - Adds MetadataBar, ValidationPanel, ReasoningPanel wrappers
 * 
 * @since 2025-12-22
 */

import React, { useState, useMemo } from 'react';
import { FiInfo, FiChevronUp, FiChevronDown, FiClock, FiCpu, FiFileText, FiAlertTriangle } from 'react-icons/fi';
import useABTSStore from '../../stores/useABTSStore';
import AdminPreviewContent from '../content/AdminPreviewContent';
import StreamingDisplay from './StreamingDisplay';
import './AIStudio.css';

export default function StepPreview({ onBack }) {
  const {
    formData,
    generationResult,
    isGenerating,
    goToStep,
    streamEvents,
    generationProgress,
    generationError,
    reasoning,
    abortGeneration
  } = useABTSStore();

  const [showMetadata, setShowMetadata] = useState(false);
  const [showReasoningPanel, setShowReasoningPanel] = useState(false);
  const [activePartIndex, setActivePartIndex] = useState(0);
  const [showAnswers, setShowAnswers] = useState(true);

  // Use onBack if provided, otherwise fallback to store's goToStep
  const handleGoBack = onBack || (() => goToStep(1));

  const content = generationResult?.content || {};
  const warnings = generationResult?.warnings || [];
  const validation = generationResult?.validation || null;
  const metadata = generationResult?.metadata || null;
  const isWriting = formData.skill === 'WRITING';
  const reasoningText = generationResult?.reasoning || reasoning;

  // Transform ABTS data to AdminPreviewContent format
  const { previewSections, previewQuestions } = useMemo(() => {
    const rawSections = content.sections || (content.section ? [content.section] : []);
    const rawQuestions = content.questions || [];

    // Transform sections: ABTS format -> AdminPreviewContent format
    const transformedSections = rawSections.map((sec, idx) => ({
      id: `abts-section-${idx}`,
      partNumber: sec.partNumber || idx + 1,
      passageText: sec.passageText || sec.taskText || sec.transcript || '',
      sectionLayout: sec.sectionLayout,
      wordCount: sec.wordCount,
      displayContentUrl: null, // AI-generated doesn't have images yet
      audioUrl: null // AI-generated doesn't have audio yet
    }));

    // Transform questions: ABTS format -> AdminPreviewContent format  
    const transformedQuestions = rawQuestions.map((q, idx) => {
      // Determine which section this question belongs to (by question number range)
      let sectionId = transformedSections[0]?.id;
      if (transformedSections.length > 1) {
        const qNum = q.questionNumber || q.question_number || idx + 1;
        // Reading: Part 1 = Q1-13, Part 2 = Q14-26, Part 3 = Q27-40
        // Listening: Part 1 = Q1-10, Part 2 = Q11-20, Part 3 = Q21-30, Part 4 = Q31-40
        if (formData.skill === 'READING') {
          if (qNum <= 13) sectionId = transformedSections[0]?.id;
          else if (qNum <= 26) sectionId = transformedSections[1]?.id || transformedSections[0]?.id;
          else sectionId = transformedSections[2]?.id || transformedSections[0]?.id;
        } else if (formData.skill === 'LISTENING') {
          if (qNum <= 10) sectionId = transformedSections[0]?.id;
          else if (qNum <= 20) sectionId = transformedSections[1]?.id || transformedSections[0]?.id;
          else if (qNum <= 30) sectionId = transformedSections[2]?.id || transformedSections[0]?.id;
          else sectionId = transformedSections[3]?.id || transformedSections[0]?.id;
        }
      }

      return {
        id: `abts-q-${idx}`,
        sectionId,
        questionNumber: q.questionNumber || q.question_number || idx + 1,
        questionType: q.questionType || q.question_type || 'UNKNOWN',
        questionContent: q.questionContent || q.question_content || {},
        correctAnswer: formatCorrectAnswer(q.correctAnswer || q.correct_answer),
        explanation: q.explanation || ''
      };
    });

    return { previewSections: transformedSections, previewQuestions: transformedQuestions };
  }, [content, formData.skill]);

  // Format correct answer for display
  function formatCorrectAnswer(answer) {
    if (!answer) return '';
    if (Array.isArray(answer)) return answer.join(', ');
    if (typeof answer === 'object') return JSON.stringify(answer);
    return String(answer);
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

  // Metadata Bar Component
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

  // Validation Panel Component
  const ValidationPanel = () => {
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
            <h4 className="studio-alerts__title"><FiAlertTriangle /> Schema Errors</h4>
            <ul>
              {schemaErrors.map((err, idx) => <li key={`schema-${idx}`}>{err}</li>)}
            </ul>
          </div>
        )}
        {contentErrors.length > 0 && (
          <div className="studio-alerts__section">
            <h4 className="studio-alerts__title"><FiAlertTriangle /> Content Errors</h4>
            <ul>
              {contentErrors.map((err, idx) => <li key={`content-${idx}`}>{err}</li>)}
            </ul>
          </div>
        )}
        {businessErrors.length > 0 && (
          <div className="studio-alerts__section">
            <h4 className="studio-alerts__title"><FiAlertTriangle /> Business Rule Errors</h4>
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

  // Reasoning Panel Component
  const ReasoningPanel = () => {
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

  // Writing has special layout (no AdminPreviewContent for now)
  if (isWriting) {
    const taskText = content.section?.taskText || content.section?.passageText || '';
    return (
      <div className="studio-preview">
        <MetadataBar />
        <ValidationPanel />
        <ReasoningPanel />
        <div className="studio-panel__content" style={{ flex: 1, overflow: 'auto', padding: '16px' }}>
          <h3>Writing Task</h3>
          <div dangerouslySetInnerHTML={{ __html: taskText }} />
          {(content.chartData || content.figureDescription || content.letterContext || content.essayMetadata) && (
            <div className="studio-json-panel" style={{ marginTop: '16px' }}>
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

  // Reading / Listening - Use AdminPreviewContent
  return (
    <div className="studio-preview">
      <MetadataBar />
      <ValidationPanel />
      <ReasoningPanel />

      <div style={{ flex: 1, overflow: 'hidden' }}>
        <AdminPreviewContent
          sections={previewSections}
          questions={previewQuestions}
          activePartIndex={activePartIndex}
          skill={formData.skill?.toLowerCase() || 'reading'}
          onPartSelect={setActivePartIndex}
          onQuestionSelect={() => { }}
          onQuestionEdit={() => { }}
          showAnswers={showAnswers}
          onToggleAnswers={() => setShowAnswers(!showAnswers)}
        />
      </div>
    </div>
  );
}
