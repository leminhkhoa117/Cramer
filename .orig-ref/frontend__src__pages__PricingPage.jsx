import React from 'react';
import { motion } from 'framer-motion';
import {
  FiCheck,
  FiX,
  FiArrowRight,
  FiMessageCircle,
  FiInfo,
  FiAward,
  FiBook,
  FiCpu,
  FiZap
} from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../stores';
import {
  TIERS,
  TIER_INFO,
  FEATURE_CATEGORIES,
  ATTEMPT_COSTS,
  LIMITS,
  TERMINOLOGY,
  formatVnd,
  formatNumber
} from '../constants/subscription';
import FAQ from '../components/common/FAQ';
import InteractiveGradingDemo from '../components/pricing/InteractiveGradingDemo';
import '../css/pricing-page.css';

// =============================================================================
// Animation Variants
// =============================================================================

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.1 }
  }
};

const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: { y: 0, opacity: 1, transition: { duration: 0.4 } }
};

const fadeInUp = {
  hidden: { y: 30, opacity: 0 },
  visible: { y: 0, opacity: 1, transition: { duration: 0.6 } }
};

// =============================================================================
// FAQ Data
// =============================================================================

const FAQ_ITEMS = [
  {
    question: `${TERMINOLOGY.ATTEMPT} và ${TERMINOLOGY.ATTEMPT_AI} là gì?`,
    answer: `${TERMINOLOGY.ATTEMPT} là lượt làm bài với chấm điểm tự động cơ bản cho Reading và Listening. ${TERMINOLOGY.ATTEMPT_AI} là lượt làm bài có sự hỗ trợ của AI, bao gồm chấm điểm chi tiết và nhận xét cá nhân hóa cho bài Writing của bạn. Gói Cramerich bao gồm ${LIMITS.cramerich.monthlyAttempts} ${TERMINOLOGY.ATTEMPT} và ${LIMITS.cramerich.monthlyAttemptAis} ${TERMINOLOGY.ATTEMPT_AI} mỗi tháng.`,
  },
  {
    question: 'Lúa là gì và dùng để làm gì?',
    answer: `Lúa là tiền ảo trong Cramer, dùng để mua thêm ${TERMINOLOGY.ATTEMPT} hoặc ${TERMINOLOGY.ATTEMPT_AI} khi vượt quá hạn mức hàng tháng. 1 ${TERMINOLOGY.ATTEMPT} thêm = ${ATTEMPT_COSTS.ATTEMPT} Lúa, 1 ${TERMINOLOGY.ATTEMPT_AI} thêm = ${ATTEMPT_COSTS.ATTEMPT_AI} Lúa. Bạn cũng có thể dùng Lúa để dịch từ vựng (1 Lúa/lần) hoặc hỏi chatbot (2 Lúa/lần) khi vượt hạn mức.`,
  },
  {
    question: 'Tôi có thể hủy đăng ký không?',
    answer: 'Có, bạn có thể hủy đăng ký bất cứ lúc nào. Sau khi hủy, bạn vẫn có thể sử dụng các tính năng trả phí cho đến hết chu kỳ thanh toán hiện tại. Lúa đã mua sẽ được giữ lại trong tài khoản.',
  },
  {
    question: 'Lúa có hết hạn không?',
    answer: 'Không, Lúa không có thời hạn sử dụng. Bạn có thể tích lũy và sử dụng bất cứ khi nào cần.',
  },
  {
    question: 'Chấm bài Writing bằng AI hoạt động như thế nào?',
    answer: 'Hệ thống AI của Cramer (DeepSeek V3) phân tích bài viết của bạn dựa trên 4 tiêu chí IELTS: Task Achievement, Coherence & Cohesion, Lexical Resource, và Grammatical Range & Accuracy. Bạn sẽ nhận được điểm band score cho từng tiêu chí cùng nhận xét chi tiết và gợi ý cải thiện.',
  },
  {
    question: 'Gói Cramerie (miễn phí) có thể làm gì?',
    answer: `Gói Cramerie miễn phí cho phép bạn có ${LIMITS.cramerie.monthlyAttempts} ${TERMINOLOGY.ATTEMPT} và ${LIMITS.cramerie.monthlyAttemptAis} ${TERMINOLOGY.ATTEMPT_AI}/tháng, sử dụng Sổ tay từ vựng (${formatNumber(LIMITS.cramerie.maxVocabulary)} từ), dịch từ vựng AI (${LIMITS.cramerie.monthlyTranslations} lần/tháng), và Trợ lý Cramer (${LIMITS.cramerie.chatbotMonthly} câu/tháng). Khi vượt hạn mức, bạn có thể dùng Lúa để tiếp tục.`,
  },
  {
    question: 'Hạn mức có reset mỗi tháng không?',
    answer: `Có! Mỗi tháng, hạn mức của bạn sẽ được reset về: ${LIMITS.cramerich.monthlyAttempts} ${TERMINOLOGY.ATTEMPT} và ${LIMITS.cramerich.monthlyAttemptAis} ${TERMINOLOGY.ATTEMPT_AI} cho Cramerich (${LIMITS.cramerie.monthlyAttempts}/${LIMITS.cramerie.monthlyAttemptAis} cho Cramerie). Cramerich còn nhận thêm 20 Lúa thưởng mỗi tháng.`,
  },
];

