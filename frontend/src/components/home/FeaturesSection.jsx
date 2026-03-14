import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  FaBookOpen,
  FaRobot,
  FaRoute,
  FaChartLine,
  FaHeadphones,
  FaPen,
} from 'react-icons/fa';

import FeatureVisualHost from './features/FeatureVisualHost';
import { useStackedReveal } from './features/useStackedReveal';

const features = [
  {
    icon: FaBookOpen,
    stepLabel: 'Kho đề',
    title: 'Kho đề phong phú',
    eyebrow: 'Nhiều bộ đề và kỹ năng để bạn khám phá',
    description: 'Tìm đề nhanh chóng, nhiều tuỳ chọn thi thử, nhanh chóng và không rườm rà.',
    highlights: [
      'Tìm đề thi thông minh',
      'Full kỹ năng',
      'Thao tác nhanh gọn',
    ],
    gradient: 'gradient-purple',
    visual: 'library',
  },
  {
    icon: FaRoute,
    stepLabel: 'Thi thử',
    title: 'Mô phỏng thi thật',
    eyebrow: 'Trải nghiệm thi máy xứng tầm',
    description: 'Giao diện sát thi thật, dễ sử dụng với nhiều cải tiến, hỗ trợ lưu trạng thái bài làm.',
    highlights: [
      'Nhiều tuỳ chọn giao diện làm bài',
      'Chuyển câu nhanh chóng',
      'Nhiều tính năng bổ trợ',
    ],
    gradient: 'gradient-blue',
    visual: 'test-simulator',
  },
  {
    icon: FaRobot,
    stepLabel: 'AI chấm',
    title: 'Chấm bài bằng AI',
    eyebrow: 'Hỗ trợ chấm bài nâng cao bằng AI',
    description: 'Nhiều loại đề thi, có kết quả thi nhanh chóng, feedback siêu chi tiết sau khi nộp bài.',
    highlights: [
      'Chấm chữa siêu chi tiết',
      'Nhận xét chính xác và đúng trọng tâm',
      'Dễ dàng nắm bắt điểm cần cải thiện',
    ],
    gradient: 'gradient-teal',
    visual: 'ai-evaluation',
  },
  {
    icon: FaHeadphones,
    stepLabel: 'Speaking',
    title: 'Thi Speaking như thật',
    eyebrow: 'Luyện Speaking theo part linh hoạt với giám khảo AI (In development)',
    description: 'Dễ dàng chọn lựa chế độ và topic ngẫu nhiên, trải nghiệm thi siêu thật với giám khảo AI, chấm chữa siêu chi tiết và phân tích giọng nói nâng cao (Beta) (In development).',
    highlights: [
      'Giao diện hỗ trợ làm quen (In development)',
      'Phản hồi thời gian thực (In development)',
      'Có phân tích và transcribe phần đã nói (In development)',
    ],
    gradient: 'gradient-orange',
    visual: 'speaking',
  },
  {
    icon: FaChartLine,
    stepLabel: 'Dashboard',
    title: 'Bảng điều khiển trực quan',
    eyebrow: 'Dễ dàng theo dõi tiến độ và nhận gợi ý cá nhân hoá',
    description: 'Theo dõi lịch sử làm bài với nhiều công cụ trực quan hoá khác nhau cùng với phân tích khách quan từ AI (In development).',
    highlights: [
      'Dễ dàng xem lại bài làm',
      'Cá nhân hoá Bảng điều khiển bằng ảnh nền sinh động',
      'Nhận đánh giá trực tiếp dựa trên những lần làm bài trước đó',
    ],
    gradient: 'gradient-purple',
    visual: 'dashboard',
  },
  {
    icon: FaPen,
    stepLabel: 'Hỗ trợ',
    title: 'Nhiều tính năng hỗ trợ',
    eyebrow: 'Sổ tay từ vựng thông minh và trợ lý Cramer',
    description: 'Dễ dàng lưu từ mới và không cần phải nhập tay, lưu từ mới ở bất cứ đâu (In development), trợ lý ảo cá nhân, dễ dàng truy vấn.',
    highlights: [
      'Lọc từ vựng đã thuộc',
      'Highlight và tra cứu nhanh gọn',
      'Truy vấn không giới hạn',
    ],
    gradient: 'gradient-blue',
    visual: 'support',
  },
];

