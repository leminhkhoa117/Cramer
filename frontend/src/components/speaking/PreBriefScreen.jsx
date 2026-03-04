import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { FiMic, FiClock, FiCheckCircle, FiLoader } from 'react-icons/fi';
import { useSpeakingStore } from '../../stores';

/**
 * PreBriefScreen - Introduction and consent screen
 *
 * Explains the test format and requests recording permission
 * Topic selection is bypassed - topics will come from test attempts
 */
export default function PreBriefScreen() {
  const [hasConsent, setHasConsent] = useState(false);
  const [isStarting, setIsStarting] = useState(false);
  const [error, setError] = useState(null);

  const acceptConsent = useSpeakingStore(state => state.acceptConsent);
  const mode = useSpeakingStore(state => state.mode);
  const createSessionOnBackend = useSpeakingStore(state => state.createSessionOnBackend);

  const handleStart = async () => {
    if (!hasConsent) {
      alert('Vui lòng đồng ý cho phép ghi âm để tiếp tục.');
      return;
    }

    setIsStarting(true);
    setError(null);

    try {
      // Create session on backend without topic (will use test attempt questions)
      await createSessionOnBackend(mode, null);

      // Accept consent and move to session
      acceptConsent();
    } catch (err) {
      console.error('Failed to create session:', err);
      setError(err.response?.data?.message || 'Không thể tạo phiên thi. Vui lòng thử lại.');
      setIsStarting(false);
    }
  };

  // Get duration based on mode
  const getDuration = () => {
    switch (mode) {
      case 'FULL': return '11-14 phút';
      case 'PART_1': return '4-5 phút';
      case 'PART_2': return '3-4 phút';
      case 'PART_3': return '4-5 phút';
      case 'PART_2_3': return '7-9 phút';
      default: return '11-14 phút';
    }
  };

  const canStart = hasConsent && !isStarting;

  return (
    <div className="speaking-prebrief">
      <motion.div
        className="speaking-prebrief__container"
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5 }}
      >
        {/* AI Avatar */}
        <motion.div
          className="speaking-prebrief__avatar"
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
        >
          <span className="speaking-prebrief__avatar-icon">🤖</span>
        </motion.div>

        {/* Title */}
        <motion.h1
          className="speaking-prebrief__title"
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          Chào mừng đến với IELTS Speaking
        </motion.h1>

        <motion.p
          className="speaking-prebrief__subtitle"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
        >
          Bạn sắp bước vào buổi thi Speaking mô phỏng với AI Examiner
        </motion.p>

        {/* Info Card */}
        <motion.div
          className="speaking-prebrief__card"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
        >
          <h2 className="speaking-prebrief__card-title">Thông tin buổi thi</h2>

          <div className="speaking-prebrief__info-list">
            <div className="speaking-prebrief__info-item">
              <FiMic className="speaking-prebrief__info-icon" />
              <div>
                <strong>Ghi âm:</strong> Micro của bạn sẽ được ghi âm trong suốt buổi thi
              </div>
            </div>

            <div className="speaking-prebrief__info-item">
              <FiClock className="speaking-prebrief__info-icon" />
              <div>
                <strong>Thời gian:</strong> {getDuration()}
              </div>
            </div>

            <div className="speaking-prebrief__info-item">
              <FiCheckCircle className="speaking-prebrief__info-icon" />
              <div>
                <strong>Đánh giá:</strong> Bạn sẽ nhận được phản hồi chi tiết sau khi hoàn thành
              </div>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="speaking-prebrief__error">
              <span>⚠️ {error}</span>
            </div>
          )}

          {/* Consent Checkbox */}
          <div className="speaking-prebrief__consent">
            <label className="speaking-prebrief__consent-label">
              <input
                type="checkbox"
                checked={hasConsent}
                onChange={(e) => setHasConsent(e.target.checked)}
                className="speaking-prebrief__consent-checkbox"
                disabled={isStarting}
              />
              <span>
                Tôi đồng ý cho phép ghi âm và hiểu rằng dữ liệu sẽ được sử dụng để đánh giá kỹ năng Speaking của tôi
              </span>
            </label>
          </div>

          {/* Start Button */}
          <button
            className={`speaking-prebrief__btn ${!canStart ? 'disabled' : ''}`}
            onClick={handleStart}
            disabled={!canStart}
          >
            {isStarting ? (
              <>
                <FiLoader className="spinning" /> Đang khởi tạo...
              </>
            ) : (
              'Bắt đầu buổi thi'
            )}
          </button>
        </motion.div>

        {/* Tips */}
        <motion.div
          className="speaking-prebrief__tips"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6 }}
        >
          <p className="speaking-prebrief__tip">
            💡 <strong>Mẹo:</strong> Đảm bảo bạn ở nơi yên tĩnh với kết nối internet ổn định
          </p>
        </motion.div>
      </motion.div>
    </div>
  );
}
