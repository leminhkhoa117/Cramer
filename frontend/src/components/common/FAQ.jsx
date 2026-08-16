import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FaChevronDown, FaQuestionCircle } from 'react-icons/fa';
import '../../css/common/faq.css';

/**
 * Reusable FAQ Component
 *
 * @param {Object} props
 * @param {Array<{question: string, answer: string}>} props.items - FAQ items array
 * @param {string} [props.title] - Optional section title (default: "Câu hỏi thường gặp")
 * @param {string} [props.subtitle] - Optional section subtitle
 * @param {string} [props.label] - Optional label badge above title
 * @param {string} [props.variant] - Style variant: 'default' | 'compact' | 'pricing'
 * @param {number} [props.defaultOpenIndex] - Index of initially open FAQ item (default: 0)
 * @param {boolean} [props.showHeader] - Whether to show the header section (default: true)
 * @param {string} [props.className] - Additional CSS class for the container
 */
const FAQ = ({
  items = [],
  title = 'Câu hỏi thường gặp',
  subtitle,
  label,
  variant = 'default',
  defaultOpenIndex = 0,
  showHeader = true,
  className = '',
}) => {
  const sectionRef = useRef(null);
  const [headerInView, setHeaderInView] = useState(false);
  const [openIndex, setOpenIndex] = useState(defaultOpenIndex);

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

  const toggleFAQ = (index) => {
    setOpenIndex(openIndex === index ? -1 : index);
  };

  if (!items || items.length === 0) {
    return null;
  }

  const containerClasses = [
    'faq-section',
    `faq-section--${variant}`,
    className,
  ].filter(Boolean).join(' ');

  return (
    <section ref={sectionRef} className={containerClasses}>
      <div className="faq-container">
        {/* Header */}
        {showHeader && (
          <div className={`faq-header ${headerInView ? 'in-view' : ''}`}>
            {label && <span className="faq-label">{label}</span>}
            {title && (
              <h2 className="faq-title">
                {typeof title === 'string' ? title : title}
              </h2>
            )}
            {subtitle && <p className="faq-subtitle">{subtitle}</p>}
          </div>
        )}

        {/* FAQ Items */}
        <div className="faq-list">
          {items.map((faq, index) => (
            <motion.div
              key={index}
              className={`faq-item ${openIndex === index ? 'open' : ''}`}
              initial={{ opacity: 0, y: 20 }}
              animate={headerInView ? { opacity: 1, y: 0 } : {}}
              transition={{ delay: index * 0.05 }}
            >
              <button
                className="faq-question"
                onClick={() => toggleFAQ(index)}
                aria-expanded={openIndex === index}
              >
                {variant !== 'pricing' && (
                  <span className="faq-question-icon">
                    <FaQuestionCircle />
                  </span>
                )}
                <span className="faq-question-text">{faq.question}</span>
                <span className={`faq-chevron ${openIndex === index ? 'rotated' : ''}`}>
                  <FaChevronDown />
                </span>
              </button>

              <AnimatePresence>
                {openIndex === index && (
                  <motion.div
                    className="faq-answer-wrapper"
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.3, ease: 'easeInOut' }}
                  >
                    <div className="faq-answer">
                      {typeof faq.answer === 'string' ? (
                        <p>{faq.answer}</p>
                      ) : (
                        faq.answer
                      )}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default FAQ;
