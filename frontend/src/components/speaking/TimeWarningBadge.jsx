import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import PropTypes from 'prop-types';

/**
 * TimeWarningBadge - Displays time remaining warnings during speaking session
 *
 * Shows:
 * - 15s warning: Yellow "soft" warning
 * - 5s warning: Red "hard" warning with pulse animation
 */
export default function TimeWarningBadge({ warningLevel, remainingSeconds }) {
  if (!warningLevel || remainingSeconds <= 0) {
    return null;
  }

  const isSoft = warningLevel === 'SOFT';
  const isHard = warningLevel === 'HARD';

  return (
    <AnimatePresence>
      <motion.div
        className={`time-warning-badge ${isSoft ? 'time-warning-badge--soft' : ''} ${isHard ? 'time-warning-badge--hard' : ''}`}
        initial={{ opacity: 0, scale: 0.9, y: -10 }}
        animate={{
          opacity: 1,
          scale: isHard ? [1, 1.05, 1] : 1,
          y: 0,
        }}
        exit={{ opacity: 0, scale: 0.9, y: -10 }}
        transition={{
          duration: 0.3,
          scale: isHard ? { repeat: Infinity, duration: 0.5 } : {},
        }}
        role="alert"
        aria-live="assertive"
      >
        <span className="time-warning-badge__icon">
          {isSoft ? '⏱️' : '⚠️'}
        </span>
        <span className="time-warning-badge__text">
          {isSoft
            ? `${remainingSeconds}s còn lại`
            : `Chỉ còn ${remainingSeconds}s!`}
        </span>
      </motion.div>
    </AnimatePresence>
  );
}

TimeWarningBadge.propTypes = {
  warningLevel: PropTypes.oneOf(['SOFT', 'HARD', null]),
  remainingSeconds: PropTypes.number,
};

TimeWarningBadge.defaultProps = {
  warningLevel: null,
  remainingSeconds: 0,
};
