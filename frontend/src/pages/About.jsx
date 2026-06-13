import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';
import { FaGithub, FaQuoteLeft, FaStar } from 'react-icons/fa';
import {
  FiHeart,
  FiTarget,
  FiUsers,
  FiArrowRight,
  FiEdit3,
  FiCompass,
  FiSend,
  FiRefreshCw,
} from 'react-icons/fi';
import '../css/about.css';

// --- Scroll-triggered animation wrapper (one-shot) ---
const useAnimationInView = (options = {}) => {
  const ref = useRef(null);
  const [isInView, setIsInView] = useState(false);

  useEffect(() => {
    const currentRef = ref.current;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setIsInView(true);
        if (options.triggerOnce) observer.unobserve(entry.target);
      } else if (!options.triggerOnce) {
        setIsInView(false);
      }
    }, { threshold: options.threshold || 0.12, ...options });

    if (currentRef) observer.observe(currentRef);
    return () => { if (currentRef) observer.unobserve(currentRef); };
  }, [options.threshold, options.triggerOnce]);

  return [ref, isInView];
};

const AnimatedItem = ({ children, delay = 0, className = '' }) => {
  const [ref, isInView] = useAnimationInView({ threshold: 0.12, triggerOnce: true });
  return (
    <div
      ref={ref}
      className={`animated-item ${isInView ? 'in-view' : ''} ${className}`}
      style={{ transitionDelay: `${delay}ms` }}
    >
      {children}
    </div>
  );
};

// --- Compact founder card: small circular avatar, horizontal layout ---
const FounderCard = ({ image, name, role, bio, hobby, favorite, github, delay }) => {
  const cardRef = useRef(null);
  const px = useMotionValue(0);
  const py = useMotionValue(0);
  const sx = useSpring(px, { stiffness: 120, damping: 22, mass: 0.5 });
  const sy = useSpring(py, { stiffness: 120, damping: 22, mass: 0.5 });
  const rotateX = useTransform(sy, (v) => v * -3);
  const rotateY = useTransform(sx, (v) => v * 3);

  const handleMove = (e) => {
    const rect = cardRef.current?.getBoundingClientRect();
    if (!rect) return;
    const nx = (e.clientX - rect.left) / rect.width - 0.5;
    const ny = (e.clientY - rect.top) / rect.height - 0.5;
    px.set(nx);
    py.set(ny);
  };

  const handleLeave = () => {
    px.set(0);
    py.set(0);
  };

  return (
    <AnimatedItem delay={delay}>
      <motion.article
        ref={cardRef}
        className="founder-card"
        onMouseMove={handleMove}
        onMouseLeave={handleLeave}
        style={{ rotateX, rotateY, transformPerspective: 1200 }}
      >
        <div className="founder-card__top">
          <div className="founder-card__avatar">
            <img src={image} alt={name} loading="lazy" />
          </div>
          <div className="founder-card__identity">
            <span className="founder-card__role">{role}</span>
            <h3 className="founder-card__name">{name}</h3>
            <a
              href={github.url}
              target="_blank"
              rel="noopener noreferrer"
              className="founder-card__github"
            >
              <FaGithub /> {github.username}
            </a>
          </div>
        </div>

        <div className="founder-card__divider" />

        <p className="founder-card__bio">{bio}</p>

        <div className="founder-card__meta">
          <span className="founder-card__meta-label">Sở thích</span>
          <span className="founder-card__meta-value">{hobby}</span>
        </div>
        <div className="founder-card__meta">
          <span className="founder-card__meta-label">Một điều mình yêu</span>
          <span className="founder-card__meta-value">{favorite}</span>
        </div>
      </motion.article>
    </AnimatedItem>
  );
};

