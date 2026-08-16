import { useState, useEffect } from 'react';
import { dashboardApi } from '../api/backendApi.js';
import BaseModal from './common/BaseModal';

/**
 * GoalModal - Edit target band scores
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onClose - Close handler
 * @param {object} currentTarget - Current target data
 * @param {function} onSave - Save success callback
 */
const GoalModal = ({ isOpen, onClose, currentTarget, onSave }) => {
  const [targetData, setTargetData] = useState({
    examName: 'IELTS',
    examDate: '',
    listening: '',
    reading: '',
    writing: '',
    speaking: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (currentTarget) {
      setTargetData({
        examName: currentTarget.examName || 'IELTS',
        examDate: currentTarget.examDate || '',
        listening: currentTarget.listening || '',
        reading: currentTarget.reading || '',
        writing: currentTarget.writing || '',
        speaking: currentTarget.speaking || '',
      });
    } else {
      setTargetData({
        examName: 'IELTS',
        examDate: '',
        listening: '',
        reading: '',
        writing: '',
        speaking: '',
      });
    }
  }, [currentTarget]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setTargetData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const payload = {
      examName: targetData.examName,
      examDate: targetData.examDate === '' ? null : targetData.examDate,
      listening: targetData.listening === '' ? null : parseFloat(targetData.listening),
      reading: targetData.reading === '' ? null : parseFloat(targetData.reading),
      writing: targetData.writing === '' ? null : parseFloat(targetData.writing),
      speaking: targetData.speaking === '' ? null : parseFloat(targetData.speaking),
    };

    try {
      const savedTarget = await dashboardApi.saveTarget(payload);
      onSave(savedTarget.data);
    } catch (err) {
      console.error('Failed to save target:', err);
      setError('Không thể lưu mục tiêu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={onClose}
      title="Chỉnh sửa mục tiêu"
      showCloseButton={true}
      footer={
        <>
          <button 
            type="button" 
            className="cm-btn cm-btn--secondary" 
            onClick={onClose} 
            disabled={loading}
          >
            Hủy
          </button>
          <button 
            type="submit" 
            form="goal-form"
            className="cm-btn cm-btn--primary" 
            disabled={loading}
          >
            {loading ? (
              <span className="cm-loading">Đang lưu...</span>
            ) : 'Lưu thay đổi'}
          </button>
        </>
      }
    >
      <form id="goal-form" onSubmit={handleSubmit} className="cm-form">
        <div className="cm-form-group">
          <label htmlFor="examName" className="cm-form-label">Kỳ thi</label>
          <input
            type="text"
            id="examName"
            name="examName"
            className="cm-form-input"
            value={targetData.examName}
            onChange={handleChange}
            placeholder="IELTS"
          />
        </div>
        
        <div className="cm-form-group">
          <label htmlFor="examDate" className="cm-form-label">Ngày thi</label>
          <input
            type="date"
            id="examDate"
            name="examDate"
            className="cm-form-input"
            value={targetData.examDate}
            onChange={handleChange}
          />
        </div>
        
        <div className="cm-form-grid">
          <div className="cm-form-group">
            <label htmlFor="listening" className="cm-form-label">Listening</label>
            <input
              type="number"
              id="listening"
              name="listening"
              className="cm-form-input"
              min="0"
              max="9"
              step="0.5"
              value={targetData.listening}
              onChange={handleChange}
              placeholder="0-9"
            />
          </div>
          <div className="cm-form-group">
            <label htmlFor="reading" className="cm-form-label">Reading</label>
            <input
              type="number"
              id="reading"
              name="reading"
              className="cm-form-input"
              min="0"
              max="9"
              step="0.5"
              value={targetData.reading}
              onChange={handleChange}
              placeholder="0-9"
            />
          </div>
          <div className="cm-form-group">
            <label htmlFor="writing" className="cm-form-label">Writing</label>
            <input
              type="number"
              id="writing"
              name="writing"
              className="cm-form-input"
              min="0"
              max="9"
              step="0.5"
              value={targetData.writing}
              onChange={handleChange}
              placeholder="0-9"
            />
          </div>
          <div className="cm-form-group">
            <label htmlFor="speaking" className="cm-form-label">Speaking</label>
            <input
              type="number"
              id="speaking"
              name="speaking"
              className="cm-form-input"
              min="0"
              max="9"
              step="0.5"
              value={targetData.speaking}
              onChange={handleChange}
              placeholder="0-9"
            />
          </div>
        </div>

        {error && <p className="cm-form-error">{error}</p>}
      </form>
    </BaseModal>
  );
};

export default GoalModal;
