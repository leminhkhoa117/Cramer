import React, { useState, useEffect, useRef } from 'react';
import { FiX, FiSave, FiRefreshCw, FiLoader } from 'react-icons/fi';
import { motion, AnimatePresence } from 'framer-motion';
import { useVocabularyStore } from '../stores';

const VocabularyModal = ({ isOpen, onClose, vocabulary = null, onSave }) => {
  const { translateWord, translating, translationError } = useVocabularyStore();
  
  const [formData, setFormData] = useState({
    word: '',
    translation: '',
    phonetic: '',
    partOfSpeech: '',
    definition: '',
    exampleSentence: '',
    notes: '',
    sourceContext: '',
  });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  
  const wordInputRef = useRef(null);
  const isEditing = !!vocabulary;

  // Initialize form data when editing
  useEffect(() => {
    if (vocabulary) {
      setFormData({
        word: vocabulary.word || '',
        translation: vocabulary.translation || '',
        phonetic: vocabulary.phonetic || '',
        partOfSpeech: vocabulary.partOfSpeech || '',
        definition: vocabulary.definition || '',
        exampleSentence: vocabulary.exampleSentence || '',
        notes: vocabulary.notes || '',
        sourceContext: vocabulary.sourceContext || '',
      });
    } else {
      // Reset form for new entry
      setFormData({
        word: '',
        translation: '',
        phonetic: '',
        partOfSpeech: '',
        definition: '',
        exampleSentence: '',
        notes: '',
        sourceContext: '',
      });
    }
    setErrors({});
  }, [vocabulary, isOpen]);

  // Focus word input when modal opens
  useEffect(() => {
    if (isOpen && wordInputRef.current) {
      setTimeout(() => wordInputRef.current?.focus(), 100);
    }
  }, [isOpen]);

  // Handle escape key
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: null }));
    }
  };

  const handleTranslate = async () => {
    if (!formData.word.trim()) {
      setErrors(prev => ({ ...prev, word: 'Vui lòng nhập từ cần dịch' }));
      return;
    }

    try {
      const result = await translateWord(formData.word, formData.sourceContext || null);
      
      // Auto-fill translation result
      setFormData(prev => ({
        ...prev,
        translation: result.translation || prev.translation,
        phonetic: result.phonetic || prev.phonetic,
        partOfSpeech: result.partOfSpeech || prev.partOfSpeech,
        definition: result.definition || prev.definition,
        exampleSentence: result.exampleSentence || prev.exampleSentence,
      }));
    } catch (err) {
      console.error('Translation failed:', err);
    }
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.word.trim()) {
      newErrors.word = 'Từ vựng không được để trống';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validate()) return;

    setSaving(true);
    try {
      await onSave(formData);
      onClose();
    } catch (err) {
      console.error('Save failed:', err);
      setErrors(prev => ({ ...prev, submit: err.message }));
    } finally {
      setSaving(false);
    }
  };

  const partOfSpeechOptions = [
    { value: '', label: 'Chọn loại từ' },
    { value: 'noun', label: 'Danh từ (Noun)' },
    { value: 'verb', label: 'Động từ (Verb)' },
    { value: 'adjective', label: 'Tính từ (Adjective)' },
    { value: 'adverb', label: 'Trạng từ (Adverb)' },
    { value: 'preposition', label: 'Giới từ (Preposition)' },
    { value: 'conjunction', label: 'Liên từ (Conjunction)' },
    { value: 'pronoun', label: 'Đại từ (Pronoun)' },
    { value: 'interjection', label: 'Thán từ (Interjection)' },
    { value: 'phrase', label: 'Cụm từ (Phrase)' },
  ];

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="vocabulary-modal-overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.div
            className="vocabulary-modal"
            initial={{ opacity: 0, scale: 0.9, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 20 }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="vocabulary-modal__header">
              <h2 className="vocabulary-modal__title">
                {isEditing ? 'Chỉnh sửa từ vựng' : 'Thêm từ mới'}
              </h2>
              <button
                className="vocabulary-modal__close-btn"
                onClick={onClose}
                aria-label="Đóng"
              >
                <FiX />
              </button>
            </div>

            {/* Form */}
            <form className="vocabulary-modal__form" onSubmit={handleSubmit}>
              {/* Word Field */}
              <div className="vocabulary-modal__field">
                <label htmlFor="word" className="vocabulary-modal__label">
                  Từ vựng <span className="vocabulary-modal__required">*</span>
                </label>
                <div className="vocabulary-modal__input-group">
                  <input
                    ref={wordInputRef}
                    type="text"
                    id="word"
                    name="word"
                    value={formData.word}
                    onChange={handleChange}
                    className={`vocabulary-modal__input ${errors.word ? 'vocabulary-modal__input--error' : ''}`}
                    placeholder="Nhập từ tiếng Anh..."
                  />
                  <button
                    type="button"
                    className="vocabulary-modal__translate-btn"
                    onClick={handleTranslate}
                    disabled={translating || !formData.word.trim()}
                    title="Dịch tự động"
                  >
                    {translating ? (
                      <FiLoader className="vocabulary-modal__spin" />
                    ) : (
                      <FiRefreshCw />
                    )}
                    <span>Dịch tự động</span>
                  </button>
                </div>
                {errors.word && (
                  <span className="vocabulary-modal__error">{errors.word}</span>
                )}
                {translationError && (
                  <span className="vocabulary-modal__error">{translationError}</span>
                )}
              </div>

              {/* Translation Field */}
              <div className="vocabulary-modal__field">
                <label htmlFor="translation" className="vocabulary-modal__label">
                  Nghĩa tiếng Việt
                </label>
                <input
                  type="text"
                  id="translation"
                  name="translation"
                  value={formData.translation}
                  onChange={handleChange}
                  className="vocabulary-modal__input"
                  placeholder="Nhập nghĩa tiếng Việt..."
                />
              </div>

              {/* Phonetic and Part of Speech Row */}
              <div className="vocabulary-modal__row">
                <div className="vocabulary-modal__field vocabulary-modal__field--half">
                  <label htmlFor="phonetic" className="vocabulary-modal__label">
                    Phiên âm (IPA)
                  </label>
                  <input
                    type="text"
                    id="phonetic"
                    name="phonetic"
                    value={formData.phonetic}
                    onChange={handleChange}
                    className="vocabulary-modal__input"
                    placeholder="ˈeksəmpəl"
                  />
                </div>

                <div className="vocabulary-modal__field vocabulary-modal__field--half">
                  <label htmlFor="partOfSpeech" className="vocabulary-modal__label">
                    Loại từ
                  </label>
                  <select
                    id="partOfSpeech"
                    name="partOfSpeech"
                    value={formData.partOfSpeech}
                    onChange={handleChange}
                    className="vocabulary-modal__select"
                  >
                    {partOfSpeechOptions.map(opt => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Definition Field */}
              <div className="vocabulary-modal__field">
                <label htmlFor="definition" className="vocabulary-modal__label">
                  Định nghĩa (tiếng Anh)
                </label>
                <textarea
                  id="definition"
                  name="definition"
                  value={formData.definition}
                  onChange={handleChange}
                  className="vocabulary-modal__textarea"
                  rows={2}
                  placeholder="English definition..."
                />
              </div>

              {/* Example Sentence Field */}
              <div className="vocabulary-modal__field">
                <label htmlFor="exampleSentence" className="vocabulary-modal__label">
                  Câu ví dụ
                </label>
                <textarea
                  id="exampleSentence"
                  name="exampleSentence"
                  value={formData.exampleSentence}
                  onChange={handleChange}
                  className="vocabulary-modal__textarea"
                  rows={2}
                  placeholder="An example sentence using this word..."
                />
              </div>

              {/* Notes Field */}
              <div className="vocabulary-modal__field">
                <label htmlFor="notes" className="vocabulary-modal__label">
                  Ghi chú
                </label>
                <textarea
                  id="notes"
                  name="notes"
                  value={formData.notes}
                  onChange={handleChange}
                  className="vocabulary-modal__textarea"
                  rows={2}
                  placeholder="Ghi chú thêm về từ này..."
                />
              </div>

              {/* Submit Error */}
              {errors.submit && (
                <div className="vocabulary-modal__submit-error">
                  {errors.submit}
                </div>
              )}

              {/* Actions */}
              <div className="vocabulary-modal__actions">
                <button
                  type="button"
                  className="vocabulary-modal__btn vocabulary-modal__btn--cancel"
                  onClick={onClose}
                  disabled={saving}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="vocabulary-modal__btn vocabulary-modal__btn--save"
                  disabled={saving}
                >
                  {saving ? (
                    <>
                      <FiLoader className="vocabulary-modal__spin" />
                      <span>Đang lưu...</span>
                    </>
                  ) : (
                    <>
                      <FiSave />
                      <span>{isEditing ? 'Cập nhật' : 'Thêm từ'}</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default VocabularyModal;