// --- Value card: minimal, single accent, no gradient wash ---
const ValueCard = ({ icon: Icon, index, title, text, delay }) => (
  <AnimatedItem delay={delay}>
    <div className="value-card">
      <div className="value-card__head">
        <span className="value-card__index">0{index}</span>
        <div className="value-card__icon"><Icon /></div>
      </div>
      <h3 className="value-card__title">{title}</h3>
      <p className="value-card__text">{text}</p>
    </div>
  </AnimatedItem>
);

// --- Timeline item ---
const TimelineItem = ({ date, title, description, delay }) => (
  <AnimatedItem delay={delay} className="timeline-item">
    <div className="timeline-dot" />
    <div className="timeline-content">
      <span className="timeline-date">{date}</span>
      <h3 className="timeline-title">{title}</h3>
      <p className="timeline-description">{description}</p>
    </div>
  </AnimatedItem>
);

// --- Process step card ---
const ProcessStep = ({ index, icon: Icon, title, text, delay }) => (
  <AnimatedItem delay={delay}>
    <div className="process-step">
      <div className="process-step__aura" aria-hidden="true" />
      <div className="process-step__head">
        <span className="process-step__index">0{index}</span>
        <div className="process-step__icon"><Icon /></div>
      </div>
      <h3 className="process-step__title">{title}</h3>
      <p className="process-step__text">{text}</p>
    </div>
  </AnimatedItem>
);

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

const getInitials = (name) => name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();

const MarqueeTestimonial = ({ item }) => (
  <article className="about-marquee-card" aria-label={`Đánh giá của ${item.author}`}>
    <FaQuoteLeft className="about-marquee-card__quote-icon" aria-hidden="true" />
    <p className="about-marquee-card__quote">{item.quote}</p>
    <div className="about-marquee-card__rating" aria-hidden="true">
      {Array.from({ length: item.rating }).map((_, i) => (
        <FaStar key={i} className="about-marquee-card__star" />
      ))}
    </div>
    <div className="about-marquee-card__footer">
      <div className="about-marquee-card__avatar" aria-hidden="true">
        {item.avatar ? (
          <img src={item.avatar} alt="" />
        ) : (
          <span>{getInitials(item.author)}</span>
        )}
      </div>
      <div className="about-marquee-card__who">
        <strong>{item.author}</strong>
        <span>{item.role}</span>
      </div>
    </div>
  </article>
);

