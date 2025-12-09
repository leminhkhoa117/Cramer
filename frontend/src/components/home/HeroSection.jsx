import React, { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import logoImage from '../../../pictures/logo/Icon.png';

const HeroSection = () => {
  const navigate = useNavigate();
  const heroRef = useRef(null);
  const [scrollProgress, setScrollProgress] = useState(0);

  // Parallax scroll handler
  useEffect(() => {
    const handleScroll = () => {
      if (!heroRef.current) return;

      const rect = heroRef.current.getBoundingClientRect();
      const heroHeight = rect.height;
      const scrolled = -rect.top;
      const progress = Math.max(0, Math.min(1, scrolled / heroHeight));

      setScrollProgress(progress);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleNavigate = (path) => {
    window.scrollTo(0, 0);
    navigate(path);
  };

  // Animation variants
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.12,
        delayChildren: 0.2,
      },
    },
  };

  const textVariants = {
    hidden: { opacity: 0, y: 30 },
    visible: {
      opacity: 1,
      y: 0,
      transition: {
        type: 'spring',
        stiffness: 80,
        damping: 20,
      },
    },
  };

  const badgeVariants = {
    hidden: { opacity: 0, scale: 0.8, y: 20 },
    visible: {
      opacity: 1,
      scale: 1,
      y: 0,
      transition: {
        type: 'spring',
        stiffness: 100,
        damping: 15,
      },
    },
  };

  // Floating badges content
  const badges = [
    'Tập luyện miễn phí 100%',
    'Môi trường thi thử giống thật nhất',
    'Nhiều tuỳ chọn học tập',
  ];

  // Content fade out on scroll
  const contentOpacity = Math.max(0, 1 - scrollProgress * 1.5);
  const contentScale = 1 - scrollProgress * 0.1;

  return (
    <section ref={heroRef} className="hero-section hero-section--parallax">
      {/* Main content - Layers 1, 2, 3 removed for clean background */}
      <motion.div
        className="hero-content"
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        style={{
          opacity: contentOpacity,
          transform: `scale(${contentScale})`,
        }}
      >
        {/* Main headline */}
        <motion.h1 className="hero-headline" variants={textVariants}>
          <span className="hero-headline-accent">Chinh phục IELTS</span>
          <br />
          <span className="hero-headline-with-logo">
            cùng{' '}
            <img src={logoImage} alt="Cramer" className="hero-logo-inline" />
          </span>
        </motion.h1>

        {/* Subheadline */}
        <motion.p className="hero-subheadline" variants={textVariants}>
          Nền tảng luyện thi IELTS thông minh với công nghệ AI,
          giúp bạn đạt band điểm mơ ước một cách hiệu quả nhất.
        </motion.p>

        {/* Floating badges */}
        <motion.div className="hero-badges" variants={textVariants}>
          {badges.map((badge, index) => (
            <motion.span
              key={index}
              className="hero-badge"
              variants={badgeVariants}
              custom={index}
            >
              {badge}
            </motion.span>
          ))}
        </motion.div>

        {/* CTA Buttons */}
        <motion.div className="hero-cta-group" variants={textVariants}>
          <button
            onClick={() => handleNavigate('/login')}
            className="hero-cta hero-cta--primary"
          >
            Bắt đầu ngay
          </button>
          <button
            onClick={() => handleNavigate('/courses')}
            className="hero-cta hero-cta--secondary"
          >
            Khám phá khóa học
          </button>
        </motion.div>

        {/* Scroll indicator */}
        <motion.div className="hero-scroll-indicator" variants={textVariants}>
          <div className="scroll-mouse">
            <div className="scroll-wheel" />
          </div>
          <span className="scroll-text">Cuộn xuống</span>
        </motion.div>
      </motion.div>
    </section>
  );
};

export default HeroSection;
