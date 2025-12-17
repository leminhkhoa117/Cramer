import React, { useState, useRef, useEffect } from 'react';
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import { FiUploadCloud, FiTrash2, FiRefreshCw } from 'react-icons/fi';
import 'react-image-crop/dist/ReactCrop.css';
import { showErrorToast } from '../utils/toast';

// Helper to get the cropped image
function getCroppedImg(image, crop, fileName) {
    const canvas = document.createElement('canvas');
    const scaleX = image.naturalWidth / image.width;
    const scaleY = image.naturalHeight / image.height;
    canvas.width = crop.width;
    canvas.height = crop.height;
    const ctx = canvas.getContext('2d');

    ctx.drawImage(
        image,
        crop.x * scaleX,
        crop.y * scaleY,
        crop.width * scaleX,
        crop.height * scaleY,
        0,
        0,
        crop.width,
        crop.height
    );

    return new Promise((resolve) => {
        canvas.toBlob((blob) => {
            if (!blob) {
                console.error('Canvas is empty');
                return;
            }
            blob.name = fileName;
            resolve(blob);
        }, 'image/jpeg', 0.95);
    });
}

const CroppableImageTab = ({ onFileCropped, aspectRatio = 1, minWidth = 100, maxSizeMB = 5 }) => {
    const [imgSrc, setImgSrc] = useState('');
    const [crop, setCrop] = useState();
    const [completedCrop, setCompletedCrop] = useState();
    const [isProcessing, setIsProcessing] = useState(false);
    const imgRef = useRef(null);
    const fileInputRef = useRef(null);

    function onImageLoad(e) {
        const { width, height } = e.currentTarget;
        const crop = centerCrop(
            makeAspectCrop(
                {
                    unit: '%',
                    width: 90,
                },
                aspectRatio,
                width,
                height
            ),
            width,
            height
        );
        setCrop(crop);
    }

    const handleFileChange = (e) => {
        if (e.target.files && e.target.files.length > 0) {
            const file = e.target.files[0];

            // 1. Validate Size
            if (file.size > maxSizeMB * 1024 * 1024) {
                showErrorToast(`File quá lớn! Vui lòng chọn ảnh dưới ${maxSizeMB}MB.`);
                return;
            }

            setIsProcessing(true);
            setCrop(undefined);

            // 2. Use URL.createObjectURL for better memory performance (no base64)
            const objectUrl = URL.createObjectURL(file);
            const img = new Image();
            img.src = objectUrl;
            img.onload = () => {
                const canvas = document.createElement('canvas');
                let { width, height } = img;
                const MAX_WIDTH = 1500; // Limit width for performance

                if (width > MAX_WIDTH) {
                    height = Math.round((height * MAX_WIDTH) / width);
                    width = MAX_WIDTH;
                }

                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0, width, height);
                
                // Clean up object URL after use
                URL.revokeObjectURL(objectUrl);

                setImgSrc(canvas.toDataURL('image/jpeg', 0.9));
                setIsProcessing(false);
            };
            img.onerror = () => {
                URL.revokeObjectURL(objectUrl);
                setIsProcessing(false);
                showErrorToast('Không thể đọc ảnh. Vui lòng thử lại.');
            };
        }
    };

    const handleCrop = async () => {
        if (completedCrop?.width && completedCrop?.height && imgRef.current) {
            const croppedImageBlob = await getCroppedImg(
                imgRef.current,
                completedCrop,
                'cropped-image.jpg'
            );
            onFileCropped(croppedImageBlob);
        }
    };

    // Live review - debounced for performance
    useEffect(() => {
        const timer = setTimeout(() => {
            handleCrop();
        }, 400); // Increased debounce for smoother performance
        return () => clearTimeout(timer);
    }, [completedCrop]);


    return (
        <div className="upload-tab-content">
            {!imgSrc && (
                <>
                    <input
                        type="file"
                        accept="image/png, image/jpeg, image/jpg"
                        onChange={handleFileChange}
                        ref={fileInputRef}
                        hidden
                    />
                    <div className="upload-drop-zone" onClick={() => fileInputRef.current.click()}>
                        <div className="upload-placeholder">
                            {isProcessing ? (
                                <FiRefreshCw className="animate-spin" size={40} />
                            ) : (
                                <FiUploadCloud size={50} />
                            )}
                            <p>Tải lên hình ảnh</p>
                            <span>(Tối đa {maxSizeMB}MB, JPG/PNG)</span>
                        </div>
                    </div>
                </>
            )}
            {imgSrc && (
                <div className="cropper-container">
                    <ReactCrop
                        crop={crop}
                        onChange={(_, percentCrop) => setCrop(percentCrop)}
                        onComplete={(c) => setCompletedCrop(c)}
                        aspect={aspectRatio}
                        minWidth={minWidth}
                    >
                        <img
                            ref={imgRef}
                            alt="Crop me"
                            src={imgSrc}
                            onLoad={onImageLoad}
                            style={{ maxHeight: '350px', maxWidth: '100%' }}
                        />
                    </ReactCrop>
                    <button onClick={() => { setImgSrc(''); onFileCropped(null); }} className="btn-change-image">
                        <FiTrash2 /> Chọn ảnh khác
                    </button>
                </div>
            )}
        </div>
    );
};

export default CroppableImageTab;