const AnimatedEnvelope = () => (
  <motion.div
    className="letter-icon"
    aria-hidden="true"
    initial={{ opacity: 0 }}
    whileInView={{ opacity: 1 }}
    viewport={{ once: true, amount: 0.3 }}
    transition={{ duration: 0.6 }}
  >
    {/* Concentric halo rings */}
    <span className="letter-icon__ring letter-icon__ring--3" />
    <span className="letter-icon__ring letter-icon__ring--2" />
    <span className="letter-icon__ring letter-icon__ring--1" />

    {/* Pulse rings */}
    <motion.span
      className="letter-icon__pulse"
      animate={{ scale: [1, 2.1], opacity: [0.5, 0] }}
      transition={{ duration: 3.2, ease: 'easeOut', repeat: Infinity }}
    />
    <motion.span
      className="letter-icon__pulse"
      animate={{ scale: [1, 2.1], opacity: [0.5, 0] }}
      transition={{ duration: 3.2, ease: 'easeOut', repeat: Infinity, delay: 1.6 }}
    />

    {/* Orbiting dots */}
    {[
      { r: 100, dur: 14, size: 6, delay: 0 },
      { r: 130, dur: 18, size: 5, delay: 1 },
      { r: 155, dur: 22, size: 4, delay: 2 },
      { r: 85, dur: 12, size: 5, delay: 0.5 },
    ].map((cfg, i) => (
      <motion.span
        key={`orbit-${i}`}
        className="letter-icon__orbit"
        animate={{ rotate: 360 }}
        transition={{ duration: cfg.dur, ease: 'linear', repeat: Infinity, delay: cfg.delay }}
        style={{
          width: cfg.r * 2,
          height: cfg.r * 2,
          marginTop: -cfg.r,
          marginLeft: -cfg.r,
        }}
      >
        <span
          className="letter-icon__dot"
          style={{ width: cfg.size, height: cfg.size, margin: `-${cfg.size / 2}px 0 0 -${cfg.size / 2}px` }}
        />
      </motion.span>
    ))}

    {/* Sparkles at corners */}
    <motion.span
      className="letter-icon__spark letter-icon__spark--a"
      animate={{ opacity: [0.3, 1, 0.3], scale: [0.8, 1.15, 0.8] }}
      transition={{ duration: 2.4, ease: 'easeInOut', repeat: Infinity }}
    >
      <FiHeart />
    </motion.span>
    <motion.span
      className="letter-icon__spark letter-icon__spark--b"
      animate={{ opacity: [0.3, 1, 0.3], scale: [0.8, 1.15, 0.8] }}
      transition={{ duration: 2.4, ease: 'easeInOut', repeat: Infinity, delay: 0.8 }}
    >
      <FiHeart />
    </motion.span>
    <motion.span
      className="letter-icon__spark letter-icon__spark--c"
      animate={{ opacity: [0.2, 0.9, 0.2], scale: [0.7, 1, 0.7] }}
      transition={{ duration: 2.8, ease: 'easeInOut', repeat: Infinity, delay: 1.2 }}
    >
      <FiHeart />
    </motion.span>
    <motion.span
      className="letter-icon__spark letter-icon__spark--d"
      animate={{ opacity: [0.2, 0.9, 0.2], scale: [0.7, 1, 0.7] }}
      transition={{ duration: 2.8, ease: 'easeInOut', repeat: Infinity, delay: 2 }}
    >
      <FiHeart />
    </motion.span>

    {/* Envelope (gently floats) */}
    <motion.div
      className="letter-icon__envelope"
      animate={{ y: [0, -10, 0], rotate: [-1.5, 1.5, -1.5] }}
      transition={{ duration: 4.2, ease: 'easeInOut', repeat: Infinity }}
    >
      <svg viewBox="0 0 96 80" className="letter-icon__svg">
        {/* Envelope body */}
        <rect
          x="4" y="20" width="88" height="56" rx="6"
          fill="#fff"
          stroke="currentColor"
          strokeWidth="1.75"
        />
        {/* Paper peeking out */}
        <motion.g
          initial={{ y: 6, opacity: 0 }}
          whileInView={{ y: -14, opacity: 1 }}
          viewport={{ once: true, amount: 0.4 }}
          transition={{ delay: 0.55, duration: 0.8, ease: 'easeOut' }}
        >
          <rect
            x="14" y="8" width="68" height="44" rx="3"
            fill="#fbfaff"
            stroke="currentColor"
            strokeWidth="1.5"
          />
          <line x1="22" y1="20" x2="74" y2="20" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" opacity="0.55" />
          <line x1="22" y1="28" x2="66" y2="28" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" opacity="0.4" />
          <line x1="22" y1="36" x2="70" y2="36" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" opacity="0.3" />
          <line x1="22" y1="44" x2="54" y2="44" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" opacity="0.22" />
        </motion.g>
        {/* Envelope flap (opens) */}
        <motion.path
          d="M 4 20 L 48 48 L 92 20"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinejoin="round"
          style={{ transformOrigin: '48px 20px' }}
          initial={{ rotateX: 0 }}
          whileInView={{ rotateX: 170 }}
          viewport={{ once: true, amount: 0.4 }}
          transition={{ delay: 0.15, duration: 0.8, ease: 'easeOut' }}
        />
        {/* Envelope front pocket */}
        <path
          d="M 4 76 L 48 46 L 92 76 Z"
          fill="#fbfaff"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinejoin="round"
        />
        {/* Wax seal */}
        <motion.circle
          cx="48" cy="62" r="7"
          fill="var(--about-accent)"
          stroke="#fff"
          strokeWidth="1.25"
          initial={{ scale: 0, opacity: 0 }}
          whileInView={{ scale: 1, opacity: 1 }}
          viewport={{ once: true, amount: 0.4 }}
          transition={{ delay: 1.05, type: 'spring', stiffness: 260, damping: 14 }}
        />
      </svg>
    </motion.div>
  </motion.div>
);

