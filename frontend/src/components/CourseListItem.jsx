import React, { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { FiChevronDown, FiBookOpen, FiHeadphones, FiEdit2, FiMic } from 'react-icons/fi';
import { motion, AnimatePresence } from 'framer-motion';
import AttemptHistoryDropdown from './AttemptHistoryDropdown';
import '../css/course-list.css';

const formatDate = (dateString) => {
  if (!dateString) return 'Chưa có';
  return new Date(dateString).toLocaleDateString('vi-VN');
};

const formatSkillName = (skill) => {
  if (!skill) return '';
  return skill.charAt(0).toUpperCase() + skill.slice(1);
};

const formatCourseTitle = (course) => {
  const setName = course.setName || course.examSource;
  const testName = course.testName || `Test ${course.testNumber}`;
  return `${setName} - ${testName}`;
};

const formatStatus = (status) => {
  const statusMap = {
    'COMPLETED': 'Hoàn thành',
    'IN_PROGRESS': 'Đang làm',
    'GRADING': 'Đang chấm',
    'PENDING': 'Chờ xử lý',
    'FAILED': 'Lỗi',
    'NOT_STARTED': 'Chưa bắt đầu',
  };
  return statusMap[status] || status;
};

const skillIconMap = {
  reading: FiBookOpen,
  listening: FiHeadphones,
  writing: FiEdit2,
  speaking: FiMic,
};

const skillColorMap = {
  reading: { bg: 'rgba(34, 197, 94, 0.1)', text: '#16a34a' },
  listening: { bg: 'rgba(59, 130, 246, 0.1)', text: '#2563eb' },
  writing: { bg: 'rgba(245, 158, 11, 0.1)', text: '#d97706' },
  speaking: { bg: 'rgba(239, 68, 68, 0.1)', text: '#dc2626' },
};

const CourseListItem = React.memo(({ course, onAttemptDeleted }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const completion = Math.min(100, Math.max(0, Math.round((course.completionRate ?? 0) * 100)));
  const skillColors = skillColorMap[course.skill?.toLowerCase()] || skillColorMap.reading;

  const metaItems = useMemo(() => [
    { label: 'Kỹ năng', value: formatSkillName(course.skill) },
    { label: 'Trạng thái', value: formatStatus(course.status), className: `status-${course.status?.toLowerCase()}` },
    { label: 'Đã làm', value: `${course.answersAttempted}/${course.totalQuestions || '?'} câu` },
    { label: 'Đúng', value: `${course.correctAnswers} câu` },
    { label: 'Lần cuối', value: formatDate(course.lastAttempt) },
    { label: 'Hoàn thành', value: `${completion}%` },
  ], [course, completion]);

  const toggleExpand = () => setIsExpanded(prev => !prev);

  return (
    <motion.div
      className={`dash-course-item ${isExpanded ? 'dash-course-item--expanded' : ''}`}
      layout
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
      onMouseMove={(e) => {
        const rect = e.currentTarget.getBoundingClientRect();
        e.currentTarget.style.setProperty('--mouse-x', `${e.clientX - rect.left}px`);
        e.currentTarget.style.setProperty('--mouse-y', `${e.clientY - rect.top}px`);
      }}
    >
      <div className="dash-course-item__header" onClick={toggleExpand} role="button" tabIndex={0} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') toggleExpand(); }}>
        <div className="dash-course-item__thumbnail">
          {course.coverImageUrl ? (
            <img src={course.coverImageUrl} alt="" className="dash-course-item__thumbnail-img" loading="lazy" />
          ) : (
            <div
              className="dash-course-item__thumbnail-placeholder"
              style={{ background: skillColors.bg, color: skillColors.text }}
            >
              {React.createElement(skillIconMap[course.skill?.toLowerCase()] || FiBookOpen, {
                className: 'dash-course-item__thumbnail-icon',
              })}
            </div>
          )}
        </div>

        <div className="dash-course-item__main">
          <h3 className="dash-course-item__title">{formatCourseTitle(course)}</h3>
          <div className="dash-course-item__badges">
            <span
              className="dash-course-item__skill-badge"
              style={{ background: skillColors.bg, color: skillColors.text }}
            >
              {formatSkillName(course.skill)}
            </span>
            <span className={`dash-course-item__status-dot status-${course.status?.toLowerCase()}`} />
            <span className="dash-course-item__date-text">{formatDate(course.lastAttempt)}</span>
          </div>
        </div>

        <div className="dash-course-item__metrics">
          {course.status === 'GRADING' ? (
            <span className="dash-course-item__score dash-course-item__score--grading">Đang chấm...</span>
          ) : course.bandScore != null ? (
            <span className="dash-course-item__score">Band {course.bandScore.toFixed(1)}</span>
          ) : (
            <span className="dash-course-item__progress-text">{completion}%</span>
          )}
        </div>

        <div className="dash-course-item__expand">
          <FiChevronDown className={`dash-course-item__expand-icon ${isExpanded ? 'dash-course-item__expand-icon--rotated' : ''}`} />
        </div>
      </div>

      <AnimatePresence initial={false}>
        {isExpanded && (
          <motion.div
            className="dash-course-item__body"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
          >
            <div className="dash-course-item__details">
              <div className="dash-course-item__meta">
                {metaItems.map((item) => (
                  <div key={item.label} className="dash-course-item__meta-item">
                    <span className="dash-course-item__meta-label">{item.label}</span>
                    <span className={`dash-course-item__meta-value ${item.className || ''}`}>{item.value}</span>
                  </div>
                ))}
              </div>

              <div className="dash-course-item__actions">
                {course.status === 'COMPLETED' && (
                  <>
                    {course.attemptId && (
                      <Link
                        to={course.skill === 'writing'
                          ? `/test/writing/review/${course.attemptId}`
                          : `/test/review/${course.attemptId}`
                        }
                        className="btn-action-dashboard btn-action-dashboard--outline"
                      >
                        Xem lại
                      </Link>
                    )}
                    <Link
                      to={course.skill === 'writing'
                        ? `/test/writing/${course.examSource}/${course.testNumber}`
                        : `/test/${course.examSource}/${course.testNumber}/${course.skill}`
                      }
                      state={{ forceNew: true }}
                      className="btn-action-dashboard"
                    >
                      Làm lại
                    </Link>
                  </>
                )}
                {course.status === 'IN_PROGRESS' && (
                  <Link
                    to={course.skill === 'writing'
                      ? `/test/writing/${course.examSource}/${course.testNumber}`
                      : `/test/${course.examSource}/${course.testNumber}/${course.skill}`
                    }
                    className="btn-action-dashboard"
                  >
                    Tiếp tục
                  </Link>
                )}
                {course.status === 'GRADING' && (
                  <Link
                    to={course.skill === 'writing'
                      ? `/test/writing/review/${course.attemptId}`
                      : `/test/review/${course.attemptId}`
                    }
                    className="btn-action-dashboard btn-action-dashboard--grading"
                  >
                    Xem tiến độ
                  </Link>
                )}
                {course.status !== 'COMPLETED' && course.status !== 'IN_PROGRESS' && course.status !== 'GRADING' && (
                  <Link
                    to={`/test/${course.examSource}/${course.testNumber}/${course.skill}`}
                    className="btn-action-dashboard"
                  >
                    Bắt đầu
                  </Link>
                )}
              </div>

              <AttemptHistoryDropdown
                history={course.history}
                examSource={course.examSource}
                testNumber={course.testNumber}
                skill={course.skill}
                onAttemptDeleted={onAttemptDeleted}
                alwaysOpen
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
});

CourseListItem.displayName = 'CourseListItem';

export default CourseListItem;
