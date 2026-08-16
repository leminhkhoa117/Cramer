import BaseModal from './common/BaseModal';
import InButtonSpinner from './common/InButtonSpinner';

/**
 * ResumeConfirmationModal - Resume or start new test attempt
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onResume - Resume/view result handler
 * @param {function} onStartNew - Start new attempt handler
 * @param {boolean} isStartingNew - Loading state for "Làm bài mới"
 * @param {boolean} isResuming - Loading state for "Tiếp tục làm bài" / "Xem kết quả"
 * @param {string} attemptStatus - 'IN_PROGRESS' | 'COMPLETED'
 */
const ResumeConfirmationModal = ({ 
  isOpen, 
  onResume, 
  onStartNew, 
  isStartingNew, 
  isResuming = false,
  attemptStatus = 'IN_PROGRESS' 
}) => {
  const isCompleted = attemptStatus === 'COMPLETED';
  const title = isCompleted ? 'Đã có bài làm trước đó' : 'Bài làm đang dang dở';
  const message = isCompleted 
    ? 'Bạn đã hoàn thành bài test này trước đó. Bạn muốn xem kết quả hay làm bài mới?'
    : 'Chúng tôi tìm thấy một lần làm bài chưa hoàn thành cho bài test này. Bạn muốn tiếp tục hay bắt đầu một bài mới?';
  const resumeButtonText = isCompleted ? 'Xem kết quả' : 'Tiếp tục làm bài';
  const isLoading = isStartingNew || isResuming;

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={() => {}} // No close on backdrop - user must choose
      title={title}
      showCloseButton={false}
      closeOnBackdropClick={false}
      footer={
        <>
          <button 
            type="button"
            className="cm-btn cm-btn--secondary" 
            onClick={onStartNew}
            disabled={isLoading}
          >
            {isStartingNew ? (
              <InButtonSpinner label="Đang tạo..." variant="dark" />
            ) : 'Làm bài mới'}
          </button>
          <button 
            type="button"
            className="cm-btn cm-btn--primary" 
            onClick={onResume}
            disabled={isLoading}
          >
            {isResuming ? (
              <InButtonSpinner label={isCompleted ? 'Đang tải...' : 'Đang tiếp tục...'} />
            ) : resumeButtonText}
          </button>
        </>
      }
    >
      <p className="cm-description">{message}</p>
    </BaseModal>
  );
};

export default ResumeConfirmationModal;
