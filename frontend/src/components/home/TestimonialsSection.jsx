import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FaQuoteLeft, FaChevronLeft, FaChevronRight, FaStar } from 'react-icons/fa';

const TestimonialsSection = () => {
    const sectionRef = useRef(null);
    const [headerInView, setHeaderInView] = useState(false);
    const [activeIndex, setActiveIndex] = useState(0);
    const [autoPlay, setAutoPlay] = useState(true);

    // Header intersection observer
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
        if (!autoPlay) return;

        const interval = setInterval(() => {
            setActiveIndex((prev) => (prev + 1) % testimonials.length);
        }, 5000);

        return () => clearInterval(interval);
    }, [autoPlay]);

    const testimonials = [
        {
            quote: "Đây là một trong những web vippro nhất mà mình từng được thử qua với nhiều dạng đề, thật sự là rất yêu founder của trang này!",
            author: "Chí Phong",
            role: "Học viên IELTS",
            avatar: "https://scontent.fcxr1-1.fna.fbcdn.net/v/t39.30808-6/472721511_1380192343154381_7028061101838486876_n.jpg?_nc_cat=107&ccb=1-7&_nc_sid=a5f93a&_nc_ohc=yrhtnUVylgQQ7kNvwFEcxcq&_nc_oc=AdmHUUD-WoCd4bfPaHbrkiyLe7F9GFGSs1GSJ7zUmWvLhOC-LqhNxlUeiD0LlBWEDeZx79J9O7FNKU2AYEBluYKT&_nc_zt=23&_nc_ht=scontent.fcxr1-1.fna&_nc_gid=zfWcC7ZyvnrBOUH1SZ95ZA&oh=00_AfkCJjRrZh09CkdZCkEFgSqCefuY8EBy0qzB6ClZuptOOA&oe=694092C7",
            rating: 5,
        },
        {
            quote: "Mình thích nhất là tính năng AI writing, nó vô cùng hữu ích vì chỉ ra được điểm mạnh/yếu rõ ràng, mà điểm còn chính xác",
            author: "Song Vũ",
            role: "Học viên IELTS",
            avatar: null,
            rating: 5,
        },
        {
            quote: "Thật sự ấn tượng với tính năng AI speaking lắm luôn. Bình thường thi thử speaking rất tốn kém, nay có Cramer thì chi phí phải chăng hơn rất nhiều mà còn giống với thi thật nữa",
            author: "Hồng Em",
            role: "Học viên IELTS",
            avatar: null,
            rating: 5,
        },
        {
            quote: "Có rất nhiều tính năng miễn phí, nhưng vì những tính năng trả phí hay ho quá nên mình đã quyết định xuống tiền và thật sự là mình không hề hối hận",
            author: "Minh Anh",
            role: "Học viên IELTS",
            avatar: null,
            rating: 5,
        },
    ];

    const handlePrev = () => {
        setAutoPlay(false);
        setActiveIndex((prev) => (prev - 1 + testimonials.length) % testimonials.length);
    };

    const handleNext = () => {
        setAutoPlay(false);
        setActiveIndex((prev) => (prev + 1) % testimonials.length);
    };

    const handleDotClick = (index) => {
        setAutoPlay(false);
        setActiveIndex(index);
    };

    const currentTestimonial = testimonials[activeIndex];

    // Generate initials for avatar placeholder
    const getInitials = (name) => {
        return name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
    };

    return (
        <section ref={sectionRef} className="testimonials-section">
            <div className="testimonials-container">
                {/* Header */}
                <div className={`testimonials-header ${headerInView ? 'in-view' : ''}`}>
                    <span className="testimonials-label">Đánh giá từ học viên</span>
                    <h2 className="testimonials-title">
                        Học viên nói gì về
                        <br />
                        <span className="text-gradient">Cramer</span>
                    </h2>
                </div>

                {/* Testimonial Carousel */}
                <div className="testimonial-carousel">
                    <AnimatePresence mode="wait">
                        <motion.div
                            key={activeIndex}
                            className="testimonial-card"
                            initial={{ opacity: 0, x: 50, scale: 0.95 }}
                            animate={{ opacity: 1, x: 0, scale: 1 }}
                            exit={{ opacity: 0, x: -50, scale: 0.95 }}
                            transition={{ duration: 0.4, ease: "easeOut" }}
                        >
                            {/* Quote icon */}
                            <div className="testimonial-quote-icon">
                                <FaQuoteLeft />
                            </div>

                            {/* Quote text */}
                            <p className="testimonial-quote">"{currentTestimonial.quote}"</p>

                            {/* Rating */}
                            <div className="testimonial-rating">
                                {[...Array(currentTestimonial.rating)].map((_, i) => (
                                    <FaStar key={i} className="star-icon" />
                                ))}
                            </div>

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
                                    <span className="testimonial-author-role">{currentTestimonial.role}</span>
                                </div>
                            </div>
                        </motion.div>
                    </AnimatePresence>

                    {/* Navigation arrows */}
                    <button className="testimonial-nav testimonial-nav--prev" onClick={handlePrev}>
                        <FaChevronLeft />
                    </button>
                    <button className="testimonial-nav testimonial-nav--next" onClick={handleNext}>
                        <FaChevronRight />
                    </button>
                </div>

                {/* Dots indicator */}
                <div className="testimonial-dots">
                    {testimonials.map((_, index) => (
                        <button
                            key={index}
                            className={`testimonial-dot ${index === activeIndex ? 'active' : ''}`}
                            onClick={() => handleDotClick(index)}
                        />
                    ))}
                </div>
            </div>
        </section>
    );
};

export default TestimonialsSection;