// =============================================================================
// Main Component
// =============================================================================

export default function PricingPage() {
  const user = useAuthStore(state => state.user);

  return (
    <div className="pricing-page">
      {/* ===================== HERO ===================== */}
      <section className="pricing-hero">
        <div className="pricing-hero__orb pricing-hero__orb--1" />
        <div className="pricing-hero__orb pricing-hero__orb--2" />
        <div className="pricing-hero__orb pricing-hero__orb--3" />

        <div className="pricing-hero__content">
          <motion.span
            className="pricing-hero__badge"
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.4 }}
          >
            🎯 Luyện thi IELTS thông minh
          </motion.span>

          <motion.h1
            className="pricing-hero__title"
            initial={{ y: -20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.1 }}
          >
            Chọn gói phù hợp với bạn
          </motion.h1>

          <motion.p
            className="pricing-hero__subtitle"
            initial={{ y: -10, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            Nâng cấp lên{' '}
            <span className="pricing-hero__highlight">Cramerich</span> để
            truy cập toàn bộ kho đề thi Cambridge và tính năng chấm bài AI cá nhân hóa
          </motion.p>

          <motion.div
            className="pricing-hero__ctas"
            initial={{ y: 10, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            {user ? (
              <Link to="/subscription" className="pricing-hero__btn pricing-hero__btn--primary">
                Xem gói của tôi
                <FiArrowRight />
              </Link>
            ) : (
              <>
                <Link to="/login" className="pricing-hero__btn pricing-hero__btn--primary">
                  Bắt đầu miễn phí
                  <FiArrowRight />
                </Link>
                <Link to="/login" className="pricing-hero__btn pricing-hero__btn--secondary">
                  Đăng nhập
                </Link>
              </>
            )}
          </motion.div>
        </div>
      </section>

      {/* ===================== TIER CARDS ===================== */}
      <section className="pricing-tiers">
        <div className="pricing-container">
          <motion.div
            className="pricing-tiers__grid"
            variants={containerVariants}
            initial="hidden"
            animate="visible"
          >
            <motion.div variants={itemVariants}>
              <TierCard tierCode={TIERS.CRAMERIE} />
            </motion.div>
            <motion.div variants={itemVariants}>
              <TierCard tierCode={TIERS.CRAMERICH} isPopular />
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* ===================== AI GRADING DEMO ===================== */}
      <InteractiveGradingDemo />

      {/* ===================== HOW IT WORKS ===================== */}
      <section className="pricing-how">
        <div className="pricing-container">
          <motion.div
            className="pricing-section__header"
            variants={fadeInUp}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
          >
            <h2 className="pricing-section__title">Hệ thống Lượt chấm hoạt động như thế nào?</h2>
            <p className="pricing-section__subtitle">
              Hiểu rõ cách sử dụng {TERMINOLOGY.ATTEMPT} và {TERMINOLOGY.ATTEMPT_AI}
            </p>
          </motion.div>

          <motion.div
            className="pricing-how__grid"
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: "-50px" }}
          >
            <motion.div className="pricing-how__card" variants={itemVariants}>
              <div className="pricing-how__card-icon pricing-how__card-icon--green">
                <FiBook />
              </div>
              <h3 className="pricing-how__card-title">{TERMINOLOGY.ATTEMPT}</h3>
              <p className="pricing-how__card-desc">
                Làm bài Reading hoặc Listening với chấm điểm tự động.
                Nhận điểm số và xem đáp án đúng ngay lập tức.
              </p>
              <div className="pricing-how__card-limit">
                <span className="pricing-how__card-limit-label">Cramerie:</span>
                <span className="pricing-how__card-limit-value">{LIMITS.cramerie.monthlyAttempts}/tháng</span>
              </div>
              <div className="pricing-how__card-limit">
                <span className="pricing-how__card-limit-label">Cramerich:</span>
                <span className="pricing-how__card-limit-value">{LIMITS.cramerich.monthlyAttempts}/tháng</span>
              </div>
            </motion.div>

            <motion.div className="pricing-how__card" variants={itemVariants}>
              <div className="pricing-how__card-icon pricing-how__card-icon--gold">
                <FiCpu />
              </div>
              <h3 className="pricing-how__card-title">{TERMINOLOGY.ATTEMPT_AI}</h3>
              <p className="pricing-how__card-desc">
                Làm bài Writing và nhận phản hồi chi tiết từ AI.
                Điểm số theo 4 tiêu chí + nhận xét cá nhân hóa.
              </p>
              <div className="pricing-how__card-limit">
                <span className="pricing-how__card-limit-label">Cramerie:</span>
                <span className="pricing-how__card-limit-value">{LIMITS.cramerie.monthlyAttemptAis}/tháng</span>
              </div>
              <div className="pricing-how__card-limit">
                <span className="pricing-how__card-limit-label">Cramerich:</span>
                <span className="pricing-how__card-limit-value">{LIMITS.cramerich.monthlyAttemptAis}/tháng</span>
              </div>
            </motion.div>

            <motion.div className="pricing-how__card" variants={itemVariants}>
              <div className="pricing-how__card-icon pricing-how__card-icon--purple">
                🌾
              </div>
              <h3 className="pricing-how__card-title">Tiền ảo Lúa</h3>
              <p className="pricing-how__card-desc">
                Vượt hạn mức? Dùng Lúa để mua thêm lượt chấm.
                Lúa không hết hạn và có thể mua thêm bất cứ lúc nào.
              </p>
              <div className="pricing-how__card-limit">
                <span className="pricing-how__card-limit-label">Chi phí:</span>
                <span className="pricing-how__card-limit-value">
                  {ATTEMPT_COSTS.ATTEMPT} Lúa/lượt thường, {ATTEMPT_COSTS.ATTEMPT_AI} Lúa/lượt nâng cao
                </span>
              </div>
            </motion.div>

            <motion.div className="pricing-how__card" variants={itemVariants}>
              <div className="pricing-how__card-icon pricing-how__card-icon--blue">
                <FiZap />
              </div>
              <h3 className="pricing-how__card-title">Reset hàng tháng</h3>
              <p className="pricing-how__card-desc">
                Mỗi tháng, hạn mức của bạn sẽ được reset về mức ban đầu.
                Cramerich còn nhận thêm 20 Lúa thưởng mỗi tháng!
              </p>
              <div className="pricing-how__card-limit">
                <span className="pricing-how__card-limit-label">Chu kỳ:</span>
                <span className="pricing-how__card-limit-value">Reset vào ngày đầu tháng</span>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* ===================== FEATURE COMPARISON TABLE ===================== */}
      <section className="pricing-compare">
        <div className="pricing-container">
          <motion.div
            className="pricing-section__header"
            variants={fadeInUp}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
          >
            <h2 className="pricing-section__title">So sánh chi tiết</h2>
            <p className="pricing-section__subtitle">
              Xem chi tiết những gì bạn nhận được với từng gói
            </p>
          </motion.div>

          <motion.div
            className="pricing-compare__wrapper"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
          >
            <table className="pricing-compare__table">
              <thead>
                <tr>
                  <th>Tính năng</th>
                  <th>
                    <div className="pricing-compare__tier-header">
                      <span className="pricing-compare__tier-emoji">🌾</span>
                      <span>Cramerie</span>
                      <span className="pricing-compare__tier-price">Miễn phí</span>
                    </div>
                  </th>
                  <th className="pricing-compare__popular">
                    <div className="pricing-compare__tier-header">
                      <span className="pricing-compare__tier-emoji">🌻</span>
                      <span>Cramerich</span>
                      <span className="pricing-compare__tier-price">{formatVnd(69000)}/tháng</span>
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                {FEATURE_CATEGORIES.map((category, catIndex) => (
                  <React.Fragment key={catIndex}>
                    <tr className="pricing-compare__category">
                      <td colSpan={3}>{category.name}</td>
                    </tr>
                    {category.features.map((feature, featIndex) => (
                      <tr key={`${catIndex}-${featIndex}`}>
                        <td className="pricing-compare__feature-name">
                          {feature.name}
                          {feature.tooltip && (
                            <span className="pricing-compare__tooltip" title={feature.tooltip}>
                              <FiInfo size={14} />
                            </span>
                          )}
                        </td>
                        <td>{renderFeatureValue(feature.cramerie)}</td>
                        <td className="pricing-compare__popular">{renderFeatureValue(feature.cramerich)}</td>
                      </tr>
                    ))}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </motion.div>
        </div>
      </section>

      {/* ===================== FAQ ===================== */}
      <FAQ
        items={FAQ_ITEMS}
        label="Câu hỏi thường gặp"
        title={<>Giải đáp mọi<br /><span className="text-gradient">thắc mắc của bạn</span></>}
        variant="default"
        defaultOpenIndex={0}
        className="pricing-faq"
      />

      {/* ===================== CTA BANNER ===================== */}
      <section className="pricing-container">
        <motion.div
          className="pricing-cta"
          initial={{ opacity: 0, scale: 0.95 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5 }}
        >
          <div className="pricing-cta__content">
            <FiAward className="pricing-cta__icon" />
            <h2 className="pricing-cta__title">Sẵn sàng chinh phục IELTS?</h2>
            <p className="pricing-cta__text">
              Nâng cấp lên Cramerich để trải nghiệm đầy đủ kho đề thi và tính năng chấm bài AI
            </p>

            {user ? (
              <Link to="/subscription" className="pricing-cta__btn">
                Quản lý gói đăng ký
                <FiArrowRight />
              </Link>
            ) : (
              <div className="pricing-cta__btns">
                <Link to="/login" className="pricing-cta__btn">
                  Đăng ký miễn phí
                  <FiArrowRight />
                </Link>
                <Link to="/login" className="pricing-cta__btn pricing-cta__btn--secondary">
                  Đăng nhập
                </Link>
              </div>
            )}
          </div>
        </motion.div>
      </section>

      {/* ===================== SUPPORT ===================== */}
      <section className="pricing-support">
        <div className="pricing-container">
          <p className="pricing-support__text">
            <FiMessageCircle />
            Cần hỗ trợ? Liên hệ{' '}
            <a href="mailto:support@cramer.vn" className="pricing-support__link">
              support@cramer.vn
            </a>
          </p>
        </div>
      </section>
    </div>
  );
}

// =============================================================================
// Helpers
// =============================================================================

function renderFeatureValue(value) {
  if (value === '✅' || value === '✓') {
    return <FiCheck className="pricing-compare__check" />;
  }
  if (value === '❌' || value === '✗' || value === '—') {
    return <FiX className="pricing-compare__x" />;
  }
  return <span className="pricing-compare__text">{value}</span>;
}

function TierCard({ tierCode, isPopular }) {
  const tier = TIER_INFO[tierCode];
  const limits = LIMITS[tierCode];
  const user = useAuthStore(state => state.user);

  const features = tierCode === TIERS.CRAMERICH ? [
    'Toàn bộ kho đề thi Cambridge (Reading, Listening, Writing)',
    `${limits.monthlyAttempts} ${TERMINOLOGY.ATTEMPT}/tháng`,
    `${limits.monthlyAttemptAis} ${TERMINOLOGY.ATTEMPT_AI}/tháng`,
    'Chấm bài Writing bằng AI với nhận xét cá nhân hóa',
    `Sổ tay từ vựng: ${formatNumber(limits.maxVocabulary)} từ`,
    `Dịch từ vựng AI: ${limits.monthlyTranslations} lần/tháng`,
    `Trợ lý Cramer: ${limits.chatbotMonthly} câu/tháng`,
    `${limits.initialLua} Lúa ban đầu + ${limits.monthlyLuaBonus} Lúa thưởng/tháng`,
  ] : [
    'Một số đề thi mẫu miễn phí',
    `${limits.monthlyAttempts} ${TERMINOLOGY.ATTEMPT}/tháng`,
    `${limits.monthlyAttemptAis} ${TERMINOLOGY.ATTEMPT_AI}/tháng`,
    `Sổ tay từ vựng: ${formatNumber(limits.maxVocabulary)} từ`,
    `Dịch từ vựng AI: ${limits.monthlyTranslations} lần/tháng`,
    `Trợ lý Cramer: ${limits.chatbotMonthly} câu/tháng`,
    `${limits.initialLua} Lúa ban đầu`,
  ];

  const isFree = tier.priceVnd === 0;
  const ctaLink = user
    ? (isFree ? '/courses' : '/subscription?upgrade=cramerich')
    : '/login';
  const ctaText = user
    ? (isFree ? 'Xem đề thi mẫu' : 'Nâng cấp ngay')
    : (isFree ? 'Bắt đầu miễn phí' : 'Đăng ký & Nâng cấp');

  return (
    <div className={`pricing-card ${isPopular ? 'pricing-card--popular' : ''}`}>
      {isPopular && <div className="pricing-card__badge">Phổ biến nhất</div>}

      <div className="pricing-card__header">
        <span className="pricing-card__emoji">{tier.emoji}</span>
        <h3 className="pricing-card__name">{tier.name}</h3>
        <p className="pricing-card__desc">{tier.description}</p>
      </div>

      <div className="pricing-card__price">
        <span className="pricing-card__price-amount">
          {tier.priceVnd === 0 ? 'Miễn phí' : formatVnd(tier.priceVnd)}
        </span>
        {tier.priceVnd > 0 && <span className="pricing-card__price-period">/tháng</span>}
      </div>

      <ul className="pricing-card__features">
        {features.map((feature, index) => {
          const isExcluded = feature.startsWith('Không có');
          return (
            <li key={index} className={`pricing-card__feature ${isExcluded ? 'pricing-card__feature--excluded' : ''}`}>
              {isExcluded ? (
                <FiX className="pricing-card__feature-icon pricing-card__feature-icon--x" />
              ) : (
                <FiCheck className="pricing-card__feature-icon pricing-card__feature-icon--check" />
              )}
              <span>{feature}</span>
            </li>
          );
        })}
      </ul>

      <Link to={ctaLink} className="pricing-card__action">
        <button
          className={`pricing-card__btn ${isPopular ? 'pricing-card__btn--primary' : 'pricing-card__btn--secondary'}`}
        >
          {ctaText}
          <FiArrowRight />
        </button>
      </Link>
    </div>
  );
}