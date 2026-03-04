import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FiClock, FiSkipForward } from 'react-icons/fi';
import { useSpeakingStore } from '../../stores';
import '../../css/speaking/speaking-components.css';

/**
 * Part2PrepLayout - Part 2 preparation phase
 *
 * Shows cue card and allows user to take notes for 1 minute
 * User can skip preparation with a confirmation modal
 */
export default function Part2PrepLayout() {
  const {
    currentQuestion,
    prepTimer,
    userNotes,
    setUserNotes,
    finishPrep,
    tickPrepTimer,
  } = useSpeakingStore();

  // State for skip confirmation modal
  const [showSkipConfirm, setShowSkipConfirm] = useState(false);

  /**
   * Prep timer countdown
   */
  useEffect(() => {
    if (prepTimer > 0) {
      const interval = setInterval(() => {
        tickPrepTimer();
      }, 1000);

      return () => clearInterval(interval);
    }
  }, [prepTimer, tickPrepTimer]);

  /**
   * Auto-transition when prep time ends
   */
  useEffect(() => {
    if (prepTimer === 0) {
      setTimeout(() => {
        finishPrep();
      }, 500);
    }
  }, [prepTimer, finishPrep]);

  const isWarning = prepTimer <= 15;
  const isCritical = prepTimer <= 5;

  // Calculate progress percentage
  const progressPercent = ((60 - prepTimer) / 60) * 100;

  return (
    <div className="speaking-part2-prep">
      <motion.div
        className="speaking-part2-prep__container"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        {/* Header */}
        <motion.div
          className="speaking-part2-prep__header"
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="speaking-part2-prep__title">Part 2: Long Turn</h1>
          <p className="speaking-part2-prep__subtitle">
            Bạn có <strong>1 phút</strong> để chuẩn bị. Sau đó bạn sẽ nói trong <strong>1-2 phút</strong>.
          </p>
        </motion.div>

        {/* Cue Card */}
        <motion.div
          className="speaking-cuecard"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.2 }}
        >
          <div className="speaking-cuecard__header">
            <span className="speaking-cuecard__badge">CUE CARD</span>
          </div>

          <h2 className="speaking-cuecard__prompt">
            {currentQuestion?.text || 'Loading question...'}
          </h2>

          {currentQuestion?.cueCardBullets && (
            <>
              <p className="speaking-cuecard__intro">You should say:</p>
              <ul className="speaking-cuecard__bullets">
                {currentQuestion.cueCardBullets.map((bullet, index) => (
                  <motion.li
                    key={index}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.3 + index * 0.1 }}
                  >
                    {bullet}
                  </motion.li>
                ))}
              </ul>
            </>
          )}
        </motion.div>

        {/* Notes Box */}
        <motion.div
          className="speaking-notes"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
        >
          <label className="speaking-notes__label">
            <span>Ghi chú của bạn:</span>
            <span className="speaking-notes__counter">{userNotes.length}/500</span>
          </label>
          <textarea
            className="speaking-notes__textarea"
            placeholder="Ghi chú những ý chính bạn muốn nói... (không bắt buộc)"
            value={userNotes}
            onChange={(e) => setUserNotes(e.target.value.slice(0, 500))}
            maxLength={500}
          />
        </motion.div>

        {/* Timer */}
        <motion.div
          className={`speaking-prep-timer ${isWarning ? 'warning' : ''} ${isCritical ? 'critical' : ''}`}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
        >
          <div className="speaking-prep-timer__header">
            <FiClock className="speaking-prep-timer__icon" />
            <span className="speaking-prep-timer__time">
              {prepTimer} giây còn lại
            </span>
          </div>

          <div className="speaking-prep-timer__bar">
            <motion.div
              className="speaking-prep-timer__fill"
              initial={{ width: '0%' }}
              animate={{ width: `${progressPercent}%` }}
              transition={{ duration: 0.3 }}
            />
          </div>

          <p className="speaking-prep-timer__message">
            {prepTimer > 15 && 'Hãy chuẩn bị những ý chính bạn muốn nói'}
            {prepTimer <= 15 && prepTimer > 5 && 'Chuẩn bị hoàn tất câu trả lời của bạn'}
            {prepTimer <= 5 && 'Bạn sắp bắt đầu nói...'}
          </p>

          {/* Skip Preparation Button */}
          <motion.button
            className="speaking-prep-timer__skip-btn"
            onClick={() => setShowSkipConfirm(true)}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.8 }}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
          >
            <FiSkipForward size={16} />
            Bỏ qua chuẩn bị
          </motion.button>
        </motion.div>
      </motion.div>

      {/* Skip Confirmation Modal */}
      <AnimatePresence>
        {showSkipConfirm && (
          <motion.div
            className="speaking-modal-overlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setShowSkipConfirm(false)}
          >
            <motion.div
              className="speaking-modal"
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              onClick={(e) => e.stopPropagation()}
            >
              <h3 className="speaking-modal__title">Bỏ qua thời gian chuẩn bị?</h3>
              <p className="speaking-modal__message">
                Bạn có chắc muốn bắt đầu nói ngay không?
                <br />
                <small>Còn {prepTimer} giây để chuẩn bị.</small>
              </p>
              <div className="speaking-modal__actions">
                <button
                  className="speaking-modal__btn speaking-modal__btn--cancel"
                  onClick={() => setShowSkipConfirm(false)}
                >
                  Tiếp tục chuẩn bị
                </button>
                <button
                  className="speaking-modal__btn speaking-modal__btn--confirm"
                  onClick={() => {
                    setShowSkipConfirm(false);
                    finishPrep();
                  }}
                >
                  Bắt đầu nói ngay
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
