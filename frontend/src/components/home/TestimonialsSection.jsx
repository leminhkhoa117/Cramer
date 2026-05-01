import React, { useMemo } from 'react';
import { motion } from 'framer-motion';
import { FaQuoteLeft, FaStar } from 'react-icons/fa';
import { TESTIMONIALS } from '../../constants/home';
import { useReducedMotion } from './hooks/useReducedMotion';

/**
 * TestimonialsSection — infinite marquee.
 *
 * Two horizontal rows scrolling in opposite directions. Each row repeats
 * the testimonials twice so the loop is seamless. Hovering pauses the
 * row under the cursor. Respects prefers-reduced-motion.
 */

const getInitials = (name) =>
    name.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase();

const TestimonialCard = ({ item, idx }) => (
    <article
        className="t-card"
        style={{
            // CSS variable used by the tilt + accent border colour
            '--t-accent': item.accent,
        }}
        aria-label={`Đánh giá của ${item.author}`}
    >
        <FaQuoteLeft className="t-card__quote-mark" aria-hidden="true" />
        <p className="t-card__quote">{item.quote}</p>

        <div className="t-card__rating" aria-label="5 trên 5 sao">
            {Array.from({ length: 5 }).map((_, i) => (
                <FaStar key={i} className="t-card__star" />
            ))}
        </div>

        <div className="t-card__footer">
            <div className="t-card__avatar" aria-hidden="true">
                <span>{getInitials(item.author)}</span>
            </div>
            <div className="t-card__person">
                <strong className="t-card__name">{item.author}</strong>
                <span className="t-card__role">{item.role}</span>
            </div>
            <div className="t-card__band" title="Band điểm đạt được">
                <span className="t-card__band-num">{item.band}</span>
                <span className="t-card__band-label">band</span>
            </div>
        </div>

        <span className="t-card__decor t-card__decor--a" aria-hidden="true" />
        <span className="t-card__decor t-card__decor--b" aria-hidden="true" />
    </article>
);

const TestimonialsSection = () => {
    const reduced = useReducedMotion();

    // Duplicate each row so CSS marquee can loop seamlessly
    const rowA = useMemo(() => [...TESTIMONIALS, ...TESTIMONIALS], []);
    const rowB = useMemo(() => [...TESTIMONIALS.slice().reverse(), ...TESTIMONIALS.slice().reverse()], []);

    return (
        <section className="testimonials-section testimonials-section--marquee">
            <div className="testimonials-container">
                <motion.div
                    className="testimonials-header"
                    initial={{ opacity: 0, y: 30 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true, amount: 0.3 }}
                    transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
                >
                    <motion.span
                        className="testimonials-label"
                        initial={{ opacity: 0, scale: 0.85 }}
                        whileInView={{ opacity: 1, scale: 1 }}
                        viewport={{ once: true }}
                        transition={{ type: 'spring', stiffness: 140, damping: 14, delay: 0.1 }}
                    >
                        Đánh giá từ học viên
                    </motion.span>
                    <h2 className="testimonials-title">
                        Học viên nói gì về
                        <br />
                        <span className="text-gradient">Cramer</span>
                    </h2>
                    <p className="testimonials-subtitle">
                        Hơn 10.000 học viên đã đồng hành cùng Cramer trên hành trình chinh phục band điểm mong muốn.
                    </p>
                </motion.div>
            </div>

            <div className={`marquee ${reduced ? 'marquee--paused' : ''}`}>
                <div className="marquee__row marquee__row--a">
                    <div className="marquee__track">
                        {rowA.map((item, i) => (
                            <TestimonialCard key={`a-${i}`} item={item} idx={i} />
                        ))}
                    </div>
                </div>
                <div className="marquee__row marquee__row--b">
                    <div className="marquee__track">
                        {rowB.map((item, i) => (
                            <TestimonialCard key={`b-${i}`} item={item} idx={i} />
                        ))}
                    </div>
                </div>

                {/* Edge fade masks */}
                <div className="marquee__fade marquee__fade--left" aria-hidden="true" />
                <div className="marquee__fade marquee__fade--right" aria-hidden="true" />
            </div>
        </section>
    );
};

export default TestimonialsSection;
