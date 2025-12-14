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
import '../css/PricingPage.css';

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
// FAQ Data (Updated with new terminology)
// =============================================================================

const FAQ_ITEMS = [
  {
    question: `${TERMINOLOGY.ATTEMPT} và ${TERMINOLOGY.ATTEMPT_AI} là gì?`,
    answer: `${TERMINOLOGY.ATTEMPT} là lượt làm bài với chấm điểm tự động cơ bản cho Reading và Listening. ${TERMINOLOGY.ATTEMPT_AI} là lượt làm bài có sự hỗ trợ của AI, bao gồm chấm điểm chi tiết và nhận xét cá nhân hóa cho bài Writing của bạn. Gói Cramerich bao gồm 60 ${TERMINOLOGY.ATTEMPT} và 30 ${TERMINOLOGY.ATTEMPT_AI} mỗi tháng.`,
  },
  {
    question: 'Lúa là gì và dùng để làm gì?',
    answer: `Lúa là tiền ảo trong Cramer, dùng để mua thêm ${TERMINOLOGY.ATTEMPT} hoặc ${TERMINOLOGY.ATTEMPT_AI} khi vượt quá hạn mức hàng tháng. 1 ${TERMINOLOGY.ATTEMPT} thêm = ${ATTEMPT_COSTS.ATTEMPT} Lúa, 1 ${TERMINOLOGY.ATTEMPT_AI} thêm = ${ATTEMPT_COSTS.ATTEMPT_AI} Lúa. Bạn cũng có thể dùng Lúa để sử dụng các tính năng AI khác như dịch từ vựng.`,
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
    answer: 'Hệ thống AI của Cramer (DeepSeek V3.2) phân tích bài viết của bạn dựa trên 4 tiêu chí IELTS: Task Achievement, Coherence & Cohesion, Lexical Resource, và Grammatical Range & Accuracy. Bạn sẽ nhận được điểm band score cho từng tiêu chí cùng nhận xét chi tiết và gợi ý cải thiện.',
  },
  {
    question: 'Gói Cramerie (miễn phí) có thể làm gì?',
    answer: `Gói Cramerie miễn phí cho phép bạn truy cập một số đề thi mẫu, sử dụng Sổ tay từ vựng (${LIMITS.cramerie.maxVocabulary} từ), và Trợ lý Cramer (${LIMITS.cramerie.chatbotMonthly} câu/tháng). Để truy cập toàn bộ kho đề và sử dụng tính năng chấm bài AI, bạn cần nâng cấp lên Cramerich.`,
  },
  {
    question: 'Hạn mức có reset mỗi tháng không?',
    answer: `Có! Mỗi tháng, gói Cramerich của bạn sẽ được reset về: 60 ${TERMINOLOGY.ATTEMPT} (20/kỹ năng) và 30 ${TERMINOLOGY.ATTEMPT_AI} (3/kỹ năng). Bạn cũng nhận thêm 20 Lúa thưởng mỗi tháng.`,
  },
];

// =============================================================================
// Main Component
// =============================================================================

