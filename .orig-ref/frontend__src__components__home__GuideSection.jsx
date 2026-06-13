import React, { useState, useRef } from 'react';
import { FaUserPlus, FaClipboardCheck, FaGraduationCap, FaArrowRight, FaArrowLeft, FaCheck } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

const GuideSection = () => {
  const navigate = useNavigate();
  const sectionRef = useRef(null);
  const [activeStep, setActiveStep] = useState(0);
  const [completedSteps, setCompletedSteps] = useState([]);

  const handleStartClick = () => {
    navigate('/login');
    window.scrollTo(0, 0);
  };

  const steps = [
    {
      step: '01',
      icon: FaUserPlus,
      title: 'Đăng ký tài khoản',
      description: 'Tạo tài khoản miễn phí chỉ trong 30 giây. Bạn có thể đăng ký bằng email hoặc tài khoản Google.',
      details: [
        'Điền thông tin cơ bản: tên, email, mật khẩu',
        'Hoặc đăng ký nhanh với Google/Facebook',
        'Xác nhận email để kích hoạt tài khoản',
        'Hoàn toàn miễn phí, không cần thẻ tín dụng'
      ],
      color: '#7c3aed',
    },
    {
      step: '02',
      icon: FaClipboardCheck,
      title: 'Làm bài test đầu vào',
      description: 'Hoàn thành bài kiểm tra đánh giá năng lực để hệ thống hiểu rõ trình độ hiện tại của bạn.',
      details: [
        'Bài test ngắn 15-20 phút',
        'Đánh giá 4 kỹ năng: Reading, Listening, Writing, Speaking',
        'Kết quả chi tiết theo từng band điểm',
        'Phân tích điểm mạnh và điểm cần cải thiện'
      ],
      color: '#6366f1',
    },
    {
      step: '03',
      icon: FaGraduationCap,
      title: 'Nhận lộ trình học tập',
      description: 'AI phân tích kết quả và tạo lộ trình học tập cá nhân hóa, giúp bạn cải thiện từng kỹ năng.',
      details: [
        'Lộ trình được thiết kế riêng cho bạn',
        'Bài học sắp xếp theo độ khó phù hợp',
        'Theo dõi tiến độ realtime',
        'Điều chỉnh lộ trình khi cần thiết'
      ],
      color: '#8b5cf6',
    },
  ];

  const handleNextStep = () => {
    if (activeStep < steps.length - 1) {
      if (!completedSteps.includes(activeStep)) {
        setCompletedSteps([...completedSteps, activeStep]);
      }
      setActiveStep(activeStep + 1);
    }
  };

  const handlePrevStep = () => {
    if (activeStep > 0) {
      setActiveStep(activeStep - 1);
    }
  };

  const handleStepClick = (index) => {
    // Mark all previous steps as completed when clicking ahead
    if (index > activeStep) {
      const newCompleted = [...completedSteps];
      for (let i = activeStep; i < index; i++) {
        if (!newCompleted.includes(i)) {
          newCompleted.push(i);
        }
      }
      setCompletedSteps(newCompleted);
    }
    setActiveStep(index);
  };

  const currentStep = steps[activeStep];
  const Icon = currentStep.icon;

  return (
    <section ref={sectionRef} className="guide-section guide-section--interactive">
      <div className="guide-container">
        {/* Header — framer-motion whileInView */}
        <motion.div
          className="guide-header in-view"
          initial={{ opacity: 0, y: 40 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.3 }}
          transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        >
          <motion.span
            className="guide-label"
            initial={{ opacity: 0, scale: 0.8 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            transition={{ type: 'spring', stiffness: 120, damping: 14, delay: 0.1 }}
          >
            Bắt đầu dễ dàng
          </motion.span>
          <h2 className="guide-title">
            Chỉ <span className="text-gradient">3 bước đơn giản</span>
            <br />
            để bắt đầu hành trình
          </h2>
          <p className="guide-subtitle">
            Không cần chuẩn bị gì phức tạp, Cramer sẽ hướng dẫn bạn từng bước một
          </p>
        </motion.div>

        {/* Interactive Journey */}
        <div className="journey-container">
          {/* Step indicators */}
          <div className="journey-steps">
            {steps.map((step, index) => (
              <div
                key={index}
                className={`journey-step-indicator ${index === activeStep ? 'active' : ''} ${completedSteps.includes(index) ? 'completed' : ''}`}
                onClick={() => handleStepClick(index)}
              >
                <div
                  className="journey-step-circle"
                  style={{
                    borderColor: index === activeStep ? step.color : undefined,
                    background: completedSteps.includes(index) ? step.color : undefined
                  }}
                >
                  {completedSteps.includes(index) ? (
                    <FaCheck className="journey-check-icon" />
                  ) : (
                    <span>{step.step}</span>
                  )}
                </div>
                <span className="journey-step-label">{step.title}</span>
                {index < steps.length - 1 && (
                  <div
                    className={`journey-step-line ${completedSteps.includes(index) ? 'completed' : ''}`}
                    style={{ background: completedSteps.includes(index) ? step.color : undefined }}
                  />
                )}
              </div>
            ))}
          </div>

          {/* Content card with animation */}
          <div className="journey-content-wrapper">
            <AnimatePresence mode="wait">
              <motion.div
                key={activeStep}
                className="journey-content"
                initial={{ opacity: 0, x: 50, scale: 0.95 }}
                animate={{ opacity: 1, x: 0, scale: 1 }}
                exit={{ opacity: 0, x: -50, scale: 0.95 }}
                transition={{ duration: 0.3, ease: "easeOut" }}
              >
                <div
                  className="journey-content-card"
                  style={{
                    borderColor: `${currentStep.color}30`,
                  }}
                >
                  {/* Icon section */}
                  <div
                    className="journey-icon-section"
                    style={{ background: `linear-gradient(135deg, ${currentStep.color}15 0%, ${currentStep.color}05 100%)` }}
                  >
                    <div
                      className="journey-icon-wrapper"
                      style={{ background: currentStep.color }}
                    >
                      <Icon className="journey-icon" />
                    </div>
                    <span className="journey-step-number" style={{ color: currentStep.color }}>
                      Bước {currentStep.step}
                    </span>
                  </div>

                  {/* Details section */}
                  <div className="journey-details-section">
                    <h3 className="journey-content-title">{currentStep.title}</h3>
                    <p className="journey-content-description">{currentStep.description}</p>

                    <ul className="journey-details-list">
                      {currentStep.details.map((detail, i) => (
                        <motion.li
                          key={i}
                          initial={{ opacity: 0, x: 20 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: 0.1 + i * 0.08 }}
                        >
                          <span
                            className="journey-detail-bullet"
                            style={{ background: currentStep.color }}
                          />
                          {detail}
                        </motion.li>
                      ))}
                    </ul>
                  </div>
                </div>

                {/* Navigation buttons */}
                <div className="journey-navigation">
                  <button
                    className="journey-nav-btn journey-nav-btn--prev"
                    onClick={handlePrevStep}
                    disabled={activeStep === 0}
                  >
                    <FaArrowLeft />
                    <span>Trước</span>
                  </button>

                  {activeStep === steps.length - 1 ? (
                    <button
                      className="journey-nav-btn journey-nav-btn--start"
                      onClick={handleStartClick}
                    >
                      <span>Bắt đầu ngay</span>
                      <FaArrowRight />
                    </button>
                  ) : (
                    <button
                      className="journey-nav-btn journey-nav-btn--next"
                      onClick={handleNextStep}
                    >
                      <span>Tiếp theo</span>
                      <FaArrowRight />
                    </button>
                  )}
                </div>
              </motion.div>
            </AnimatePresence>
          </div>
        </div>
      </div>
    </section>
  );
};

export default GuideSection;