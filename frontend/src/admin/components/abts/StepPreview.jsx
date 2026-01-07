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

import React, { useState, useMemo, useCallback } from 'react';
import { FiInfo, FiChevronUp, FiChevronDown, FiClock, FiCpu, FiFileText, FiAlertTriangle, FiCopy, FiHeadphones, FiCheck, FiImage } from 'react-icons/fi';
import useABTSStore from '../../stores/useABTSStore';
import AdminPreviewContent from '../content/AdminPreviewContent';
import StreamingDisplay from './StreamingDisplay';
import QuestionEditModal from '../content/QuestionEditModal';
import IssueSelector from './IssueSelector';
import RefinementModal from './RefinementModal';
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
    abortGeneration,
    updateGeneratedQuestion
  } = useABTSStore();

  const [showMetadata, setShowMetadata] = useState(false);
  const [showReasoningPanel, setShowReasoningPanel] = useState(false);
  const [activePartIndex, setActivePartIndex] = useState(0);
  const [showAnswers, setShowAnswers] = useState(true);
  const [showQuestionEditor, setShowQuestionEditor] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState(null);
  const [showTranscriptPanel, setShowTranscriptPanel] = useState(false);
  const [copiedTranscript, setCopiedTranscript] = useState(false);
  const [isValidationExpanded, setIsValidationExpanded] = useState(null); // null = auto
  const [imageUrls, setImageUrls] = useState({}); // { partNumber: url }

  // Use onBack if provided, otherwise fallback to store's goToStep
  const handleGoBack = onBack || (() => goToStep(1));

  // Question editing handlers
  const handleQuestionEdit = (question) => {
    setEditingQuestion(question);
    setShowQuestionEditor(true);
  };

  const handleQuestionSave = async (questionId, updates) => {
    updateGeneratedQuestion(questionId, updates);
    setShowQuestionEditor(false);
    setEditingQuestion(null);
  };

  const content = generationResult?.content || {};
  const warnings = generationResult?.warnings || [];
  const validation = generationResult?.validation || null;
  const metadata = generationResult?.metadata || null;
  const isWriting = formData.skill === 'WRITING';
  const reasoningText = generationResult?.reasoning || reasoning;

  // Extract figure/image description from content (for map/plan labeling)
  const figureDescription = content.figureDescription;

  // Transform ABTS data to AdminPreviewContent format
  const { previewSections, previewQuestions } = useMemo(() => {
    const rawSections = content.sections || (content.section ? [content.section] : []);
    const rawQuestions = content.questions || [];

    // Transform sections: ABTS format -> AdminPreviewContent format
    const transformedSections = rawSections.map((sec, idx) => {
      // Filter out blocks with invalid/missing question_numbers
      let filteredLayout = sec.sectionLayout;
      if (sec.sectionLayout?.blocks && Array.isArray(sec.sectionLayout.blocks)) {
        const validBlocks = sec.sectionLayout.blocks.filter(block => {
          // Block must have question_numbers array with at least one element
          if (!block.question_numbers || !Array.isArray(block.question_numbers) || block.question_numbers.length === 0) {
            console.warn('[StepPreview] Filtering out block with missing question_numbers:', block);
            return false;
          }
          // Block must have valid block_type
          const validTypes = ['NOTE_COMPLETION', 'INSTRUCTIONS_ONLY', 'MATCHING_FEATURES', 'PLAN_MAP_DIAGRAM_LABELING'];
          if (!block.block_type || !validTypes.includes(block.block_type.toUpperCase())) {
            console.warn('[StepPreview] Filtering out block with invalid block_type:', block.block_type);
            return false;
          }
          return true;
        });

        // If all blocks were invalid, set layout to null to trigger fallback grouping
        filteredLayout = validBlocks.length > 0 ? { ...sec.sectionLayout, blocks: validBlocks } : null;
      }

      const partNum = sec.partNumber || idx + 1;

      return {
        id: `abts-section-${idx}`,
        partNumber: partNum,
        passageText: sec.passageText || sec.taskText || sec.transcript || '',
        sectionLayout: filteredLayout,
        wordCount: sec.wordCount,
        displayContentUrl: imageUrls[partNum] || null, // User-entered image URL
        audioUrl: null
      };
    });

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

  // Get combined transcripts for all parts (MUST be before early returns)
  const allTranscripts = useMemo(() => {
    const rawSections = content.sections || (content.section ? [content.section] : []);
    return rawSections.map((sec, idx) => ({
      partNumber: sec.partNumber || idx + 1,
      transcript: sec.transcript || sec.passageText || ''
    })).filter(s => s.transcript);
  }, [content]);

  // Copy transcript handler (MUST be before early returns)
  const handleCopyTranscript = useCallback(async () => {
    const fullTranscript = allTranscripts
      .map(s => `=== Part ${s.partNumber} ===\n\n${s.transcript}`)
      .join('\n\n');

    try {
      await navigator.clipboard.writeText(fullTranscript);
      setCopiedTranscript(true);
      setTimeout(() => setCopiedTranscript(false), 2000);
    } catch (err) {
      console.error('Failed to copy transcript:', err);
    }
  }, [allTranscripts]);

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

  // Validation Panel Component - Collapsible (uses parent state to prevent reset on re-render)
  const ValidationPanel = () => {
    const schemaErrors = validation?.schemaErrors || [];
    const contentErrors = validation?.contentErrors || [];
    const businessErrors = validation?.businessRuleErrors || [];
    const allWarnings = warnings || [];

    const totalIssues = schemaErrors.length + contentErrors.length + businessErrors.length + allWarnings.length;
    // Use parent state, with auto-expand default based on issue count
    const isExpanded = isValidationExpanded === null ? totalIssues <= 3 : isValidationExpanded;
    const handleToggle = (e) => {
      e.stopPropagation(); // Prevent event bubbling
      setIsValidationExpanded(!isExpanded);
    };

    if (totalIssues === 0) {
      return null;
    }

    return (
      <div className="studio-alerts">
        <div
          className="studio-alerts__header"
          onClick={handleToggle}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            cursor: 'pointer',
            padding: '8px 12px',
            background: 'rgba(234, 179, 8, 0.1)',
            borderRadius: '6px',
            marginBottom: isExpanded ? '8px' : '0'
          }}
        >
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', fontWeight: 600, color: '#ca8a04' }}>
            <FiAlertTriangle size={14} />
            {totalIssues} issue{totalIssues > 1 ? 's' : ''} found
          </span>
          <button
            className="studio-meta__toggle"
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#ca8a04' }}
          >
            {isExpanded ? <FiChevronUp size={16} /> : <FiChevronDown size={16} />}
          </button>
        </div>

        {isExpanded && (
          <>
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
                {/* Agent 2 Issue Selector */}
                <IssueSelector
                  issues={allWarnings.map((msg, idx) => ({ id: `warn-${idx}`, message: msg, type: 'WARNING' }))}
                  type="warning"
                />
              </div>
            )}
          </>
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

  // Check if skill is Listening
  const isListening = formData.skill === 'LISTENING';

  // Listening Tools Panel (Audio URLs + Transcript Copy)
  // Check if current part needs image (PLAN_MAP_DIAGRAM_LABELING)
  const currentPartNeedsImage = useMemo(() => {
    const currentSection = previewSections[activePartIndex];
    if (!currentSection?.sectionLayout?.blocks) return false;
    return currentSection.sectionLayout.blocks.some(
      block => block.block_type === 'PLAN_MAP_DIAGRAM_LABELING'
    );
  }, [previewSections, activePartIndex]);

  // Get current part number for image URL state
  const currentPartNumber = previewSections[activePartIndex]?.partNumber || 1;

  // Handle image URL change
  const handleImageUrlChange = useCallback((url) => {
    setImageUrls(prev => ({ ...prev, [currentPartNumber]: url }));
  }, [currentPartNumber]);

  // Listening Tools Panel (Audio URLs + Transcript Copy + Image URL)
  const ListeningToolsPanel = () => {
    if (!isListening || !generationResult?.content) return null;

    return (
      <div className="studio-listening-tools">
        {/* Figure Description & Image URL section - only when needed */}
        {(figureDescription || currentPartNeedsImage) && (
          <div className="studio-listening-tools__image-section">
            <div className="studio-listening-tools__header">
              <FiImage size={14} />
              <span>Image / Map (Part {currentPartNumber})</span>
            </div>

            {figureDescription && (
              <div className="studio-listening-tools__figure-desc">
                <details>
                  <summary>Figure Description (for manual recreation)</summary>
                  <pre>{typeof figureDescription === 'string'
                    ? figureDescription
                    : JSON.stringify(figureDescription, null, 2)}</pre>
                </details>
              </div>
            )}

            <div className="studio-listening-tools__url-input">
              <input
                type="text"
                placeholder="Enter image URL..."
                value={imageUrls[currentPartNumber] || ''}
                onChange={(e) => handleImageUrlChange(e.target.value)}
              />
              {imageUrls[currentPartNumber] && (
                <span className="studio-listening-tools__url-preview">Image loaded</span>
              )}
            </div>
          </div>
        )}

        {/* Audio URL section removed - use Save to Database modal instead */}
        <div className="studio-listening-tools__header">
          <FiHeadphones size={14} />
          <span>Transcript</span>
        </div>

        {/* Transcript Actions */}
        <div className="studio-listening-tools__actions">
          <button
            className={`studio-btn ${copiedTranscript ? 'studio-btn--success' : 'studio-btn--ghost'}`}
            onClick={handleCopyTranscript}
            disabled={allTranscripts.length === 0}
          >
            {copiedTranscript ? <FiCheck size={14} /> : <FiCopy size={14} />}
            {copiedTranscript ? 'Copied!' : 'Copy Transcript'}
          </button>
          <button
            className={`studio-btn studio-btn--ghost ${showTranscriptPanel ? 'active' : ''}`}
            onClick={() => setShowTranscriptPanel(prev => !prev)}
          >
            <FiFileText size={14} />
            {showTranscriptPanel ? 'Hide Transcript' : 'View Transcript'}
          </button>
        </div>

        {/* Transcript Preview Panel */}
        {showTranscriptPanel && (
          <div className="studio-listening-tools__transcript">
            {allTranscripts.map(section => (
              <div key={section.partNumber} className="transcript-section">
                <h4>Part {section.partNumber}</h4>
                <pre>{section.transcript}</pre>
              </div>
            ))}
          </div>
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
        <RefinementModal />
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
      <ListeningToolsPanel />
      <RefinementModal />
      <ReasoningPanel />

      <div style={{ flex: 1, overflow: 'hidden' }}>
        <AdminPreviewContent
          sections={previewSections}
          questions={previewQuestions}
          activePartIndex={activePartIndex}
          skill={formData.skill?.toLowerCase() || 'reading'}
          onPartSelect={setActivePartIndex}
          onQuestionSelect={() => { }}
          onQuestionEdit={handleQuestionEdit}
          showAnswers={showAnswers}
          onToggleAnswers={() => setShowAnswers(!showAnswers)}
        />
      </div>

      {showQuestionEditor && (
        <QuestionEditModal
          isOpen={showQuestionEditor}
          onClose={() => { setShowQuestionEditor(false); setEditingQuestion(null); }}
          question={editingQuestion}
          onSave={handleQuestionSave}
        />
      )}
    </div>
  );
}