export default function PricingPage() {
  const user = useAuthStore(state => state.user);

  return (
    <div className="pricing-page">
      {/* ===================== HERO SECTION ===================== */}
      <section className="pricing-hero">
        {/* Decorative orbs */}
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
            Nâng cấp lên <span className="text-highlight-gold">Cramerich</span> để 
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

      {/* ===================== TIER COMPARISON (2 columns) ===================== */}
      <section className="pricing-tiers">
        <div className="pricing-container">
          <motion.div
            className="pricing-tiers__grid"
            variants={containerVariants}
            initial="hidden"
            animate="visible"
          >
            {/* Cramerie (Free) */}
            <motion.div variants={itemVariants}>
              <TierCard tierCode={TIERS.CRAMERIE} />
            </motion.div>

            {/* Cramerich (Paid) - Popular */}
            <motion.div variants={itemVariants}>
              <TierCard tierCode={TIERS.CRAMERICH} isPopular />
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* ===================== DEMO COMPARISON ===================== */}
      <section className="demo-comparison">
        <div className="pricing-container">
          <motion.div 
            className="pricing-section__header"
            variants={fadeInUp}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
          >
            <h2 className="pricing-section__title">Trải nghiệm sự khác biệt</h2>
            <p className="pricing-section__subtitle">
              So sánh giữa {TERMINOLOGY.ATTEMPT} và {TERMINOLOGY.ATTEMPT_AI}
            </p>
          </motion.div>

          <motion.div 
            className="demo-comparison__grid"
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: "-50px" }}
          >
            {/* Lượt chấm thường */}
            <motion.div className="demo-card" variants={itemVariants}>
              <div className="demo-card__header demo-card__header--normal">
                <span className="demo-card__badge demo-card__badge--normal">Cơ bản</span>
                <h3 className="demo-card__title">{TERMINOLOGY.ATTEMPT}</h3>
              </div>
              <div className="demo-card__content">
                <div className="demo-card__score-section">
                  <div className="demo-card__score">7.0</div>
                  <span className="demo-card__score-label">Band Score</span>
                </div>
                <ul className="demo-card__features">
                  <li><FiCheck /> Điểm tổng và điểm từng phần</li>
                  <li><FiCheck /> Đáp án đúng/sai cho R/L</li>
                  <li><FiCheck /> Kết quả ngay lập tức</li>
                  <li><FiCheck /> Lưu lịch sử làm bài</li>
                  <li><FiCheck /> Xem giải thích đáp án</li>
                </ul>
                <p className="demo-card__note">
                  Phù hợp cho: Luyện tập nhanh, kiểm tra tiến độ
                </p>
              </div>
            </motion.div>

            {/* Lượt chấm nâng cao */}
            <motion.div className="demo-card demo-card--highlighted" variants={itemVariants}>
              <div className="demo-card__header demo-card__header--ai">
                <span className="demo-card__badge demo-card__badge--ai">AI Powered</span>
                <h3 className="demo-card__title">{TERMINOLOGY.ATTEMPT_AI}</h3>
              </div>
              <div className="demo-card__content">
                <div className="demo-card__score-section">
                  <div className="demo-card__score demo-card__score--ai">7.0</div>
                  <span className="demo-card__score-label">Band Score</span>
                </div>
                <ul className="demo-card__features">
                  <li><FiCheck /> Điểm chi tiết 4 tiêu chí IELTS</li>
                  <li><FiCheck /> Nhận xét cá nhân hóa bằng AI</li>
                  <li><FiCheck /> Phân tích điểm mạnh & điểm yếu</li>
                  <li><FiCheck /> Gợi ý cải thiện cụ thể</li>
                  <li><FiCheck /> Mẫu câu hay & từ vựng nâng cao</li>
                </ul>
                <p className="demo-card__note demo-card__note--highlighted">
                  Phù hợp cho: Writing, cần phản hồi chi tiết để cải thiện
                </p>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* ===================== HOW IT WORKS ===================== */}
      <section className="how-it-works">
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
            className="how-it-works__grid"
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: "-50px" }}
          >
            {/* Card 1: Lượt chấm thường */}
            <motion.div className="how-card" variants={itemVariants}>
              <div className="how-card__icon how-card__icon--green">
                <FiBook />
              </div>
              <h3 className="how-card__title">{TERMINOLOGY.ATTEMPT}</h3>
              <p className="how-card__description">
                Làm bài Reading hoặc Listening với chấm điểm tự động. 
                Nhận điểm số và xem đáp án đúng ngay lập tức.
              </p>
              <div className="how-card__limit">
                <span className="how-card__limit-label">Cramerich:</span>
                <span className="how-card__limit-value">60/tháng (20/kỹ năng)</span>
              </div>
            </motion.div>

            {/* Card 2: Lượt chấm nâng cao */}
            <motion.div className="how-card" variants={itemVariants}>
              <div className="how-card__icon how-card__icon--gold">
                <FiCpu />
              </div>
              <h3 className="how-card__title">{TERMINOLOGY.ATTEMPT_AI}</h3>
              <p className="how-card__description">
                Làm bài Writing và nhận phản hồi chi tiết từ AI. 
                Điểm số theo 4 tiêu chí + nhận xét cá nhân hóa.
              </p>
              <div className="how-card__limit">
                <span className="how-card__limit-label">Cramerich:</span>
                <span className="how-card__limit-value">30/tháng (3/kỹ năng)</span>
              </div>
            </motion.div>

            {/* Card 3: Lúa */}
            <motion.div className="how-card" variants={itemVariants}>
              <div className="how-card__icon how-card__icon--purple">
                🌾
              </div>
              <h3 className="how-card__title">Tiền ảo Lúa</h3>
              <p className="how-card__description">
                Vượt hạn mức? Dùng Lúa để mua thêm lượt chấm. 
                Lúa không hết hạn và có thể mua thêm bất cứ lúc nào.
              </p>
              <div className="how-card__limit">
                <span className="how-card__limit-label">Chi phí:</span>
                <span className="how-card__limit-value">
                  {ATTEMPT_COSTS.ATTEMPT} Lúa/lượt thường, {ATTEMPT_COSTS.ATTEMPT_AI} Lúa/lượt nâng cao
                </span>
              </div>
            </motion.div>

            {/* Card 4: Monthly Reset */}
            <motion.div className="how-card" variants={itemVariants}>
              <div className="how-card__icon how-card__icon--blue">
                <FiZap />
              </div>
              <h3 className="how-card__title">Reset hàng tháng</h3>
              <p className="how-card__description">
                Mỗi tháng, hạn mức của bạn sẽ được reset về mức ban đầu. 
                Cramerich còn nhận thêm 20 Lúa thưởng mỗi tháng!
              </p>
              <div className="how-card__limit">
                <span className="how-card__limit-label">Chu kỳ:</span>
                <span className="how-card__limit-value">Reset vào ngày đầu tháng</span>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* ===================== FEATURE COMPARISON TABLE ===================== */}
      <section className="comparison-section">
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
            className="comparison-table-wrapper"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
          >
            <table className="comparison-table">
              <thead>
                <tr>
                  <th>Tính năng</th>
                  <th>
                    <div className="comparison-table__tier-header">
                      <span className="comparison-table__tier-emoji">🌾</span>
                      <span>Cramerie</span>
                      <span className="comparison-table__tier-price">Miễn phí</span>
                    </div>
                  </th>
                  <th className="comparison-table__popular">
                    <div className="comparison-table__tier-header">
                      <span className="comparison-table__tier-emoji">🌻</span>
                      <span>Cramerich</span>
                      <span className="comparison-table__tier-price">{formatVnd(69000)}/tháng</span>
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                {FEATURE_CATEGORIES.map((category, catIndex) => (
                  <React.Fragment key={catIndex}>
                    <tr className="comparison-table__category">
                      <td colSpan={3}>{category.name}</td>
                    </tr>
                    {category.features.map((feature, featIndex) => (
                      <tr key={`${catIndex}-${featIndex}`}>
                        <td className="comparison-table__feature-name">
                          {feature.name}
                          {feature.tooltip && (
                            <span className="comparison-table__tooltip" title={feature.tooltip}>
                              <FiInfo size={14} />
                            </span>
                          )}
                        </td>
                        <td>{renderFeatureValue(feature.cramerie)}</td>
                        <td className="comparison-table__popular">{renderFeatureValue(feature.cramerich)}</td>
                      </tr>
                    ))}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </motion.div>
        </div>
      </section>

      {/* ===================== FAQ SECTION ===================== */}
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

      {/* ===================== CONTACT SUPPORT ===================== */}
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
// Helper Components
// =============================================================================

function renderFeatureValue(value) {
  if (value === '✅' || value === '✓') {
    return <FiCheck className="comparison-table__check" />;
  }
  if (value === '❌' || value === '✗' || value === '—') {
    return <FiX className="comparison-table__x" />;
  }
  return <span className="comparison-table__text">{value}</span>;
}

/**
 * TierCard - Marketing tier card component (no payment logic)
 */
function TierCard({ tierCode, isPopular }) {
  const tier = TIER_INFO[tierCode];
  const limits = LIMITS[tierCode];
  const user = useAuthStore(state => state.user);
  
  const features = tierCode === TIERS.CRAMERICH ? [
    'Toàn bộ kho đề thi Cambridge (Reading, Listening, Writing)',
    `${limits.monthlyAttempts} ${TERMINOLOGY.ATTEMPT}/tháng (${limits.perSkillAttempts}/kỹ năng)`,
    `${limits.monthlyAttemptAis} ${TERMINOLOGY.ATTEMPT_AI}/tháng (${limits.perSkillAttemptAis}/kỹ năng)`,
    'Chấm bài Writing bằng AI với nhận xét cá nhân hóa',
    `Sổ tay từ vựng: ${formatNumber(limits.maxVocabulary)} từ`,
    `Dịch từ vựng AI: ${limits.dailyTranslations} lần/ngày`,
    `Trợ lý Cramer: ${limits.chatbotMonthly} câu/tháng`,
    `${limits.initialLua} Lúa ban đầu + ${limits.monthlyLuaBonus} Lúa thưởng/tháng`,
  ] : [
    'Một số đề thi mẫu miễn phí',
    `Không có ${TERMINOLOGY.ATTEMPT} miễn phí`,
    `Không có ${TERMINOLOGY.ATTEMPT_AI}`,
    `Sổ tay từ vựng: ${limits.maxVocabulary} từ`,
    `Dịch từ vựng AI: ${limits.dailyTranslations} lần/ngày`,
    `Trợ lý Cramer: ${limits.chatbotMonthly} câu/tháng`,
    `${limits.initialLua} Lúa ban đầu`,
  ];

  // Determine CTA link and text
  const isFree = tier.priceVnd === 0;
  const ctaLink = user 
    ? (isFree ? '/courses' : '/subscription?upgrade=cramerich')
    : '/login';
  const ctaText = user
    ? (isFree ? 'Xem đề thi mẫu' : 'Nâng cấp ngay')
    : (isFree ? 'Bắt đầu miễn phí' : 'Đăng ký & Nâng cấp');

  return (
    <div className={`tier-card ${isPopular ? 'tier-card--popular' : ''}`}>
      {isPopular && <div className="tier-card__badge">Phổ biến nhất</div>}
      
      <div className="tier-card__header">
        <span className="tier-card__emoji">{tier.emoji}</span>
        <h3 className="tier-card__name">{tier.name}</h3>
        <p className="tier-card__description">{tier.description}</p>
      </div>

      <div className="tier-card__price">
        <span className="tier-card__price-amount">
          {tier.priceVnd === 0 ? 'Miễn phí' : formatVnd(tier.priceVnd)}
        </span>
        {tier.priceVnd > 0 && <span className="tier-card__price-period">/tháng</span>}
      </div>

      <ul className="tier-card__features">
        {features.map((feature, index) => {
          const isExcluded = feature.startsWith('Không có');
          return (
            <li key={index} className={`tier-card__feature ${isExcluded ? 'tier-card__feature--excluded' : ''}`}>
              {isExcluded ? (
                <FiX className="tier-card__feature-icon tier-card__feature-icon--x" />
              ) : (
                <FiCheck className="tier-card__feature-icon tier-card__feature-icon--check" />
              )}
              <span>{feature}</span>
            </li>
          );
        })}
      </ul>

      <Link to={ctaLink} className="tier-card__action">
        <button
          className={`tier-card__btn ${isPopular ? 'tier-card__btn--popular' : 'tier-card__btn--upgrade'}`}
        >
          {ctaText}
          <FiArrowRight />
        </button>
      </Link>
    </div>
  );
}
