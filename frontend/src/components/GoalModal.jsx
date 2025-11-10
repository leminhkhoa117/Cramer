import React, { useState, useEffect } from 'react';
import { targetApi } from '../api/backendApi';
import '../css/GoalModal.css';

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
            await targetApi.saveMyTarget(payload);
            onSave();
        } catch (err) {
            console.error('Failed to save target:', err);
            setError('Không thể lưu mục tiêu. Vui lòng thử lại.');
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) {
        return null;
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <div className="modal-panel">
                    <div className="modal-header">
                        <h2>Chỉnh sửa mục tiêu</h2>
                        <button
                            type="button"
                            className="modal-close"
                            aria-label="Đóng cửa sổ"
                            onClick={onClose}
                        >
                            ×
                        </button>
                    </div>

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label htmlFor="examName">Kỳ thi</label>
                            <input
                                type="text"
                                id="examName"
                                name="examName"
                                value={targetData.examName}
                                onChange={handleChange}
                                placeholder="IELTS"
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="examDate">Ngày thi</label>
                            <input
                                type="date"
                                id="examDate"
                                name="examDate"
                                value={targetData.examDate}
                                onChange={handleChange}
                            />
                        </div>
                        <div className="scores-grid">
                            <div className="form-group">
                                <label htmlFor="listening">Listening</label>
                                <input
                                    type="number"
                                    id="listening"
                                    name="listening"
                                    min="0"
                                    max="9"
                                    step="0.5"
                                    value={targetData.listening}
                                    onChange={handleChange}
                                    placeholder="0-9"
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="reading">Reading</label>
                                <input
                                    type="number"
                                    id="reading"
                                    name="reading"
                                    min="0"
                                    max="9"
                                    step="0.5"
                                    value={targetData.reading}
                                    onChange={handleChange}
                                    placeholder="0-9"
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="writing">Writing</label>
                                <input
                                    type="number"
                                    id="writing"
                                    name="writing"
                                    min="0"
                                    max="9"
                                    step="0.5"
                                    value={targetData.writing}
                                    onChange={handleChange}
                                    placeholder="0-9"
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="speaking">Speaking</label>
                                <input
                                    type="number"
                                    id="speaking"
                                    name="speaking"
                                    min="0"
                                    max="9"
                                    step="0.5"
                                    value={targetData.speaking}
                                    onChange={handleChange}
                                    placeholder="0-9"
                                />
                            </div>
                        </div>

                        {error && <p className="modal-error">{error}</p>}

                        <div className="modal-actions">
                            <button type="button" className="btn btn-secondary" onClick={onClose} disabled={loading}>
                                Hủy
                            </button>
                            <button type="submit" className="btn btn-primary" disabled={loading}>
                                {loading ? 'Đang lưu...' : 'Lưu thay đổi'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default GoalModal;
