import React from 'react';
import { FiStar, FiEdit3, FiTrash2, FiVolume2, FiBookOpen } from 'react-icons/fi';
import { motion } from 'framer-motion';

const VocabularyCard = ({ 
  vocabulary, 
  onEdit, 
  onDelete, 
  onToggleMastered,
  isDeleting = false 
}) => {
  const {
    id,
    word,
    translation,
    phonetic,
    partOfSpeech,
    definition,
    exampleSentence,
    mastered,
    sourceContext,
  } = vocabulary;

  const handleSpeak = () => {
    if ('speechSynthesis' in window) {
      const utterance = new SpeechSynthesisUtterance(word);
      utterance.lang = 'en-US';
      utterance.rate = 0.9;
      window.speechSynthesis.speak(utterance);
    }
  };

  const partOfSpeechMap = {
    noun: { label: 'n.', color: 'var(--vocab-noun)' },
    verb: { label: 'v.', color: 'var(--vocab-verb)' },
    adjective: { label: 'adj.', color: 'var(--vocab-adj)' },
    adverb: { label: 'adv.', color: 'var(--vocab-adv)' },
    preposition: { label: 'prep.', color: 'var(--vocab-prep)' },
    conjunction: { label: 'conj.', color: 'var(--vocab-conj)' },
    pronoun: { label: 'pron.', color: 'var(--vocab-pron)' },
    interjection: { label: 'interj.', color: 'var(--vocab-interj)' },
    phrase: { label: 'phr.', color: 'var(--vocab-phrase)' },
  };

  const posInfo = partOfSpeech ? partOfSpeechMap[partOfSpeech.toLowerCase()] : null;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.9 }}
      className={`vocabulary-card ${mastered ? 'vocabulary-card--mastered' : ''} ${isDeleting ? 'vocabulary-card--deleting' : ''}`}
    >
      {/* Header with word and actions */}
      <div className="vocabulary-card__header">
        <div className="vocabulary-card__word-section">
          <h3 className="vocabulary-card__word">{word}</h3>
          <button 
            className="vocabulary-card__speak-btn"
            onClick={handleSpeak}
            title="Phát âm"
            aria-label="Phát âm từ"
          >
            <FiVolume2 />
          </button>
        </div>
        
        <button
          className={`vocabulary-card__star-btn ${mastered ? 'vocabulary-card__star-btn--active' : ''}`}
          onClick={() => onToggleMastered(id)}
          title={mastered ? 'Bỏ đánh dấu đã thuộc' : 'Đánh dấu đã thuộc'}
          aria-label={mastered ? 'Bỏ đánh dấu đã thuộc' : 'Đánh dấu đã thuộc'}
        >
          <FiStar />
        </button>
      </div>

      {/* Phonetic and Part of Speech */}
      <div className="vocabulary-card__meta">
        {phonetic && (
          <span className="vocabulary-card__phonetic">/{phonetic}/</span>
        )}
        {posInfo && (
          <span 
            className="vocabulary-card__pos"
            style={{ '--pos-color': posInfo.color }}
          >
            {posInfo.label}
          </span>
        )}
      </div>

      {/* Translation */}
      {translation && (
        <div className="vocabulary-card__translation">
          <span className="vocabulary-card__label">Nghĩa:</span>
          <span className="vocabulary-card__value">{translation}</span>
        </div>
      )}

      {/* Definition */}
      {definition && (
        <div className="vocabulary-card__definition">
          <span className="vocabulary-card__label">Định nghĩa:</span>
          <p className="vocabulary-card__value">{definition}</p>
        </div>
      )}

      {/* Example Sentence */}
      {exampleSentence && (
        <div className="vocabulary-card__example">
          <span className="vocabulary-card__label">Ví dụ:</span>
          <p className="vocabulary-card__value vocabulary-card__value--italic">
            "{exampleSentence}"
          </p>
        </div>
      )}

      {/* Source Context */}
      {sourceContext && (
        <div className="vocabulary-card__source">
          <FiBookOpen className="vocabulary-card__source-icon" />
          <span className="vocabulary-card__source-text">{sourceContext}</span>
        </div>
      )}

      {/* Actions */}
      <div className="vocabulary-card__actions">
        <button
          className="vocabulary-card__action-btn vocabulary-card__action-btn--edit"
          onClick={() => onEdit(vocabulary)}
          title="Chỉnh sửa"
          aria-label="Chỉnh sửa từ vựng"
        >
          <FiEdit3 />
          <span>Sửa</span>
        </button>
        <button
          className="vocabulary-card__action-btn vocabulary-card__action-btn--delete"
          onClick={() => onDelete(id)}
          disabled={isDeleting}
          title="Xóa"
          aria-label="Xóa từ vựng"
        >
          <FiTrash2 />
          <span>Xóa</span>
        </button>
      </div>
    </motion.div>
  );
};

export default VocabularyCard;
