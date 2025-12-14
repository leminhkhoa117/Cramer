import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FaChevronDown, FaQuestionCircle } from 'react-icons/fa';

const FAQSection = () => {
    const sectionRef = useRef(null);
    const [headerInView, setHeaderInView] = useState(false);
    const [openIndex, setOpenIndex] = useState(0); // First FAQ open by default

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

    const faqs = [
        {
            question: "Cramer có miễn phí không?",
            answer: "Cramer cung cấp nhiều tính năng miễn phí bao gồm luyện thi Reading, Listening với hàng trăm đề. Các tính năng cao cấp như AI Writing & Speaking, lộ trình cá nhân hóa có phí hợp lý để duy trì chất lượng dịch vụ."
        },
        {
            question: "Làm sao để bắt đầu luyện tập?",
            answer: "Rất đơn giản! Bạn chỉ cần đăng ký tài khoản, làm bài test đầu vào để đánh giá trình độ, sau đó hệ thống sẽ tự động đề xuất lộ trình học tập phù hợp với mục tiêu của bạn."
        },
        {
            question: "AI chấm điểm Writing và Speaking có chính xác không?",
            answer: "AI của Cramer được huấn luyện dựa trên tiêu chí chấm điểm chính thức của IELTS và liên tục được cải tiến. Độ chính xác đạt trung bình 0.5 band so với giám khảo thật, giúp bạn tự tin hơn khi thi thật."
        },
        {
            question: "Tôi có thể hủy đăng ký bất cứ lúc nào không?",
            answer: "Hoàn toàn có thể! Bạn có thể hủy đăng ký gói premium bất cứ lúc nào. Tài khoản sẽ tự động chuyển về gói miễn phí khi hết thời hạn đã thanh toán, không mất phí ẩn."
        },
        {
            question: "Cramer có phù hợp với người mới bắt đầu không?",
            answer: "Có! Cramer phù hợp với mọi trình độ từ beginner đến advanced. Hệ thống sẽ tự động điều chỉnh độ khó dựa trên kết quả bài test đầu vào và tiến trình học tập của bạn."
        },
        {
            question: "Đề thi trên Cramer có giống đề thi thật không?",
            answer: "Các đề thi trên Cramer được biên soạn theo format chuẩn của Cambridge IELTS, với thời gian và cấu trúc giống hệt phòng thi thật, giúp bạn làm quen và tự tin hơn khi bước vào kỳ thi chính thức."
        },
    ];

    const toggleFAQ = (index) => {
        setOpenIndex(openIndex === index ? -1 : index);
    };

    return (
        <section ref={sectionRef} className="faq-section">
            <div className="faq-container">
                {/* Header */}
                <div className={`faq-header ${headerInView ? 'in-view' : ''}`}>
                    <span className="faq-label">Câu hỏi thường gặp</span>
                    <h2 className="faq-title">
                        Giải đáp mọi
                        <br />
                        <span className="text-gradient">thắc mắc của bạn</span>
                    </h2>
                </div>

                {/* FAQ Items */}
                <div className="faq-list">
                    {faqs.map((faq, index) => (
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
                            >
                                <span className="faq-question-icon">
                                    <FaQuestionCircle />
                                </span>
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
                                        animate={{ height: "auto", opacity: 1 }}
                                        exit={{ height: 0, opacity: 0 }}
                                        transition={{ duration: 0.3, ease: "easeInOut" }}
                                    >
                                        <div className="faq-answer">
                                            {faq.answer}
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

export default FAQSection;
