/**
 * AudioUploadModal - Modal for uploading audio files.
 * Supports drag-and-drop and file picker for MP3/WAV files.
 * 
 * @since 2025-12-21 - Content Upload Feature
 */

import React, { useState, useRef, useCallback, useEffect } from 'react';
import { FiUpload, FiMusic, FiX, FiCheck, FiAlertTriangle, FiTrash2 } from 'react-icons/fi';
import '../../css/common/modal.css';

export default function AudioUploadModal({
    isOpen,
    onClose,
    onSave,
    initialAudioUrl = '',
    title = 'Upload Audio',
    maxSizeMB = 50
}) {
    const [audioFile, setAudioFile] = useState(null);
    const [audioUrl, setAudioUrl] = useState(initialAudioUrl);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [error, setError] = useState(null);
    const [isDragOver, setIsDragOver] = useState(false);
    const fileInputRef = useRef(null);

    useEffect(() => {
        if (isOpen) {
            setAudioFile(null);
            setAudioUrl(initialAudioUrl || '');
            setError(null);
            setUploadProgress(0);
        }
    }, [isOpen, initialAudioUrl]);

    if (!isOpen) return null;

    // Allowed file types
    const allowedTypes = ['audio/mpeg', 'audio/wav', 'audio/mp3', 'audio/x-wav'];
    const allowedExtensions = ['.mp3', '.wav'];

    // Format file size
    const formatSize = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    // Format duration
    const formatDuration = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    // Validate file
    const validateFile = (file) => {
        if (!file) return 'Không có file được chọn';

        const ext = '.' + file.name.split('.').pop().toLowerCase();
        if (!allowedExtensions.includes(ext) && !allowedTypes.includes(file.type)) {
            return 'Chỉ hỗ trợ file MP3 hoặc WAV';
        }

        const sizeMB = file.size / (1024 * 1024);
        if (sizeMB > maxSizeMB) {
            return `File quá lớn. Tối đa ${maxSizeMB}MB (file hiện tại: ${sizeMB.toFixed(1)}MB)`;
        }

        return null;
    };

    // Handle file selection
    const handleFileSelect = (file) => {
        const validationError = validateFile(file);
        if (validationError) {
            setError(validationError);
            return;
        }

        setError(null);
        setAudioFile(file);

        // Create preview URL
        const previewUrl = URL.createObjectURL(file);
        setAudioUrl(previewUrl);
    };

    const handleUrlChange = (e) => {
        const value = e.target.value.trim();
        setError(null);
        setAudioFile(null);
        setAudioUrl(value);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    // Handle file input change
    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            handleFileSelect(file);
        }
    };

    // Handle drag events
    const handleDragOver = useCallback((e) => {
        e.preventDefault();
        setIsDragOver(true);
    }, []);

    const handleDragLeave = useCallback((e) => {
        e.preventDefault();
        setIsDragOver(false);
    }, []);

    const handleDrop = useCallback((e) => {
        e.preventDefault();
        setIsDragOver(false);

        const file = e.dataTransfer.files[0];
        if (file) {
            handleFileSelect(file);
        }
    }, []);

    // Clear selected file
    const handleClear = () => {
        if (audioUrl && audioUrl.startsWith('blob:')) {
            URL.revokeObjectURL(audioUrl);
        }
        setAudioFile(null);
        setAudioUrl('');
        setError(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    // Handle save/upload
    const handleSave = async () => {
        if (!audioFile && !audioUrl) {
            setError('Vui lòng chọn file audio');
            return;
        }

        if (audioFile && audioUrl && audioUrl.startsWith('blob:')) {
            setError('Hiện chỉ hỗ trợ lưu bằng URL. Vui lòng dán link audio.');
            return;
        }

        if (!audioFile && audioUrl) {
            onSave({
                file: null,
                url: audioUrl,
                name: null,
                size: null
            });
            onClose();
            return;
        }

        setIsUploading(true);
        setUploadProgress(0);

        try {
            // Simulate upload progress (replace with actual upload logic)
            for (let i = 0; i <= 100; i += 10) {
                await new Promise(resolve => setTimeout(resolve, 100));
                setUploadProgress(i);
            }

            // In a real implementation, you would:
            // 1. Upload file to Supabase Storage
            // 2. Get the public URL
            // 3. Pass that URL to onSave

            // For now, we'll pass the local preview URL or file
            onSave({
                file: audioFile,
                url: audioUrl,
                name: audioFile?.name,
                size: audioFile?.size
            });

            setIsUploading(false);
            onClose();
        } catch (err) {
            setError('Lỗi khi tải lên: ' + err.message);
            setIsUploading(false);
        }
    };

    return (
        <div className="admin-modal-overlay" onClick={onClose}>
            <div className="admin-modal admin-modal--medium" onClick={e => e.stopPropagation()}>
                {/* Header */}
                <div className="admin-modal__header">
                    <h3>{title}</h3>
                    <button className="admin-modal__close" onClick={onClose}>
                        <FiX size={20} />
                    </button>
                </div>

                {/* Content */}
                <div className="admin-modal__content">
                    <div className="audio-upload-url">
                        <label className="audio-upload-url__label">Audio URL (tuỳ chọn)</label>
                        <input
                            type="text"
                            className="audio-upload-url__input"
                            placeholder="https://example.com/audio.mp3"
                            value={audioFile ? '' : audioUrl}
                            onChange={handleUrlChange}
                        />
                        <p className="audio-upload-url__hint">Dán URL audio nếu đã có link sẵn.</p>
                    </div>
                    {/* Dropzone */}
                    {!audioUrl && (
                        <div
                            className={`audio-upload-dropzone ${isDragOver ? 'drag-over' : ''}`}
                            onDragOver={handleDragOver}
                            onDragLeave={handleDragLeave}
                            onDrop={handleDrop}
                            onClick={() => fileInputRef.current?.click()}
                        >
                            <div className="icon">🎵</div>
                            <h4>Kéo thả file audio vào đây</h4>
                            <p>hoặc click để chọn file</p>
                            <p className="file-types">Hỗ trợ: MP3, WAV (tối đa {maxSizeMB}MB)</p>
                        </div>
                    )}

                    <input
                        ref={fileInputRef}
                        type="file"
                        accept=".mp3,.wav,audio/mpeg,audio/wav"
                        onChange={handleFileChange}
                        style={{ display: 'none' }}
                    />

                    {/* Audio Preview */}
                    {audioUrl && (
                        <div className="audio-upload-preview">
                            <div className="preview-header">
                                <FiMusic size={24} />
                                <div className="file-details">
                                    <span className="file-name">{audioFile?.name || 'Audio file'}</span>
                                    {audioFile && (
                                        <span className="file-size">{formatSize(audioFile.size)}</span>
                                    )}
                                </div>
                                <button
                                    className="btn-clear"
                                    onClick={handleClear}
                                    title="Xóa"
                                >
                                    <FiTrash2 size={18} />
                                </button>
                            </div>

                            <audio
                                controls
                                src={audioUrl}
                                className="audio-player"
                            />
                        </div>
                    )}

                    {/* Upload Progress */}
                    {isUploading && (
                        <div className="audio-upload-progress">
                            <div className="progress-bar">
                                <div
                                    className="progress-fill"
                                    style={{ width: `${uploadProgress}%` }}
                                />
                            </div>
                            <div className="progress-text">
                                <span>Đang tải lên...</span>
                                <span>{uploadProgress}%</span>
                            </div>
                        </div>
                    )}

                    {/* Error */}
                    {error && (
                        <div className="passage-input-error">
                            <FiAlertTriangle size={16} />
                            {error}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="admin-modal__footer">
                    <button className="admin-btn admin-btn--secondary" onClick={onClose}>
                        Hủy
                    </button>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={handleSave}
                        disabled={(!audioFile && !audioUrl) || isUploading}
                    >
                        <FiCheck size={16} />
                        <span>{isUploading ? 'Đang tải...' : 'Lưu Audio'}</span>
                    </button>
                </div>
            </div>
        </div>
    );
}
