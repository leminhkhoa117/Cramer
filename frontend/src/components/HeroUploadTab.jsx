import React, { useState, useRef } from 'react';
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import { FiUploadCloud } from 'react-icons/fi';

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
        }, 'image/jpeg');
    });
}


const HeroUploadTab = ({ onFileCropped }) => {
    const [imgSrc, setImgSrc] = useState('');
    const [crop, setCrop] = useState();
    const [completedCrop, setCompletedCrop] = useState();
    const imgRef = useRef(null);
    const fileInputRef = useRef(null);

    // Recommended aspect ratio for the hero image
    const aspect = 16 / 5;

    function onImageLoad(e) {
        imgRef.current = e.currentTarget;
        const { width, height } = e.currentTarget;
        const crop = centerCrop(
            makeAspectCrop(
                {
                    unit: '%',
                    width: 90,
                },
                aspect,
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
            setCrop(undefined); // Makes crop preview update between images
            const reader = new FileReader();
            reader.addEventListener('load', () => setImgSrc(reader.result.toString() || ''));
            reader.readAsDataURL(e.target.files[0]);
        }
    };
    
    const handleCrop = async () => {
        if (completedCrop?.width && completedCrop?.height && imgRef.current) {
            const croppedImageBlob = await getCroppedImg(
                imgRef.current,
                completedCrop,
                'hero-background.jpg'
            );
            onFileCropped(croppedImageBlob);
        }
    };
    
    // This effect calls the handleCrop whenever the user stops dragging the crop area
    // This provides a "live review"
    React.useEffect(() => {
        handleCrop();
    }, [completedCrop]);


    return (
        <div className="upload-tab-content">
            {!imgSrc && (
                <>
                    <input
                        type="file"
                        accept="image/*"
                        onChange={handleFileChange}
                        ref={fileInputRef}
                        hidden
                    />
                    <div className="upload-drop-zone" onClick={() => fileInputRef.current.click()}>
                         <div className="upload-placeholder">
                            <FiUploadCloud size={50} />
                            <p>Tải lên ảnh bìa của bạn</p>
                            <span>Ảnh sẽ được cắt theo tỷ lệ 16:5</span>
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
                        aspect={aspect}
                        minWidth={200}
                    >
                        <img
                            ref={imgRef}
                            alt="Crop me"
                            src={imgSrc}
                            onLoad={onImageLoad}
                            style={{ maxHeight: '350px' }}
                        />
                    </ReactCrop>
                     <button onClick={() => setImgSrc('')} className="btn-change-image">
                        Chọn ảnh khác
                    </button>
                </div>
            )}
        </div>
    );
};

export default HeroUploadTab;
