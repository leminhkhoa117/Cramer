import { useRef, useState } from 'react';
import { FiPlus, FiX } from 'react-icons/fi';
import useTestSetStore from '../../../stores/useTestSetStore';

export default function EditSetModal({ testSet, onClose }) {
    const { updateTestSet, selectedSetTests, updateTest } = useTestSetStore();
    const [formData, setFormData] = useState({
        code: testSet.code || '',
        name: testSet.name || '',
        description: testSet.description || '',
        sourceType: testSet.sourceType || 'custom',
        coverImageUrl: testSet.coverImageUrl || ''
    });
    const [batchDifficulty, setBatchDifficulty] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadError, setUploadError] = useState(null);
    const fileInputRef = useRef(null);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    // Handle file selection and upload
    const handleFileSelect = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // Validate file type
        if (!file.type.startsWith('image/')) {
            setUploadError('Vui lòng chọn file ảnh (jpg, png, webp)');
            return;
        }

        // Validate file size (max 5MB)
        if (file.size > 5 * 1024 * 1024) {
            setUploadError('Kích thước file tối đa là 5MB');
            return;
        }

        setUploadError(null);
        setIsUploading(true);

        try {
            // Dynamic import to avoid circular dependencies
            const { supabase } = await import('../../../../api/supabaseClient');

            // Generate unique filename
            const fileExt = file.name.split('.').pop();
            const fileName = `cover_${Date.now()}.${fileExt}`;
            const filePath = `SETS/${testSet.id}/${fileName}`;

            // Upload to THUMBNAILS bucket
            const { error: uploadError } = await supabase.storage
                .from('THUMBNAILS')
                .upload(filePath, file, {
                    cacheControl: '3600',
                    upsert: true
                });

            if (uploadError) {
                console.error('Upload error:', uploadError);
                setUploadError(`Lỗi upload: ${uploadError.message}`);
                setIsUploading(false);
                return;
            }

            // Get public URL
            const { data } = supabase.storage
                .from('THUMBNAILS')
                .getPublicUrl(filePath);

            if (data?.publicUrl) {
                setFormData(prev => ({ ...prev, coverImageUrl: data.publicUrl }));
            }
        } catch (err) {
            console.error('Upload failed:', err);
            setUploadError('Không thể upload ảnh. Vui lòng thử lại.');
        } finally {
            setIsUploading(false);
        }
    };

    // Clear cover image
    const handleClearImage = () => {
        setFormData(prev => ({ ...prev, coverImageUrl: '' }));
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        try {
            // Update test set metadata
            await updateTestSet(testSet.id, formData);

            // Apply batch difficulty if selected
            if (batchDifficulty) {
                const updatePromises = selectedSetTests.map(test =>
                    updateTest(test.id, {
                        testNumber: test.testNumber,
                        difficulty: batchDifficulty
                    })
                );
                await Promise.all(updatePromises);
            }

            onClose();
        } catch (err) {
            console.error('Error updating test set:', err);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="admin-modal-overlay-custom" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="admin-edit-modal admin-edit-modal--wide" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header">
                    <h2>Chỉnh sửa bộ đề</h2>
                    <button className="admin-edit-modal-close" onClick={onClose} disabled={isSubmitting}>
                        <FiX size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="admin-edit-modal-body">
                        {/* Cover Image Section */}
                        <div className="form-group">
                            <label>Ảnh bìa (382x180 px)</label>
                            <div className="cover-image-upload">
                                {/* Preview */}
                                <div className="cover-image-preview">
                                    {formData.coverImageUrl ? (
                                        <img
                                            src={formData.coverImageUrl}
                                            alt="Cover preview"
                                            onError={(e) => {
                                                e.target.style.display = 'none';
                                                e.target.nextSibling.style.display = 'flex';
                                            }}
                                        />
                                    ) : null}
                                    <div
                                        className="cover-image-placeholder"
                                        style={{ display: formData.coverImageUrl ? 'none' : 'flex' }}
                                    >
                                        <FiPlus size={24} />
                                        <span>382 x 180</span>
                                    </div>
                                </div>

                                {/* Upload Controls */}
                                <div className="cover-image-controls">
                                    <input
                                        ref={fileInputRef}
                                        type="file"
                                        accept="image/*"
                                        onChange={handleFileSelect}
                                        disabled={isUploading || isSubmitting}
                                        style={{ display: 'none' }}
                                        id="cover-image-input"
                                    />
                                    <button
                                        type="button"
                                        className="admin-btn admin-btn--secondary admin-btn--sm"
                                        onClick={() => fileInputRef.current?.click()}
                                        disabled={isUploading || isSubmitting}
                                    >
                                        {isUploading ? 'Đang tải...' : 'Tải ảnh lên'}
                                    </button>
                                    {formData.coverImageUrl && (
                                        <button
                                            type="button"
                                            className="admin-btn admin-btn--danger admin-btn--sm"
                                            onClick={handleClearImage}
                                            disabled={isUploading || isSubmitting}
                                        >
                                            Xóa ảnh
                                        </button>
                                    )}
                                </div>

                                {/* URL Input */}
                                <div className="cover-image-url">
                                    <input
                                        type="url"
                                        name="coverImageUrl"
                                        className="form-input form-input--sm"
                                        placeholder="Hoặc nhập URL ảnh..."
                                        value={formData.coverImageUrl}
                                        onChange={handleChange}
                                        disabled={isUploading || isSubmitting}
                                    />
                                </div>

                                {/* Error */}
                                {uploadError && (
                                    <span className="form-error">{uploadError}</span>
                                )}
                            </div>
                        </div>

                        <div className="form-group">
                            <label htmlFor="edit-name">Tên bộ đề *</label>
                            <input
                                type="text"
                                id="edit-name"
                                name="name"
                                className="form-input"
                                value={formData.name}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="edit-description">Mô tả</label>
                            <textarea
                                id="edit-description"
                                name="description"
                                className="form-textarea"
                                value={formData.description}
                                onChange={handleChange}
                                disabled={isSubmitting}
                                rows={3}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="edit-sourceType">Loại nguồn</label>
                            <select
                                id="edit-sourceType"
                                name="sourceType"
                                className="form-select"
                                value={formData.sourceType}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            >
                                <option value="custom">Tùy chỉnh</option>
                                <option value="cambridge">Cambridge</option>
                                <option value="ai_generated">AI tạo</option>
                            </select>
                        </div>

                        {selectedSetTests.length > 0 && (
                            <div className="form-group" style={{ marginTop: '16px', paddingTop: '16px', borderTop: '1px solid var(--admin-border-primary)' }}>
                                <label htmlFor="edit-batchDifficulty">Đổi độ khó tất cả bài thi</label>
                                <select
                                    id="edit-batchDifficulty"
                                    className="form-select"
                                    value={batchDifficulty}
                                    onChange={(e) => setBatchDifficulty(e.target.value)}
                                    disabled={isSubmitting}
                                >
                                    <option value="">-- Không thay đổi --</option>
                                    <option value="BEGINNER">Beginner (Cơ bản)</option>
                                    <option value="INTERMEDIATE">Intermediate (Trung bình)</option>
                                    <option value="ADVANCED">Advanced (Nâng cao)</option>
                                </select>
                                <p style={{ fontSize: '0.75rem', color: 'var(--admin-text-muted)', marginTop: '4px' }}>
                                    Áp dụng cho tất cả {selectedSetTests.length} bài thi
                                </p>
                            </div>
                        )}
                    </div>


                    <div className="admin-edit-modal-footer">
                        <button
                            type="button"
                            className="admin-btn admin-btn--secondary"
                            onClick={onClose}
                            disabled={isSubmitting}
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="admin-btn admin-btn--primary"
                            disabled={isSubmitting || isUploading}
                        >
                            {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}