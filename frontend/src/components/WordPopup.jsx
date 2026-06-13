import React, { useState, useEffect, useRef, useCallback } from 'react';
import { FiBook, FiLoader, FiX, FiVolume2 } from 'react-icons/fi';
import { motion, AnimatePresence } from 'framer-motion';
import { useVocabularyStore } from '../stores';
import { showSuccessToast, showErrorToast } from '../utils/toast';

const WordPopup = ({ 
  isOpen, 
  word, 
  context = null, 
  position = { x: 0, y: 0 },
  onClose,
  onSaveComplete,
}) => {
  const { translateWord, translating, addWord } = useVocabularyStore();
  
  const [translation, setTranslation] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  
  const popupRef = useRef(null);

  // Calculate popup position
  const [adjustedPosition, setAdjustedPosition] = useState({ x: 0, y: 0 });

  useEffect(() => {
    if (isOpen && popupRef.current) {
      const popup = popupRef.current;
      const rect = popup.getBoundingClientRect();
      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;
      
      let x = position.x;
      let y = position.y + 10; // 10px below the click

      // Prevent overflow on right
      if (x + rect.width > viewportWidth - 20) {
        x = viewportWidth - rect.width - 20;
      }
      
      // Prevent overflow on left
      if (x < 20) {
        x = 20;
      }
      
      // Prevent overflow on bottom - show above if needed
      if (y + rect.height > viewportHeight - 20) {
        y = position.y - rect.height - 10;
      }

      setAdjustedPosition({ x, y });
    }
  }, [isOpen, position]);

  // Reset state when word changes
  useEffect(() => {
    if (isOpen && word) {
      setTranslation(null);
      setError(null);
    }
  }, [isOpen, word]);

  // Close on escape key
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  // Close on click outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (popupRef.current && !popupRef.current.contains(e.target)) {
        onClose();
      }
    };
    if (isOpen) {
      // Delay to prevent immediate close on the click that opened it
      setTimeout(() => {
        document.addEventListener('mousedown', handleClickOutside);
      }, 100);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen, onClose]);

  const handleTranslate = useCallback(async () => {
    if (!word) return;
    
    setError(null);
    try {
      const result = await translateWord(word, context);
      setTranslation(result);
    } catch (err) {
      setError('Không thể dịch từ này');
    }
  }, [word, context, translateWord]);

  const handleSpeak = () => {
    if ('speechSynthesis' in window && word) {
      const utterance = new SpeechSynthesisUtterance(word);
      utterance.lang = 'en-US';
      utterance.rate = 0.9;
      window.speechSynthesis.speak(utterance);
    }
  };

  const handleSaveToNotebook = async () => {
    if (!word) return;
    
    setSaving(true);
    setError(null);
    
    try {
      const wordData = {
        word: word,
        translation: translation?.translation || '',
        phonetic: translation?.phonetic || '',
        partOfSpeech: translation?.partOfSpeech || '',
        definition: translation?.definition || '',
        exampleSentence: translation?.exampleSentence || '',
        sourceContext: context || '',
      };
      
      await addWord(wordData);
      showSuccessToast(`Đã thêm "${word}" vào sổ tay`);
      onClose();
      onSaveComplete?.();
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Không thể lưu từ này';
      setError(errorMsg);
      showErrorToast(errorMsg);
    } finally {
      setSaving(false);
    }
  };

  // Auto-translate when popup opens
  useEffect(() => {
    if (isOpen && word && !translation && !translating) {
      handleTranslate();
    }
  }, [isOpen, word, translation, translating, handleTranslate]);

  return (
    <AnimatePresence>
      {isOpen && word && (
        <motion.div
          ref={popupRef}
          className="word-popup"
          initial={{ opacity: 0, scale: 0.9, y: -10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.9, y: -10 }}
          style={{
            left: adjustedPosition.x,
            top: adjustedPosition.y,
          }}
        >
          {/* Close button */}
          <button
            className="word-popup__close"
            onClick={onClose}
            aria-label="Đóng"
          >
            <FiX />
          </button>

          {/* Word header */}
          <div className="word-popup__header">
            <span className="word-popup__word">{word}</span>
            <button
              className="word-popup__speak"
              onClick={handleSpeak}
              title="Phát âm"
              aria-label="Phát âm"
            >
              <FiVolume2 />
            </button>
          </div>

          {/* Content */}
          <div className="word-popup__content">
            {translating ? (
              <div className="word-popup__loading">
                <FiLoader className="word-popup__spin" />
                <span>Đang dịch...</span>
              </div>
            ) : error ? (
              <div className="word-popup__error">
                {error}
              </div>
            ) : translation ? (
              <div className="word-popup__translation">
                {translation.phonetic && (
                  <div className="word-popup__phonetic">
                    /{translation.phonetic}/
                  </div>
                )}
                {translation.partOfSpeech && (
                  <span className="word-popup__pos">
                    {translation.partOfSpeech}
                  </span>
                )}
                <div className="word-popup__meaning">
                  {translation.translation}
                </div>
                {translation.definition && (
                  <div className="word-popup__definition">
                    {translation.definition}
                  </div>
                )}
              </div>
            ) : (
              <div className="word-popup__empty">
                Click để dịch
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="word-popup__actions">
            <button
              className="word-popup__save-btn"
              onClick={handleSaveToNotebook}
              disabled={saving || translating}
            >
              {saving ? (
                <>
                  <FiLoader className="word-popup__spin" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <>
                  <FiBook />
                  <span>Lưu vào sổ tay</span>
                </>
              )}
            </button>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default WordPopup;
