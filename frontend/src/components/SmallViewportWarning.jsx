import { useState, useEffect } from 'react';
import { FiAlertTriangle } from 'react-icons/fi';

export default function SmallViewportWarning({ minWidth = 768 }) {
  const [isSmall, setIsSmall] = useState(false);

  useEffect(() => {
    const check = () => setIsSmall(window.innerWidth < minWidth);
    check();
    window.addEventListener('resize', check);
    return () => window.removeEventListener('resize', check);
  }, [minWidth]);

  if (!isSmall) return null;

  return (
    <div className="viewport-warning-overlay">
      <div className="viewport-warning-card">
        <FiAlertTriangle className="viewport-warning-icon" />
        <h2>Màn hình quá nhỏ</h2>
        <p>
          Trang này yêu cầu màn hình rộng ít nhất <strong>{minWidth}px</strong> để hiển thị đầy đủ nội dung bài thi.
          Vui lòng sử dụng thiết bị có màn hình lớn hơn hoặc xoay ngang màn hình.
        </p>
      </div>
    </div>
  );
}
