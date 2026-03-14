import React, { useEffect, useRef, useState } from 'react';
import { motion, useInView } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useCountUp } from '../../hooks/useCountUp';
import logoImage from '../../../pictures/logo/Icon.png';

// Animated stat counter component
const HeroStat = ({ target, suffix, label, isActive, delay = 0 }) => {
  const value = useCountUp(target, 2000, isActive);

  return (
    <motion.div
      className="hero-stat"
      initial={{ opacity: 0, y: 20 }}
      animate={isActive ? { opacity: 1, y: 0 } : {}}
      transition={{ type: 'spring', stiffness: 80, damping: 18, delay }}
    >
      <span className="hero-stat-number">
        {value.toLocaleString()}{suffix}
      </span>
      <span className="hero-stat-label">{label}</span>
    </motion.div>
  );
};

const HeroSection = () => {
  const navigate = useNavigate();
  const heroRef = useRef(null);
  const statsRef = useRef(null);
  const [scrollProgress, setScrollProgress] = useState(0);
  const [enableParallax, setEnableParallax] = useState(() => {
    if (typeof window === 'undefined') return true;
    return window.innerWidth > 992;
  });
  const statsInView = useInView(statsRef, { once: true, amount: 0.5 });

  useEffect(() => {
    const mediaQuery = window.matchMedia('(max-width: 992px)');
    const updateParallaxMode = () => setEnableParallax(!mediaQuery.matches);

    updateParallaxMode();
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', updateParallaxMode);
    } else {
      mediaQuery.addListener(updateParallaxMode);
    }

    return () => {
      if (mediaQuery.removeEventListener) {
        mediaQuery.removeEventListener('change', updateParallaxMode);
      } else {
        mediaQuery.removeListener(updateParallaxMode);
      }
    };
  }, []);

  // Parallax scroll handler
  useEffect(() => {
    if (!enableParallax) {
      setScrollProgress(0);
      return undefined;
    }

    const handleScroll = () => {
      if (!heroRef.current) return;

      const rect = heroRef.current.getBoundingClientRect();
      const heroHeight = rect.height;
      const scrolled = -rect.top;
      const progress = Math.max(0, Math.min(1, scrolled / heroHeight));

      setScrollProgress(progress);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, [enableParallax]);

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
  const contentOpacity = enableParallax ? Math.max(0, 1 - scrollProgress * 1.5) : 1;
  const contentScale = enableParallax ? 1 - scrollProgress * 0.1 : 1;

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

        {/* Animated stat counters */}
        <motion.div ref={statsRef} className="hero-stats-bar" variants={textVariants}>
          <HeroStat target={10000} suffix="+" label="Học viên" isActive={statsInView} delay={0} />
          <div className="hero-stat-divider" />
          <HeroStat target={500} suffix="+" label="Đề thi" isActive={statsInView} delay={0.15} />
          <div className="hero-stat-divider" />
          <HeroStat target={95} suffix="%" label="Hài lòng" isActive={statsInView} delay={0.3} />
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
