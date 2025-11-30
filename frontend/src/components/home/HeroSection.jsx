import React from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import logoImage from '../../../pictures/logo/Icon.png';

const HeroSection = () => {
  const navigate = useNavigate();

  const handleNavigate = (path) => {
    window.scrollTo(0, 0);
    navigate(path);
  };

  // Animation variants for entrance animations
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.15,
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

  return (
    <section className="hero-section">
      {/* Floating orbs background - CSS animated for performance */}
      <div className="hero-orbs">
        <div className="hero-orb hero-orb--1" />
        <div className="hero-orb hero-orb--2" />
        <div className="hero-orb hero-orb--3" />
        <div className="hero-orb hero-orb--4" />
        <div className="hero-orb hero-orb--5" />
      </div>

      <motion.div
        className="hero-content"
        variants={containerVariants}
        initial="hidden"
        animate="visible"
      >
        {/* Main headline */}
        <motion.h1 className="hero-headline" variants={textVariants}>
          <span className="hero-headline-accent">Chinh phục IELTS</span>
          <br />
          <span className="hero-headline-with-logo">
            cùng {' '}
            <img src={logoImage} alt="Cramer" className="hero-logo-inline" />
          </span>
        </motion.h1>

        {/* Subheadline */}
        <motion.p className="hero-subheadline" variants={textVariants}>
          Nền tảng luyện thi IELTS thông minh với công nghệ AI,
          giúp bạn đạt band điểm mơ ước một cách hiệu quả nhất.
        </motion.p>

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

        {/* Scroll indicator - part of content flow */}
        <motion.div className="hero-scroll-indicator" variants={textVariants}>
          <div className="scroll-mouse">
            <div className="scroll-wheel" />
          </div>
          <span className="scroll-text">Cuộn xuống</span>
        </motion.div>
      </motion.div >
    </section >
  );
};

export default HeroSection;
