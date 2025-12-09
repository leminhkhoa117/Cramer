import React, { useRef, useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  FaBookOpen,
  FaRobot,
  FaRoute,
  FaChartLine,
  FaHeadphones,
  FaPen
} from 'react-icons/fa';

// Feature images - imported using ES modules
import featureImg1 from '../../../pictures/images/1_courses.jpg';
import featureImg2 from '../../../pictures/images/2_AI_evaluation.jpg';
import featureImg3 from '../../../pictures/images/3_personalized.jpg';
import featureImg4 from '../../../pictures/images/4_dashboard.jpg';
import featureImg5 from '../../../pictures/images/5_listening.jpg';
import featureImg6 from '../../../pictures/images/6_writing.jpg';

// Single feature card for stacked reveal
const StackedFeatureCard = ({ feature, index, progress }) => {
  // Calculate card-specific transforms based on scroll progress
  const cardProgress = Math.max(0, Math.min(1, (progress - index * 0.08) / 0.35));

  // STRONGER reveal effect
  const translateY = (1 - cardProgress) * (120 + index * 40);
  const scale = 0.75 + cardProgress * 0.25;
  const opacity = cardProgress;
  const rotateX = (1 - cardProgress) * 25;

  // STRONGER Zig-zag pattern: 150px offset
  const zigzagOffset = index % 2 === 0 ? -150 : 150;
  const zigzagX = (1 - cardProgress) * zigzagOffset;

  return (
    <motion.div
      className={`stacked-feature-card stacked-feature-card--${index} ${index % 2 === 0 ? 'stacked-feature-card--left' : 'stacked-feature-card--right'}`}
      style={{
        transform: `perspective(1000px) translateY(${translateY}px) translateX(${zigzagX}px) scale(${scale}) rotateX(${rotateX}deg)`,
        opacity,
        zIndex: 10 - index,
      }}
    >
      <div className="stacked-feature-card-inner">
        <div className="stacked-feature-left">
          <div className={`stacked-feature-icon ${feature.gradient}`}>
            <feature.icon />
          </div>
          <div className="stacked-feature-content">
            <h3 className="stacked-feature-title">{feature.title}</h3>
            <p className="stacked-feature-description">{feature.description}</p>
            {feature.highlights && (
              <ul className="stacked-feature-highlights">
                {feature.highlights.map((highlight, i) => (
                  <li key={i}>{highlight}</li>
                ))}
              </ul>
            )}
          </div>
        </div>
        {/* 21:9 Image */}
        <div className="stacked-feature-image">
          {feature.image ? (
            <img
              src={feature.image}
              alt={feature.title}
              className="stacked-feature-img"
            />
          ) : (
            <div className="stacked-feature-image-placeholder">
              <span>Screenshot</span>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  );
};

const FeaturesSection = () => {
  const sectionRef = useRef(null);
  const headerRef = useRef(null);
  const [headerInView, setHeaderInView] = useState(false);
  const [scrollProgress, setScrollProgress] = useState(0);

  // Track scroll progress within the section
  useEffect(() => {
    const handleScroll = () => {
      if (!sectionRef.current) return;

      const rect = sectionRef.current.getBoundingClientRect();
      const sectionHeight = rect.height;
      const windowHeight = window.innerHeight;

      const scrolled = windowHeight - rect.top;
      const totalScrollDistance = sectionHeight + windowHeight;
      const progress = Math.max(0, Math.min(1, scrolled / totalScrollDistance));

      setScrollProgress(progress);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Header intersection observer
  useEffect(() => {
    const headerObserver = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setHeaderInView(true);
          headerObserver.unobserve(entry.target);
        }
      },
      { threshold: 0.2 }
    );

    if (headerRef.current) {
      headerObserver.observe(headerRef.current);
    }

    return () => {
      if (headerRef.current) headerObserver.unobserve(headerRef.current);
    };
  }, []);

  // Features data with imported images
  const features = [
    {
      icon: FaBookOpen,
      title: 'Kho đề thi phong phú',
      description: 'Truy cập hơn 1000+ đề thi IELTS từ Cambridge, British Council và IDP với môi trường thi thử chân thực.',
      highlights: [
        'Đề thi Reading & Listening cập nhật mới nhất',
        'Thời gian và format giống phòng thi thật',
        'Chấm điểm tự động ngay lập tức'
      ],
      gradient: 'gradient-purple',
      image: featureImg1,
    },
    {
      icon: FaRobot,
      title: 'AI Đánh giá thông minh',
      description: 'Công nghệ AI tiên tiến phân tích bài viết và bài nói, đưa ra nhận xét chi tiết theo tiêu chí chấm điểm IELTS.',
      highlights: [
        'Chấm điểm Writing Task 1 & Task 2',
        'Phân tích từ vựng và ngữ pháp',
        'Gợi ý cải thiện cụ thể'
      ],
      gradient: 'gradient-blue',
      image: featureImg2,
    },
    {
      icon: FaRoute,
      title: 'Lộ trình cá nhân hóa',
      description: 'Hệ thống tự động phân tích điểm mạnh, điểm yếu và thiết kế lộ trình học tập phù hợp với mục tiêu của bạn.',
      highlights: [
        'Đánh giá năng lực ban đầu',
        'Lộ trình theo band điểm mục tiêu',
        'Điều chỉnh linh hoạt theo tiến độ'
      ],
      gradient: 'gradient-teal',
      image: featureImg3,
    },
    {
      icon: FaChartLine,
      title: 'Theo dõi tiến độ chi tiết',
      description: 'Dashboard trực quan với biểu đồ và báo cáo giúp bạn nắm bắt sự tiến bộ theo từng kỹ năng.',
      highlights: [
        'Biểu đồ tiến độ theo thời gian',
        'So sánh với mục tiêu đặt ra',
        'Phân tích điểm cần cải thiện'
      ],
      gradient: 'gradient-orange',
      image: featureImg4,
    },
    {
      icon: FaHeadphones,
      title: 'Luyện Listening đa dạng',
      description: 'Bài tập nghe với nhiều giọng nói khác nhau: British, American, Australian giúp làm quen với format thi thật.',
      highlights: [
        'Đa dạng chủ đề và giọng nói',
        'Tốc độ phát điều chỉnh được',
        'Script và giải thích chi tiết'
      ],
      gradient: 'gradient-purple',
      image: featureImg5,
    },
    {
      icon: FaPen,
      title: 'Writing Workshop',
      description: 'Kho bài mẫu band 7-9, từ vựng theo chủ đề và hướng dẫn cấu trúc bài viết chuẩn IELTS.',
      highlights: [
        'Bài mẫu cho mọi dạng đề',
        'Từ vựng và collocations',
        'Template và cấu trúc chuẩn'
      ],
      gradient: 'gradient-blue',
      image: featureImg6,
    },
  ];

  return (
    <section ref={sectionRef} className="features-section features-section--stacked">
      <div className="features-container">
        {/* Header */}
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
            Cramer tích hợp đầy đủ công cụ và tài nguyên để giúp bạn đạt band điểm mơ ước
          </p>
        </div>

        {/* Stacked cards container */}
        <div className="stacked-features-container">
          {features.map((feature, index) => (
            <StackedFeatureCard
              key={index}
              feature={feature}
              index={index}
              progress={scrollProgress}
            />
          ))}
        </div>
      </div>
    </section>
  );
};

export default FeaturesSection;
