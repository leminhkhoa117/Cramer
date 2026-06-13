import React, { useRef } from 'react';
import { motion, useScroll, useTransform } from 'framer-motion';
import {
    FaBookOpen,
    FaRobot,
    FaRoute,
    FaChartLine,
    FaHeadphones,
    FaPen,
} from 'react-icons/fa';

import FeatureVisualHost from './features/FeatureVisualHost';
import { useReducedMotion } from './hooks/useReducedMotion';

/**
 * FeaturesSection — Kinetic 3D zigzag.
 *
 * Each card alternates side and is driven by its own scroll progress
 * via framer-motion's `useScroll` so the card rotates / lifts into place
 * as it enters the viewport. No expensive watchers per card; framer's
 * motion values bypass React renders.
 */

const features = [
    {
        icon: FaBookOpen,
        chip: '01 · Kho đề',
        eyebrow: 'Nhiều bộ đề và kỹ năng để bạn khám phá',
        title: 'Kho đề phong phú',
        description: 'Tìm đề nhanh chóng, nhiều tuỳ chọn thi thử, nhanh gọn và không rườm rà.',
        highlights: ['Tìm đề thông minh', 'Full kỹ năng', 'Thao tác nhanh gọn'],
        gradient: 'gradient-purple',
        visual: 'library',
    },
    {
        icon: FaRoute,
        chip: '02 · Thi thử',
        eyebrow: 'Trải nghiệm thi máy xứng tầm',
        title: 'Mô phỏng thi thật',
        description: 'Giao diện sát thi thật, dễ sử dụng với nhiều cải tiến, hỗ trợ lưu trạng thái bài làm.',
        highlights: ['Giao diện linh hoạt', 'Chuyển câu nhanh', 'Tính năng bổ trợ'],
        gradient: 'gradient-blue',
        visual: 'test-simulator',
    },
    {
        icon: FaRobot,
        chip: '03 · AI chấm',
        eyebrow: 'Hỗ trợ chấm bài nâng cao bằng AI',
        title: 'Chấm bài bằng AI',
        description: 'Nhiều loại đề thi, có kết quả nhanh chóng, feedback siêu chi tiết sau khi nộp bài.',
        highlights: ['Chấm chữa chi tiết', 'Nhận xét chính xác', 'Thấy điểm cần cải thiện'],
        gradient: 'gradient-teal',
        visual: 'ai-evaluation',
    },
    {
        icon: FaHeadphones,
        chip: '04 · Speaking',
        eyebrow: 'Luyện Speaking với giám khảo AI (Beta)',
        title: 'Thi Speaking như thật',
        description: 'Chế độ và topic ngẫu nhiên, trải nghiệm thi giống thật, phân tích giọng nói nâng cao.',
        highlights: ['Phản hồi thời gian thực', 'Transcribe phần đã nói', 'Giao diện tập làm quen'],
        gradient: 'gradient-orange',
        visual: 'speaking',
    },
    {
        icon: FaChartLine,
        chip: '05 · Dashboard',
        eyebrow: 'Theo dõi tiến độ với gợi ý cá nhân hoá',
        title: 'Bảng điều khiển trực quan',
        description: 'Theo dõi lịch sử làm bài với nhiều công cụ trực quan hoá kết hợp phân tích từ AI.',
        highlights: ['Xem lại bài làm', 'Cá nhân hoá dashboard', 'Đánh giá liên tục'],
        gradient: 'gradient-purple',
        visual: 'dashboard',
    },
    {
        icon: FaPen,
        chip: '06 · Hỗ trợ',
        eyebrow: 'Sổ tay từ vựng và trợ lý Cramer',
        title: 'Nhiều tính năng hỗ trợ',
        description: 'Lưu từ mới ở bất cứ đâu, trợ lý ảo cá nhân, dễ dàng truy vấn không giới hạn.',
        highlights: ['Lọc từ vựng đã thuộc', 'Highlight tra nhanh', 'Truy vấn không giới hạn'],
        gradient: 'gradient-blue',
        visual: 'support',
    },
];

const ZigzagCard = ({ feature, index, reduced }) => {
    const Icon = feature.icon;
    const isLeft = index % 2 === 0;
    const cardRef = useRef(null);

    // Each card has its own scroll progress range based on its own position.
    // Output drives a small 3D tilt + Y translate to feel "magnetic."
    const { scrollYProgress } = useScroll({
        target: cardRef,
        offset: ['start 90%', 'end 10%'],
    });

    const rotateY = useTransform(
        scrollYProgress,
        [0, 0.5, 1],
        reduced ? [0, 0, 0] : [isLeft ? 12 : -12, 0, isLeft ? -8 : 8]
    );
    const rotateX = useTransform(
        scrollYProgress,
        [0, 0.5, 1],
        reduced ? [0, 0, 0] : [8, 0, -6]
    );
    const translateY = useTransform(
        scrollYProgress,
        [0, 0.5, 1],
        reduced ? [0, 0, 0] : [40, 0, -30]
    );
    const opacity = useTransform(scrollYProgress, [0, 0.18, 0.85, 1], [0.4, 1, 1, 0.85]);

    return (
        <motion.article
            ref={cardRef}
            className={`zigzag-card zigzag-card--${isLeft ? 'left' : 'right'} ${feature.gradient}`}
            style={{
                rotateY,
                rotateX,
                y: translateY,
                opacity,
            }}
        >
            <div className="zigzag-card__ribbon" aria-hidden="true" />
            <span className="zigzag-card__index" aria-hidden="true">
                {String(index + 1).padStart(2, '0')}
            </span>
            <span className="zigzag-card__chip">{feature.chip}</span>

            <div className="zigzag-card__copy">
                <div className={`zigzag-card__icon ${feature.gradient}`}>
                    <Icon />
                </div>
                <span className="zigzag-card__eyebrow">{feature.eyebrow}</span>
                <h3 className="zigzag-card__title">{feature.title}</h3>
                <p className="zigzag-card__description">{feature.description}</p>
                <ul className="zigzag-card__highlights">
                    {feature.highlights.map((h) => (
                        <li key={h}>{h}</li>
                    ))}
                </ul>
            </div>

            <div className="zigzag-card__visual">
                <FeatureVisualHost visual={feature.visual} />
            </div>
        </motion.article>
    );
};

const FeaturesSection = () => {
    const reduced = useReducedMotion();

    return (
        <section className="features-section features-section--zigzag3d">
            <div className="features-container">
                <motion.div
                    className="features-header in-view"
                    initial={{ opacity: 0, y: 30 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true, amount: 0.3 }}
                    transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
                >
                    <span className="features-label">Tính năng nổi bật</span>
                    <h2 className="features-title">
                        Tất cả những gì bạn cần
                        <br />
                        <span className="text-gradient">để chinh phục IELTS</span>
                    </h2>
                    <p className="features-subtitle">
                        Cramer tích hợp đầy đủ công cụ và tài nguyên để giúp bạn đạt band điểm mơ ước.
                    </p>
                </motion.div>

                <div className="zigzag-track">
                    {features.map((feature, index) => (
                        <ZigzagCard
                            key={feature.title}
                            feature={feature}
                            index={index}
                            reduced={reduced}
                        />
                    ))}
                </div>
            </div>
        </section>
    );
};

export default FeaturesSection;