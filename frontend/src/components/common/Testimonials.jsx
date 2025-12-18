import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FaQuoteLeft, FaChevronLeft, FaChevronRight, FaStar } from 'react-icons/fa';
import '../../css/common/testimonials.css';

/**
 * Reusable Testimonials Component (Carousel or Grid)
 * 
 * @param {Object} props
 * @param {Array<{quote: string, author: string, role?: string, avatar?: string, rating?: number, detail?: string}>} props.items - Testimonial items
 * @param {string} [props.title] - Optional section title
 * @param {string} [props.subtitle] - Optional section subtitle
 * @param {string} [props.label] - Optional label badge above title
 * @param {'carousel' | 'grid'} [props.variant] - Display variant (default: 'carousel')
 * @param {boolean} [props.autoPlay] - Enable auto-rotation for carousel (default: true)
 * @param {number} [props.autoPlayInterval] - Auto-rotation interval in ms (default: 5000)
 * @param {boolean} [props.showHeader] - Whether to show the header section (default: true)
 * @param {string} [props.className] - Additional CSS class for the container
 */
const Testimonials = ({
  items = [],
  title = 'Đánh giá từ học viên',
  subtitle,
  label,
  variant = 'carousel',
  autoPlay: autoPlayProp = true,
  autoPlayInterval = 5000,
  showHeader = true,
  className = '',
}) => {
  const sectionRef = useRef(null);
  const [headerInView, setHeaderInView] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const [autoPlay, setAutoPlay] = useState(autoPlayProp);

  // Header intersection observer for animation
  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setHeaderInView(true);
          observer.unobserve(entry.target);
        }
      },
      { threshold: 0.2 }
    );

    if (sectionRef.current) {
      observer.observe(sectionRef.current);
    }

    return () => {
      if (sectionRef.current) observer.unobserve(sectionRef.current);
    };
  }, []);

  // Auto-play carousel
  useEffect(() => {
    if (!autoPlay || variant !== 'carousel' || !items.length) return;

    const interval = setInterval(() => {
      setActiveIndex((prev) => (prev + 1) % items.length);
    }, autoPlayInterval);

    return () => clearInterval(interval);
  }, [autoPlay, variant, items.length, autoPlayInterval]);

  const handlePrev = () => {
    setAutoPlay(false);
    setActiveIndex((prev) => (prev - 1 + items.length) % items.length);
  };

  const handleNext = () => {
    setAutoPlay(false);
    setActiveIndex((prev) => (prev + 1) % items.length);
  };

  const handleDotClick = (index) => {
    setAutoPlay(false);
    setActiveIndex(index);
  };

  // Generate initials for avatar placeholder
  const getInitials = (name) => {
    if (!name) return '?';
    return name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
  };

  if (!items || items.length === 0) {
    return null;
  }

  const containerClasses = [
    'testimonials-section',
    `testimonials-section--${variant}`,
    className,
  ].filter(Boolean).join(' ');

  const currentTestimonial = items[activeIndex];

  return (
    <section ref={sectionRef} className={containerClasses}>
      <div className="testimonials-container">
        {/* Header */}
        {showHeader && (
          <div className={`testimonials-header ${headerInView ? 'in-view' : ''}`}>
            {label && <span className="testimonials-label">{label}</span>}
            {title && (
              <h2 className="testimonials-title">
                {typeof title === 'string' ? title : title}
              </h2>
            )}
            {subtitle && <p className="testimonials-subtitle">{subtitle}</p>}
          </div>
        )}

        {/* Carousel Variant */}
        {variant === 'carousel' && (
          <>
            <div className="testimonial-carousel">
              <AnimatePresence mode="wait">
                <motion.div
                  key={activeIndex}
                  className="testimonial-card"
                  initial={{ opacity: 0, x: 50, scale: 0.95 }}
                  animate={{ opacity: 1, x: 0, scale: 1 }}
                  exit={{ opacity: 0, x: -50, scale: 0.95 }}
                  transition={{ duration: 0.4, ease: 'easeOut' }}
                >
                  {/* Quote icon */}
                  <div className="testimonial-quote-icon">
                    <FaQuoteLeft />
                  </div>

                  {/* Quote text */}
                  <p className="testimonial-quote">"{currentTestimonial.quote}"</p>

                  {/* Rating */}
                  {currentTestimonial.rating && (
                    <div className="testimonial-rating">
                      {[...Array(currentTestimonial.rating)].map((_, i) => (
                        <FaStar key={i} className="star-icon" />
                      ))}
                    </div>
                  )}

                  {/* Author info */}
                  <div className="testimonial-author">
                    <div className="testimonial-avatar">
                      {currentTestimonial.avatar ? (
                        <img src={currentTestimonial.avatar} alt={currentTestimonial.author} />
                      ) : (
                        <span className="avatar-initials">
                          {getInitials(currentTestimonial.author)}
                        </span>
                      )}
                    </div>
                    <div className="testimonial-author-info">
                      <span className="testimonial-author-name">{currentTestimonial.author}</span>
                      <span className="testimonial-author-role">
                        {currentTestimonial.detail || currentTestimonial.role}
                      </span>
                    </div>
                  </div>
                </motion.div>
              </AnimatePresence>

              {/* Navigation arrows */}
              <button 
                className="testimonial-nav testimonial-nav--prev" 
                onClick={handlePrev}
                aria-label="Previous testimonial"
              >
                <FaChevronLeft />
              </button>
              <button 
                className="testimonial-nav testimonial-nav--next" 
                onClick={handleNext}
                aria-label="Next testimonial"
              >
                <FaChevronRight />
              </button>
            </div>

            {/* Dots indicator */}
            <div className="testimonial-dots">
              {items.map((_, index) => (
                <button
                  key={index}
                  className={`testimonial-dot ${index === activeIndex ? 'active' : ''}`}
                  onClick={() => handleDotClick(index)}
                  aria-label={`Go to testimonial ${index + 1}`}
                />
              ))}
            </div>
          </>
        )}

        {/* Grid Variant */}
        {variant === 'grid' && (
          <motion.div
            className="testimonials-grid"
            initial="hidden"
            animate={headerInView ? 'visible' : 'hidden'}
            variants={{
              hidden: { opacity: 0 },
              visible: {
                opacity: 1,
                transition: { staggerChildren: 0.1 }
              }
            }}
          >
            {items.map((item, index) => (
              <motion.div
                key={index}
                className="testimonial-grid-card"
                variants={{
                  hidden: { y: 20, opacity: 0 },
                  visible: { y: 0, opacity: 1, transition: { duration: 0.4 } }
                }}
              >
                {/* Stars */}
                {item.rating && (
                  <div className="testimonial-grid-stars">
                    {[...Array(item.rating)].map((_, i) => (
                      <FaStar key={i} className="star-icon" />
                    ))}
                  </div>
                )}

                {/* Quote */}
                <p className="testimonial-grid-quote">"{item.quote}"</p>

                {/* Author */}
                <div className="testimonial-grid-author">
                  <div className="testimonial-grid-avatar">
                    {item.avatar ? (
                      <img src={item.avatar} alt={item.author} />
                    ) : (
                      getInitials(item.author)
                    )}
                  </div>
                  <div className="testimonial-grid-info">
                    <span className="testimonial-grid-name">{item.author}</span>
                    <span className="testimonial-grid-detail">
                      {item.detail || item.role}
                    </span>
                  </div>
                </div>
              </motion.div>
            ))}
          </motion.div>
        )}
      </div>
    </section>
  );
};

export default Testimonials;
