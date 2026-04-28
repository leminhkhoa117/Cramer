import React, { useRef } from 'react';
import { motion, useInView } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useCountUp } from '../../hooks/useCountUp';
import { HERO_BADGES, HERO_STATS } from '../../constants/home';
import logoImage from '../../../pictures/logo/Icon.png';

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
    const statsRef = useRef(null);
    const statsInView = useInView(statsRef, { once: true, amount: 0.5 });

    const handleNavigate = (path) => {
        window.scrollTo(0, 0);
        navigate(path);
    };

    const containerVariants = {
        hidden: { opacity: 0 },
        visible: { opacity: 1, transition: { staggerChildren: 0.12, delayChildren: 0.2 } },
    };
    const textVariants = {
        hidden: { opacity: 0, y: 30 },
        visible: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 80, damping: 20 } },
    };
    const badgeVariants = {
        hidden: { opacity: 0, scale: 0.8, y: 20 },
        visible: { opacity: 1, scale: 1, y: 0, transition: { type: 'spring', stiffness: 100, damping: 15 } },
    };

    return (
        <section className="hero-section">
            <motion.div
                className="hero-content"
                variants={containerVariants}
                initial="hidden"
                animate="visible"
            >
                <motion.span className="hero-eyebrow" variants={textVariants}>
                    <span className="hero-eyebrow__dot" />
                    Nền tảng luyện thi IELTS thế hệ mới
                </motion.span>

                <motion.h1 className="hero-headline" variants={textVariants}>
                    <span className="hero-headline-accent">Chinh phục IELTS</span>
                    <br />
                    <span className="hero-headline-with-logo">
                        cùng{' '}
                        <img src={logoImage} alt="Cramer" className="hero-logo-inline" />
                    </span>
                </motion.h1>

                <motion.p className="hero-subheadline" variants={textVariants}>
                    Nền tảng luyện thi IELTS thông minh với công nghệ AI,
                    giúp bạn đạt band điểm mơ ước một cách hiệu quả nhất.
                </motion.p>

                <motion.div className="hero-badges" variants={textVariants}>
                    {HERO_BADGES.map((badge, index) => (
                        <motion.span key={badge} className="hero-badge" variants={badgeVariants} custom={index}>
                            {badge}
                        </motion.span>
                    ))}
                </motion.div>

                <motion.div className="hero-cta-group" variants={textVariants}>
                    <button
                        onClick={() => handleNavigate('/login')}
                        className="hero-cta hero-cta--primary"
                    >
                        Bắt đầu ngay
                        <span className="hero-cta__arrow" aria-hidden="true">→</span>
                    </button>
                    <button
                        onClick={() => handleNavigate('/courses')}
                        className="hero-cta hero-cta--secondary"
                    >
                        Khám phá khóa học
                    </button>
                </motion.div>

                <motion.div ref={statsRef} className="hero-stats-bar" variants={textVariants}>
                    {HERO_STATS.map((stat, idx) => (
                        <React.Fragment key={stat.label}>
                            <HeroStat
                                target={stat.target}
                                suffix={stat.suffix}
                                label={stat.label}
                                isActive={statsInView}
                                delay={idx * 0.15}
                            />
                            {idx < HERO_STATS.length - 1 && <div className="hero-stat-divider" />}
                        </React.Fragment>
                    ))}
                </motion.div>

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
