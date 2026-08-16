/**
 * StepPreview - Preview & Edit generated content
 * 
 * V6.0: Refactored to reuse AdminPreviewContent for consistent UI
 * - Uses same test-taking UI as admin editor preview
 * - Removes ~200 lines of duplicate rendering code
 * - Adds MetadataBar and ReasoningPanel wrappers
 * 
 * @since 2025-12-22
 */

import React, { useState, useMemo, useCallback } from 'react';
import { FiInfo, FiChevronUp, FiChevronDown, FiClock, FiCpu, FiFileText, FiCopy, FiHeadphones, FiCheck, FiImage } from 'react-icons/fi';
import useABTSStore from '../../stores/useABTSStore';
import AdminPreviewContent from '../content/AdminPreviewContent';
import StreamingDisplay from './StreamingDisplay';
import QuestionEditModal from '../content/QuestionEditModal';
import RefinementModal from './RefinementModal';
import './AIStudio.css';

// Format correct answer for display
function formatCorrectAnswer(answer) {
  if (!answer) return '';
  if (Array.isArray(answer)) return answer.join(', ');
  if (typeof answer === 'object') return JSON.stringify(answer);
  return String(answer);
}

export default function StepPreview({ onBack }) {
  const {
    formData,
    generationResult,
    isGenerating,
    goToStep,
    streamEvents,
    streamPreview,
    streamChunkCount,
    generationProgress,
    generationError,
    partErrors,
    reasoning,
    abortGeneration,
    updateGeneratedQuestion,
    imageUrls,
    setImageUrl
  } = useABTSStore();

  const [showMetadata, setShowMetadata] = useState(false);
  const [showReasoningPanel, setShowReasoningPanel] = useState(false);
  const [activePartIndex, setActivePartIndex] = useState(0);
  const [showAnswers, setShowAnswers] = useState(true);
  const [showQuestionEditor, setShowQuestionEditor] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState(null);
  const [showTranscriptPanel, setShowTranscriptPanel] = useState(false);
  const [copiedTranscript, setCopiedTranscript] = useState(false);

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
  const metadata = generationResult?.metadata || null;
  const isWriting = formData.skill === 'WRITING';
  const reasoningText = generationResult?.reasoning || reasoning;

  // Extract figure/image description from content (for map/plan labeling)
  const figureDescription = content.figure_description ?? content.figureDescription;

  // Transform ABTS data to AdminPreviewContent format.
  // Backend raw shapes: reading {section:{passage_text}, questions}, listening
  // {transcript, section_layout, questions}, writing {task_prompt,...}, and
  // multi-part {sections:[{part,...}]}.
  const { previewSections, previewQuestions } = useMemo(() => {
    let rawSections = [];
    if (Array.isArray(content.sections)) {
      rawSections = content.sections;
    } else if (content.section) {
      rawSections = [content.section];
    } else {
      rawSections = [content];
    }

    const fallbackPart = formData.selectedParts?.[0] ?? formData.partNumber ?? 1;

    // Transform sections: ABTS format -> AdminPreviewContent format
    const transformedSections = rawSections.map((sec, idx) => {
      // Filter out blocks with invalid/missing question_numbers
      let filteredLayout = sec.section_layout ?? sec.sectionLayout ?? null;
      if (filteredLayout?.blocks && Array.isArray(filteredLayout.blocks)) {
        const validBlocks = filteredLayout.blocks.filter((block) => {
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
        filteredLayout = validBlocks.length > 0 ? { ...filteredLayout, blocks: validBlocks } : null;
      }

      const partNum = sec.part ?? sec.partNumber ?? (rawSections.length === 1 ? fallbackPart : idx + 1);

      return {
        id: `abts-section-${idx}`,
        partNumber: partNum,
        passageText: sec.passage_text ?? sec.passageText ?? sec.transcript ?? sec.task_prompt ?? '',
        sectionLayout: filteredLayout,
        wordCount: sec.wordCount ?? null,
        displayContentUrl: imageUrls[partNum] || null, // User-entered image URL
        audioUrl: null
      };
    });

    // Transform questions: ABTS format -> AdminPreviewContent format.
    // Multi-part content nests questions inside each section entry.
    const transformedQuestions = (content.questions ?? (
        Array.isArray(content.sections)
            ? content.sections.flatMap((section) => section.questions || [])
            : []
    )).map((q, idx) => {
      // Determine which section this question belongs to (by question number range)
      let sectionId = transformedSections[0]?.id;
      if (transformedSections.length > 1) {
        const qNum = q.question_number ?? q.questionNumber ?? idx + 1;
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
        questionNumber: q.question_number ?? q.questionNumber ?? idx + 1,
        questionType: q.question_type ?? q.questionType ?? 'UNKNOWN',
        questionContent: q.question_content ?? q.questionContent ?? {},
        correctAnswer: formatCorrectAnswer(q.correct_answer ?? q.correctAnswer),
        explanation: q.explanation || ''
      };
    });

    return { previewSections: transformedSections, previewQuestions: transformedQuestions };
  }, [content, formData.skill, formData.selectedParts, formData.partNumber, imageUrls]);

  // Get combined transcripts for all parts (MUST be before early returns)
  const allTranscripts = useMemo(() => {
    let rawSections = [];
    if (Array.isArray(content.sections)) {
      rawSections = content.sections;
    } else if (content.section) {
      rawSections = [content.section];
    } else {
      rawSections = [content];
    }
    const fallbackPart = formData.selectedParts?.[0] ?? formData.partNumber ?? 1;
    return rawSections.map((sec, idx) => ({
      partNumber: sec.part ?? sec.partNumber ?? (rawSections.length === 1 ? fallbackPart : idx + 1),
      transcript: sec.transcript || sec.passage_text || sec.passageText || ''
    })).filter(s => s.transcript);
  }, [content, formData.selectedParts, formData.partNumber]);

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

  // Check if skill is Listening (MUST be before early returns)
  const isListening = formData.skill === 'LISTENING';

  // Check if current part needs image - PLAN_MAP_DIAGRAM_LABELING (MUST be before early returns)
  const currentPartNeedsImage = useMemo(() => {
    const currentSection = previewSections[activePartIndex];
    if (!currentSection?.sectionLayout?.blocks) return false;
    return currentSection.sectionLayout.blocks.some(
      block => block.block_type === 'PLAN_MAP_DIAGRAM_LABELING'
    );
  }, [previewSections, activePartIndex]);

  // Get current part number for image URL state (MUST be before early returns)
  const currentPartNumber = previewSections[activePartIndex]?.partNumber || 1;

  // Handle image URL change (MUST be before early returns)
  const handleImageUrlChange = useCallback((url) => {
    setImageUrl(currentPartNumber, url);
  }, [currentPartNumber, setImageUrl]);

  // 1. Loading / Streaming State
  if (isGenerating) {
    return (
      <div className="studio-preview">
        <StreamingDisplay
          isActive={isGenerating}
          events={streamEvents}
          streamPreview={streamPreview}
          streamChunkCount={streamChunkCount}
          progress={generationProgress}
          error={generationError}
          partErrors={partErrors}
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
    const taskText = content.section?.task_prompt || content.section?.taskText || content.task_prompt || '';
    return (
      <div className="studio-preview">
        <MetadataBar />
        <RefinementModal />
        <ReasoningPanel />
        <div className="studio-panel__content" style={{ flex: 1, overflow: 'auto', padding: '16px' }}>
          <h3>Writing Task</h3>
          <div dangerouslySetInnerHTML={{ __html: taskText }} />
          {(content.chart_data || content.figure_description || content.letter_context || content.essay_metadata) && (
            <div className="studio-json-panel" style={{ marginTop: '16px' }}>
              <div className="studio-json-header">
                <span className="studio-json-header__title">Writing Details</span>
              </div>
              <div className="studio-json-content">
                {content.task_type && <pre>task_type: {content.task_type}</pre>}
                {content.word_requirement && <pre>word_requirement: {content.word_requirement}</pre>}
                {content.chart_data && <pre>{JSON.stringify(content.chart_data, null, 2)}</pre>}
                {content.figure_description && <pre>{JSON.stringify(content.figure_description, null, 2)}</pre>}
                {content.letter_context && <pre>{JSON.stringify(content.letter_context, null, 2)}</pre>}
                {content.essay_metadata && <pre>{JSON.stringify(content.essay_metadata, null, 2)}</pre>}
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