const FeaturesSection = () => {
  const sectionRef = useRef(null);
  const headerRef = useRef(null);
  const cardRefs = useRef([]);
  const motionRefs = useRef([]);

  const [headerInView, setHeaderInView] = useState(false);
  const [progressStep, setProgressStep] = useState(1);
  const [activeCardIndex, setActiveCardIndex] = useState(0);
  const [cardLiveMask, setCardLiveMask] = useState(() => features.map(() => false));

  const [isCompactViewport, setIsCompactViewport] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.innerWidth <= 768;
  });

  const [prefersReducedMotion, setPrefersReducedMotion] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  });

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;

    const compactQuery = window.matchMedia('(max-width: 768px)');
    const reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');

    const syncMediaState = () => {
      setIsCompactViewport(compactQuery.matches);
      setPrefersReducedMotion(reducedMotionQuery.matches);
    };

    syncMediaState();

    if (compactQuery.addEventListener) {
      compactQuery.addEventListener('change', syncMediaState);
      reducedMotionQuery.addEventListener('change', syncMediaState);
    } else {
      compactQuery.addListener(syncMediaState);
      reducedMotionQuery.addListener(syncMediaState);
    }

    return () => {
      if (compactQuery.removeEventListener) {
        compactQuery.removeEventListener('change', syncMediaState);
        reducedMotionQuery.removeEventListener('change', syncMediaState);
      } else {
        compactQuery.removeListener(syncMediaState);
        reducedMotionQuery.removeListener(syncMediaState);
      }
    };
  }, []);

  useEffect(() => {
    if (typeof IntersectionObserver === 'undefined') {
      setHeaderInView(true);
      return undefined;
    }

    const headerObserver = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) return;

        setHeaderInView(true);
        headerObserver.disconnect();
      },
      { threshold: 0.24 }
    );

    if (headerRef.current) {
      headerObserver.observe(headerRef.current);
    }

    return () => headerObserver.disconnect();
  }, []);

  const disableScrollLinkedMotion = prefersReducedMotion || isCompactViewport;

  const handleLiveMaskChange = useCallback(
    (nextMask) => {
      setCardLiveMask((prevMask) => {
        if (disableScrollLinkedMotion) {
          return nextMask;
        }

        return prevMask.map((wasLive, index) => wasLive || Boolean(nextMask[index]));
      });
    },
    [disableScrollLinkedMotion]
  );

  useStackedReveal({
    sectionRef,
    cardRefs,
    motionRefs,
    cardCount: features.length,
    disabled: disableScrollLinkedMotion,
    onStepChange: setProgressStep,
    onLiveMaskChange: handleLiveMaskChange,
    onActiveIndexChange: setActiveCardIndex,
  });

  const currentStep = String(Math.max(1, Math.min(features.length, progressStep))).padStart(2, '0');
  const totalStep = String(features.length).padStart(2, '0');

  return (
    <section
      ref={sectionRef}
      className="features-section features-section--stacked features-section--zigzag-lite"
      style={{ '--stack-progress': 0 }}
    >
      <div className="features-container">
        <div
          ref={headerRef}
          className={`features-header ${headerInView ? 'in-view' : ''}`}
        >
          <span className="features-label">Tính năng nổi bật</span>
          <h2 className="features-title">
            Tất cả những gì bạn cần
            <br />
            <span className="text-gradient">để chinh phục IELTS</span>
          </h2>
          <p className="features-subtitle">
            Cramer tích hợp đầy đủ công cụ và tài nguyên để giúp bạn đạt band điểm mơ ước.
          </p>
        </div>

        <div className="stacked-progress-shell" aria-hidden="true">
          <span className="stacked-progress-step">
            {currentStep} / {totalStep}
          </span>
          <div className="stacked-progress-track">
            <span className="stacked-progress-fill" />
          </div>
        </div>

        <div className="stacked-features-container">
          {features.map((feature, index) => {
            const Icon = feature.icon;
            const stepText = `${String(index + 1).padStart(2, '0')} · ${feature.stepLabel}`;
            const isLive = cardLiveMask[index] ?? false;
            const isActive = activeCardIndex === index;

            return (
              <article
                key={feature.title}
                ref={(node) => {
                  cardRefs.current[index] = node;
                }}
                className={`stacked-feature-card ${feature.gradient} ${index % 2 === 0 ? 'stacked-feature-card--left' : 'stacked-feature-card--right'} ${isLive ? 'is-live' : ''} ${isActive ? 'is-active' : ''}`}
                style={{ zIndex: features.length - index }}
              >
                <div
                  ref={(node) => {
                    motionRefs.current[index] = node;
                  }}
                  className="stacked-feature-card-motion"
                >
                  <div className="stacked-feature-card-inner">
                    <div className="stacked-feature-left">
                      <div className={`stacked-feature-icon ${feature.gradient}`}>
                        <Icon />
                      </div>

                      <div className="stacked-feature-content">
                        <span className={`stacked-feature-step ${isLive ? 'is-live' : ''}`} data-text={stepText}>
                          {stepText}
                        </span>
                        <span className="stacked-feature-eyebrow">{feature.eyebrow}</span>

                        <h3 className="stacked-feature-title">{feature.title}</h3>
                        <p className="stacked-feature-description">{feature.description}</p>

                        <ul className="stacked-feature-highlights">
                          {feature.highlights.map((highlight, highlightIndex) => (
                            <li
                              key={`${feature.title}-${highlight}`}
                              style={{ '--highlight-delay': `${highlightIndex * 85}ms` }}
                            >
                              {highlight}
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>

                    <div className="stacked-feature-visual-wrap">
                      <FeatureVisualHost visual={feature.visual} />
                    </div>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
};

export default FeaturesSection;