const AboutPage = () => {
  const navigate = useNavigate();

  const marqueeRowA = useMemo(() => [...testimonials, ...testimonials], []);
  const marqueeRowB = useMemo(() => [...testimonials.slice().reverse(), ...testimonials.slice().reverse()], []);

  return (
    <main className="about-page">
      {/* ---------- HERO ---------- */}
      <section className="about-hero absorb-parent-padding">
        <div className="about-hero__aurora" aria-hidden="true" />
        <div className="container about-hero__container">
          <motion.span
            className="about-hero__eyebrow"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, ease: 'easeOut' }}
          >
            <span className="about-hero__eyebrow-dot" />
            Về Cramer — Chương 01
          </motion.span>

          <motion.h1
            className="about-hero__title"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.08, ease: 'easeOut' }}
          >
            Một nơi luyện IELTS
            <br />
            <em>tử tế</em>, do sinh viên dựng lên.
          </motion.h1>

          <motion.p
            className="about-hero__lede"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2, ease: 'easeOut' }}
          >
            Cramer bắt đầu từ một cuộc trò chuyện đêm muộn giữa hai người bạn cùng mê công nghệ
            và mệt mỏi vì tài liệu luyện thi rời rạc. Chúng mình chán cảnh phải nhảy qua mười mấy
            trang web chỉ để ghép đủ một bài test tử tế, chán cảnh trả tiền cho những khoá học
            bóng bẩy nhưng rỗng tuếch.
          </motion.p>

          <motion.p
            className="about-hero__lede about-hero__lede--muted"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.32, ease: 'easeOut' }}
          >
            Thế là chúng mình ngồi xuống, mở Figma, mở VSCode, và tự hỏi: &ldquo;Nếu mình làm lại
            từ đầu thì sẽ thế nào?&rdquo; Đây chính là câu trả lời — gọn, sạch, và có tâm.
          </motion.p>

          <motion.div
            className="about-hero__byline"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.45, ease: 'easeOut' }}
          >
            <div className="about-hero__byline-stack">
              <img src="/pictures/about/HuynhQuocHuu.png" alt="Huỳnh Quốc Hữu" />
              <img src="/pictures/about/LeMinhKhoa.jpg" alt="Lê Minh Khoa" />
            </div>
            <div className="about-hero__byline-text">
              <span>Viết &amp; duy trì bởi</span>
              <strong>Huỳnh Quốc Hữu &amp; Lê Minh Khoa</strong>
            </div>
          </motion.div>
        </div>
      </section>

      {/* ---------- MANIFESTO ---------- */}
      <section className="about-manifesto">
        <div className="container">
          <AnimatedItem>
            <div className="manifesto-card">
              <div className="manifesto-card__aura" aria-hidden="true" />
              <span className="manifesto-card__eyebrow">Tuyên ngôn</span>
              <FaQuoteLeft className="manifesto-card__quote-icon" aria-hidden="true" />
              <blockquote className="manifesto-card__quote">
                Học ngoại ngữ không phải là một cuộc đua. Nó là một công trình cá nhân —
                được xây bằng thời gian, sự kiên nhẫn, và một chút may mắn. Cramer không
                hứa sẽ rút ngắn công trình đó. Chúng mình chỉ hứa sẽ không làm nó khó hơn
                cần thiết.
              </blockquote>
              <div className="manifesto-card__signature">
                <div className="manifesto-card__sig-line" />
                <div className="manifesto-card__sig-text">
                  <strong>Hữu &amp; Khoa</strong>
                  <span>Đồng sáng lập Cramer</span>
                </div>
              </div>
            </div>
          </AnimatedItem>
        </div>
      </section>

      {/* ---------- VISION ---------- */}
      <section className="about-vision">
        <div className="container">
          <div className="vision-grid">
            <div className="vision-lead">
              <AnimatedItem>
                <span className="section-eyebrow">Tầm nhìn</span>
              </AnimatedItem>
              <AnimatedItem delay={120}>
                <h2 className="vision-heading">
                  Học ngoại ngữ chất lượng <em>không nên</em> là đặc quyền.
                </h2>
              </AnimatedItem>
            </div>

            <div className="vision-body">
              <AnimatedItem delay={200}>
                <p className="vision-paragraph">
                  Chúng mình tin rằng bất kỳ ai — dù ở thành phố lớn hay một ngôi trường huyện —
                  đều xứng đáng có một nền tảng luyện IELTS rõ ràng, tin cậy, và không dối trá.
                  Không phải ai cũng có sẵn vài chục triệu để ghi danh một trung tâm lớn.
                  Nhưng ai cũng có thể dành ra một buổi tối yên tĩnh, một tách trà nóng,
                  và một chút niềm tin vào bản thân.
                </p>
              </AnimatedItem>
              <AnimatedItem delay={320}>
                <p className="vision-paragraph vision-paragraph--muted">
                  Cramer không phải một khoá học đóng gói. Nó là một xưởng đang vận hành: ra đề,
                  chấm bài, phản hồi, rồi lại sửa — mỗi tuần một chút, cùng chính người học.
                  Chúng mình viết từng dòng code không phải để làm ra một thứ &ldquo;gọi vốn được&rdquo;,
                  mà để làm ra một thứ mình tự tin gửi cho người em họ đang luyện IELTS dùng thử.
                </p>
              </AnimatedItem>

              <AnimatedItem delay={440}>
                <ul className="vision-stats">
                  <li>
                    <strong>100%</strong>
                    <span>Tính năng cốt lõi miễn phí</span>
                  </li>
                  <li>
                    <strong>04</strong>
                    <span>Kỹ năng được bao phủ đầy đủ</span>
                  </li>
                  <li>
                    <strong>∞</strong>
                    <span>Số lần luyện tập, không giới hạn</span>
                  </li>
                </ul>
              </AnimatedItem>
            </div>
          </div>
        </div>
      </section>

      {/* ---------- STORY / TIMELINE ---------- */}
      <section className="about-story">
        <div className="container">
          <AnimatedItem>
            <span className="section-eyebrow section-eyebrow--center">Câu chuyện</span>
          </AnimatedItem>
          <AnimatedItem delay={120}>
            <h2 className="section-title">Một hành trình ngắn, nhưng thật.</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="section-subtitle">
              Cramer không xuất phát từ một quỹ đầu tư hay một phòng họp. Nó bắt đầu trên Discord,
              lúc 1 giờ sáng, với hai cốc cà phê đã nguội và một tệp Notion có tên
              &ldquo;ý-tưởng-điên-rồ.md&rdquo;.
            </p>
          </AnimatedItem>

          <div className="timeline">
            <TimelineItem
              date="Tháng 10, 2025"
              title="Ý tưởng nảy sinh"
              description="Trong một buổi tối bình thường trên Discord, Hữu và Khoa than thở về việc khó tìm nguồn luyện IELTS miễn phí mà vẫn chất lượng. Sau vài lần kêu ca, một trong hai đứa nói: 'Hay mình tự làm luôn đi.' Câu nói tưởng đùa ấy hoá ra không đùa."
              delay={200}
            />
            <TimelineItem
              date="Tháng 11, 2025"
              title="Bắt đầu xây dựng"
              description="Khoa cầm bàn phím và gõ những commit đầu tiên. Hữu nghiên cứu cấu trúc đề thi IELTS, soạn kho câu hỏi mẫu, và vẽ wireframe trên Figma lúc nửa đêm. Những đêm làm việc qua voice chat, vừa code vừa nghe nhau than thở bài tập ở trường, hoá ra lại là phần vui nhất."
              delay={360}
            />
            <TimelineItem
              date="Tháng 01, 2026"
              title="Bản alpha đầu tiên"
              description="Cramer chạm tay người học thật. Có bug, có chỗ xấu, có chỗ chậm. Nhưng có một điều làm chúng mình nhớ mãi: một bạn học viên nhắn 'cảm ơn anh, em đã làm được một bài reading trọn vẹn đầu tiên'. Nhiêu đó đủ để tiếp tục."
              delay={520}
            />
            <TimelineItem
              date="Hiện tại"
              title="Không ngừng tinh chỉnh"
              description="Cramer đang lớn lên mỗi tuần. Mỗi tính năng mới đều xuất phát từ một câu hỏi thật của người học thật — rồi chúng mình cố gắng trả lời bằng sản phẩm. Lộ trình không được vẽ trên slide, mà viết trong một file Markdown, cập nhật theo từng buổi cà phê sáng."
              delay={680}
            />
          </div>
        </div>
      </section>

      {/* ---------- FOUNDERS ---------- */}
      <section className="about-founders">
        <div className="container">
          <AnimatedItem>
            <span className="section-eyebrow section-eyebrow--center">Hai đứa mình</span>
          </AnimatedItem>
          <AnimatedItem delay={120}>
            <h2 className="section-title">Đội ngũ đúng hai người — và đủ.</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="section-subtitle">
              Một người viết ý tưởng, một người viết code. Ít người, ít họp, nhiều việc được giao.
              Khi bạn gửi phản hồi, nó đến tay đúng người trong vòng vài phút — vì cả đội chỉ có
              hai người, và không ai trong hai người trốn việc được cả.
            </p>
          </AnimatedItem>

          <div className="founders-grid">
            <FounderCard
              image="/pictures/about/HuynhQuocHuu.png"
              name="Huỳnh Quốc Hữu"
              role="Đồng sáng lập · Product"
              bio="Vạch ra ý tưởng, nội dung và phương pháp học. Hữu tin rằng việc học IELTS có thể vừa nghiêm túc vừa dễ chịu, miễn là sản phẩm đủ gọn gàng. Mỗi bài test trên Cramer đều được Hữu đọc kỹ ít nhất hai lần — một lần với tư cách người ra đề, một lần với tư cách người đi thi."
              hobby="Đọc nghiên cứu, khoa học nhận thức, và tất nhiên — tiếng Anh."
              favorite="Podcast 60 phút và một cuốn sổ tay bìa cứng."
              github={{ url: 'https://github.com/huuunleashed', username: 'huuunleashed' }}
              delay={160}
            />
            <FounderCard
              image="/pictures/about/LeMinhKhoa.jpg"
              name="Lê Minh Khoa"
              role="Đồng sáng lập · Engineering"
              bio="Biến ý tưởng thành phần mềm chạy được. Khoa chăm chút từng chi tiết kỹ thuật để Cramer mượt trên mọi máy, kể cả những chiếc laptop đã bảy năm tuổi của sinh viên. Với Khoa, một tính năng chưa gọi là xong khi nó còn chạy chậm trên máy yếu — vì người cần nó nhất, lắm khi, lại dùng máy yếu nhất."
              hobby="Sáng tác nhạc, tập gym, và nghe nhạc không cùng gu với Hữu."
              favorite="Một buổi sáng Sài Gòn yên tĩnh và bàn phím cơ switch xanh."
              github={{ url: 'https://github.com/leminhkhoa117', username: 'leminhkhoa117' }}
              delay={280}
            />
          </div>
        </div>
      </section>

      {/* ---------- VALUES ---------- */}
      <section className="about-values">
        <div className="container">
          <AnimatedItem>
            <span className="section-eyebrow section-eyebrow--center">Nguyên tắc</span>
          </AnimatedItem>
          <AnimatedItem delay={120}>
            <h2 className="section-title">Ba điều chúng mình không nhân nhượng.</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="section-subtitle">
              Không phải slogan. Đây là bộ lọc mỗi lần chúng mình quyết định thêm hay bỏ một tính năng.
            </p>
          </AnimatedItem>

          <div className="values-grid">
            <ValueCard
              icon={FiTarget}
              index={1}
              title="Chất lượng là trên hết"
              text="Mỗi bài test đều được chọn lọc và xây dựng kỹ lưỡng, bám sát cấu trúc đề thi thật. Không nhồi số lượng, không đăng cho có."
              delay={160}
            />
            <ValueCard
              icon={FiUsers}
              index={2}
              title="Dành cho tất cả"
              text="Ai cũng xứng đáng có một nơi luyện tập tử tế mà không phải đắn đo quá nhiều về chi phí. Paywall có — nhưng chỉ ở những chỗ thật sự hợp lý."
              delay={280}
            />
            <ValueCard
              icon={FiHeart}
              index={3}
              title="Từ người học, cho người học"
              text="Chúng mình cũng đang luyện IELTS. Mọi tính năng đều được thiết kế trước tiên cho chính mình dùng — nếu mình không thích, mình không đăng."
              delay={400}
            />
          </div>
        </div>
      </section>

      {/* ---------- PROCESS ---------- */}
      <section className="about-process">
        <div className="container">
          <AnimatedItem>
            <span className="section-eyebrow section-eyebrow--center">Nhịp làm việc</span>
          </AnimatedItem>
          <AnimatedItem delay={120}>
            <h2 className="section-title">Bốn bước, lặp lại mỗi tuần.</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="section-subtitle">
              Chúng mình không tin vào sprint cứng nhắc. Tuần nào cũng là một chu kỳ nhỏ:
              lắng nghe — chọn việc — làm — gửi đi — rồi lại lắng nghe.
            </p>
          </AnimatedItem>

          <div className="process-grid">
            <ProcessStep
              index={1}
              icon={FiCompass}
              title="Chọn đúng việc"
              text="Đọc phản hồi trên Discord, Zalo, Gmail. Viết ra một danh sách ngắn những vấn đề người học thật sự gặp phải — không đoán, không bịa."
              delay={160}
            />
            <ProcessStep
              index={2}
              icon={FiEdit3}
              title="Phác thảo giải pháp"
              text="Vẽ nhanh trên giấy hoặc Figma. Tính năng chưa bao giờ cần phải hoàn hảo — nó chỉ cần đủ rõ để hai đứa hiểu giống nhau."
              delay={280}
            />
            <ProcessStep
              index={3}
              icon={FiSend}
              title="Gửi bản nhỏ nhất"
              text="Không chờ bản hoàn chỉnh. Cứ có gì dùng được là đẩy lên. Một tính năng đang sống, dù còn rough, vẫn giá trị hơn một bản đẹp nhưng mãi không ra mắt."
              delay={400}
            />
            <ProcessStep
              index={4}
              icon={FiRefreshCw}
              title="Lắng nghe & sửa"
              text="Người học dùng, phản hồi. Chúng mình ghi lại, sắp xếp, và bắt đầu vòng lặp tiếp theo. Không có tính năng nào là xong vĩnh viễn — chỉ có đủ tốt cho tuần này."
              delay={520}
            />
          </div>
        </div>
      </section>

      {/* ---------- TESTIMONIALS (muted marquee) ---------- */}
      <section className="about-testimonials about-testimonials--marquee">
        <div className="container">
          <AnimatedItem>
            <span className="section-eyebrow section-eyebrow--center">Tiếng nói thật</span>
          </AnimatedItem>
          <AnimatedItem delay={120}>
            <h2 className="section-title">Học viên kể lại.</h2>
          </AnimatedItem>
        </div>

        <div className="about-marquee">
          <div className="about-marquee__row about-marquee__row--a">
            <div className="about-marquee__track">
              {marqueeRowA.map((item, i) => (
                <MarqueeTestimonial key={`a-${i}`} item={item} />
              ))}
            </div>
          </div>
          <div className="about-marquee__row about-marquee__row--b">
            <div className="about-marquee__track">
              {marqueeRowB.map((item, i) => (
                <MarqueeTestimonial key={`b-${i}`} item={item} />
              ))}
            </div>
          </div>
          <div className="about-marquee__fade about-marquee__fade--left" aria-hidden="true" />
          <div className="about-marquee__fade about-marquee__fade--right" aria-hidden="true" />
        </div>
      </section>

      {/* ---------- LETTER ---------- */}
      <section className="about-letter">
        <div className="container">
          <AnimatedItem>
            <div className="letter-card">
              <div className="letter-card__aura" aria-hidden="true" />
              <AnimatedEnvelope />
              <span className="letter-card__eyebrow">Thư gửi người đọc</span>
              <h2 className="letter-card__heading">Cảm ơn bạn đã đọc đến đây.</h2>

              <div className="letter-card__body">
                <p>
                  Nếu bạn đọc được đến dòng này, thì bạn đã đi xa hơn 90% người ghé thăm
                  một trang &ldquo;Về chúng tôi&rdquo; — và chúng mình thật sự trân trọng điều đó.
                </p>
                <p>
                  Cramer không phải một startup hào nhoáng. Nó là một dự án sinh viên — nhỏ,
                  chân thật, và mang đầy giới hạn. Sẽ có lúc server chậm vì chúng mình đang
                  deploy. Sẽ có lúc chấm bài chưa hoàn hảo vì AI còn non. Sẽ có những tính năng
                  bạn muốn mà chưa thấy — vì hai đứa mình chỉ có hai mươi tư giờ mỗi ngày.
                </p>
                <p>
                  Nhưng có một điều chúng mình cam kết: mỗi lần bạn gửi một phản hồi, nó sẽ
                  được đọc. Không bởi bot, không bởi &ldquo;team support&rdquo; nào đó —
                  mà bởi chính Hữu hoặc Khoa, thường là trong lúc đang ăn cơm tối.
                </p>
                <p>
                  Cảm ơn vì đã tin một sản phẩm còn đang lớn. Hẹn gặp bạn trong bài test đầu tiên.
                </p>
              </div>

              <div className="letter-card__signature">
                <div className="letter-card__sig-row">
                  <div className="letter-card__sig-block">
                    <span className="letter-card__sig-hand">— Hữu</span>
                    <span className="letter-card__sig-role">Đồng sáng lập · Product</span>
                  </div>
                  <div className="letter-card__sig-block">
                    <span className="letter-card__sig-hand">— Khoa</span>
                    <span className="letter-card__sig-role">Đồng sáng lập · Engineering</span>
                  </div>
                </div>
                <span className="letter-card__sig-place">TP.HCM, một đêm khuya nào đó.</span>
              </div>
            </div>
          </AnimatedItem>
        </div>
      </section>

      {/* ---------- CTA ---------- */}
      <section className="about-cta">
        <div className="container">
          <div className="about-cta__card">
            <AnimatedItem>
              <span className="section-eyebrow">Bắt đầu</span>
            </AnimatedItem>
            <AnimatedItem delay={120}>
              <h2 className="cta-title">Đã sẵn sàng mở một bài test đầu tiên?</h2>
            </AnimatedItem>
            <AnimatedItem delay={220}>
              <p className="cta-text">
                Không cần thẻ tín dụng, không cần cam kết. Chỉ cần một buổi tối rảnh và một tách cà phê.
              </p>
            </AnimatedItem>
            <AnimatedItem delay={320}>
              <div className="cta-actions">
                <button onClick={() => navigate('/dashboard')} className="cta-button">
                  Vào luyện tập <FiArrowRight />
                </button>
              </div>
            </AnimatedItem>
          </div>
        </div>
      </section>
    </main>
  );
};

export default AboutPage;
