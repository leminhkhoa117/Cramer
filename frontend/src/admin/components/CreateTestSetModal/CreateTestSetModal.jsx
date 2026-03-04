import React, { useState, useEffect } from 'react';
import { FiX, FiFolder, FiType, FiTag, FiHash, FiClock } from 'react-icons/fi';
import { useToast } from '../Toast';
import useAdminContentStore from '../../stores/useAdminContentStore';
import TagInput from '../abts/TagInput'; // Reusing TagInput component
import '../../css/admin-variables.css';
import '../common/AdminModal.css';


/**
 * Modal to Create or Edit a Test Set (Collection of Tests)
 */
export default function CreateTestSetModal({ isOpen, onClose, testSet = null }) {
    const toast = useToast();
    const { createTestSet, updateTestSet, isLoadingSet } = useAdminContentStore();

    const [formData, setFormData] = useState({
        code: '',
        name: '',
        description: '',
        sourceType: 'CAMBRIDGE', // Default
        displayOrder: 0,
        hashtagIds: []
    });

    // Populate for editing
    useEffect(() => {
        if (testSet) {
            setFormData({
                code: testSet.code || '',
                name: testSet.name || '',
                description: testSet.description || '',
                sourceType: testSet.sourceType || 'CAMBRIDGE',
                displayOrder: testSet.displayOrder || 0,
                // If the testSet object has hashtags populated, map them to IDs
                hashtagIds: testSet.hashtags ? testSet.hashtags.map(h => h.id) : []
            });
        } else {
            setFormData({
                code: '',
                name: '',
                description: '',
                sourceType: 'CAMBRIDGE',
                displayOrder: 0,
                hashtagIds: []
            });
        }
    }, [testSet, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.code || !formData.name) {
            toast.error("Vui lòng nhập Mã bộ đề và Tên tiếng Việt");
            return;
        }

        try {
            if (testSet) {
                await updateTestSet(testSet.id, formData);
                toast.success("Cập nhật bộ đề thành công!");
            } else {
                await createTestSet(formData);
                toast.success("Đã tạo bộ đề mới!");
            }
            onClose();
        } catch (err) {
            toast.error(err.response?.data?.error || "Có lỗi xảy ra");
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content create-set-modal">
                <div className="modal-header">
                    <h3 className="modal-title">
                        <FiFolder className="title-icon" />
                        {testSet ? 'Chỉnh sửa Bộ đề' : 'Tạo Bộ đề mới'}
                    </h3>
                    <button className="modal-close" onClick={onClose}>
                        <FiX size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
                    <div className="modal-body">
                        <div className="form-grid">
                            <div className="form-group">
                                <label><FiHash /> Mã bộ đề (VD: cam18)</label>
                                <input
                                    className="form-input"
                                    type="text"
                                    value={formData.code}
                                    onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                                    placeholder="Nhập mã code duy nhất"
                                    disabled={!!testSet}
                                />
                            </div>

                            <div className="form-group">
                                <label><FiType /> Nguồn</label>
                                <select
                                    className="form-select"
                                    value={formData.sourceType}
                                    onChange={(e) => setFormData({ ...formData, sourceType: e.target.value })}
                                >
                                    <option value="CAMBRIDGE">Cambridge</option>
                                    <option value="IDP">IDP</option>
                                    <option value="BC">British Council</option>
                                    <option value="OTHER">Khác</option>
                                </select>
                            </div>
                        </div>

                        <div className="form-group">
                            <label><FiTag /> Tên Bộ đề (Tiếng Việt)</label>
                            <input
                                className="form-input"
                                type="text"
                                value={formData.name}
                                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                placeholder="VD: Cambridge IELTS 18"
                            />
                        </div>

                        <div className="form-group">
                            <label>Tên Bộ đề (Tiếng Anh)</label>
                            <input
                                className="form-input"
                                type="text"
                                value={formData.name}
                                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                placeholder="VD: Cambridge IELTS 18 Academic"
                            />
                        </div>

                        <div className="form-group">
                            <label>Mô tả ngắn</label>
                            <textarea
                                className="form-textarea"
                                value={formData.description}
                                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                rows={3}
                            />
                        </div>

                        <div className="form-group">
                            <label>Hashtags</label>
                            <TagInput
                                value={formData.hashtagIds}
                                onChange={(ids) => setFormData({ ...formData, hashtagIds: ids })}
                                mode="select"
                            />
                        </div>

                        <div className="form-group">
                            <label><FiClock /> Thứ tự hiển thị</label>
                            <input
                                className="form-input"
                                type="number"
                                value={formData.displayOrder}
                                onChange={(e) => setFormData({ ...formData, displayOrder: parseInt(e.target.value) })}
                            />
                        </div>
                    </div>

                    <div className="modal-footer">
                        <button type="button" className="modal-btn modal-btn-secondary" onClick={onClose}>
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="modal-btn modal-btn-primary"
                            disabled={isLoadingSet}
                        >
                            {isLoadingSet ? 'Đang lưu...' : (testSet ? 'Cập nhật' : 'Tạo bộ đề')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
