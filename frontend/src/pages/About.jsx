import React, { useState, useEffect, useRef, Suspense, lazy } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { FaGithub } from 'react-icons/fa';
import { 
  FiHeart, 
  FiTarget, 
  FiUsers, 
  FiChevronDown, 
  FiArrowRight,
  FiZap,
  FiAward,
  FiBookOpen
} from 'react-icons/fi';
import { useInView, useSectionInView } from '../hooks/useInView';
import '../css/About.css';

// Lazy load 3D scene for performance
const Scene3DAbout = lazy(() => import('../components/3d/Scene3DAbout'));

// --- Custom Hook for Scroll Animation (for AnimatedItem - triggerOnce) ---
const useAnimationInView = (options = {}) => {
  const ref = useRef(null);
  const [isInView, setIsInView] = useState(false);

  useEffect(() => {
    const currentRef = ref.current;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setIsInView(true);
        if (options.triggerOnce) {
          observer.unobserve(entry.target);
        }
      } else if (!options.triggerOnce) {
        setIsInView(false);
      }
    }, { threshold: options.threshold || 0.1, ...options });

    if (currentRef) {
      observer.observe(currentRef);
    }

    return () => {
      if (currentRef) {
        observer.unobserve(currentRef);
      }
    };
  }, [options.threshold, options.triggerOnce]);

  return [ref, isInView];
};

// --- Animated Component Wrapper ---
const AnimatedItem = ({ children, delay = 0, className = '' }) => {
  const [ref, isInView] = useAnimationInView({ threshold: 0.1, triggerOnce: true });
  
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

// --- Founder Card with 3D Parallax ---
const FounderCard = ({ image, name, role, bio, hobby, github, delay }) => {
  const cardRef = useRef(null);
  const [transform, setTransform] = useState('');
  const [glareStyle, setGlareStyle] = useState({});
  const [isHovering, setIsHovering] = useState(false);
  const currentRotationRef = useRef({ x: 0, y: 0 });
  const animationFrameRef = useRef(null);

  const handleMouseMove = (e) => {
    if (!cardRef.current) return;
    
    setIsHovering(true);
    
    const rect = cardRef.current.getBoundingClientRect();
    const mouseX = Math.max(0, Math.min(rect.width, e.clientX - rect.left)) - rect.width / 2;
    const mouseY = Math.max(0, Math.min(rect.height, e.clientY - rect.top)) - rect.height / 2;
    
    const targetRotateX = (mouseY / (rect.height / 2)) * -12;
    const targetRotateY = (mouseX / (rect.width / 2)) * 12;
    
    // Cancel previous animation frame
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
    }
    
    // Smooth interpolation
    animationFrameRef.current = requestAnimationFrame(() => {
      const lerpFactor = 0.15;
      const smoothRotateX = currentRotationRef.current.x + (targetRotateX - currentRotationRef.current.x) * lerpFactor;
      const smoothRotateY = currentRotationRef.current.y + (targetRotateY - currentRotationRef.current.y) * lerpFactor;
      
      currentRotationRef.current = { x: smoothRotateX, y: smoothRotateY };
      
      const rotateXVal = Math.max(-12, Math.min(12, smoothRotateX));
      const rotateYVal = Math.max(-12, Math.min(12, smoothRotateY));
      
      setTransform(`perspective(1000px) rotateX(${rotateXVal}deg) rotateY(${rotateYVal}deg) translateZ(15px) scale(1.02)`);
    });
    
    const glareX = ((Math.max(0, Math.min(rect.width, e.clientX - rect.left)) / rect.width) * 100);
    const glareY = ((Math.max(0, Math.min(rect.height, e.clientY - rect.top)) / rect.height) * 100);
    setGlareStyle({
      background: `radial-gradient(circle at ${glareX}% ${glareY}%, rgba(255,255,255,0.15) 0%, transparent 50%)`,
    });
  };

  const handleMouseLeave = () => {
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
    }
    currentRotationRef.current = { x: 0, y: 0 };
    setTransform('perspective(1000px) rotateX(0deg) rotateY(0deg) translateZ(0px) scale(1)');
    setGlareStyle({});
    setIsHovering(false);
  };

  useEffect(() => {
    return () => {
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, []);

  return (
    <AnimatedItem delay={delay}>
      <div
        ref={cardRef}
        className={`founder-card ${isHovering ? 'is-hovering' : ''}`}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        style={{ transform: transform || undefined }}
      >
        <div className="founder-card-border" />
        <div className="founder-card-inner">
          <div className="founder-card-glare" style={glareStyle} />
          <div className="founder-card__image-wrapper">
            <img src={image} alt={name} className="founder-card__image" loading="lazy" />
          </div>
          <div className="founder-card__content">
            <h3 className="founder-card__name">{name}</h3>
            <span className="founder-card__role">{role}</span>
            <p className="founder-card__bio">{bio}</p>
            <p className="founder-card__hobby"><strong>Sở thích:</strong> {hobby}</p>
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
      </div>
    </AnimatedItem>
  );
};

// --- Value Card with 3D Parallax ---
const ValueCard = ({ icon: Icon, title, text, delay }) => {
  const cardRef = useRef(null);
  const [transform, setTransform] = useState('');
  const [glareStyle, setGlareStyle] = useState({});

  const handleMouseMove = (e) => {
    if (!cardRef.current) return;
    
    const rect = cardRef.current.getBoundingClientRect();
    const mouseX = Math.max(0, Math.min(rect.width, e.clientX - rect.left)) - rect.width / 2;
    const mouseY = Math.max(0, Math.min(rect.height, e.clientY - rect.top)) - rect.height / 2;
    
    const rotateXVal = Math.max(-15, Math.min(15, (mouseY / (rect.height / 2)) * -15));
    const rotateYVal = Math.max(-15, Math.min(15, (mouseX / (rect.width / 2)) * 15));
    
    setTransform(`perspective(1000px) rotateX(${rotateXVal}deg) rotateY(${rotateYVal}deg) translateZ(20px) scale(1.03)`);
    
    const glareX = ((Math.max(0, Math.min(rect.width, e.clientX - rect.left)) / rect.width) * 100);
    const glareY = ((Math.max(0, Math.min(rect.height, e.clientY - rect.top)) / rect.height) * 100);
    setGlareStyle({
      background: `radial-gradient(circle at ${glareX}% ${glareY}%, rgba(255,255,255,0.2) 0%, transparent 50%)`,
    });
  };

  const handleMouseLeave = () => {
    setTransform('');
    setGlareStyle({});
  };

  return (
    <AnimatedItem delay={delay}>
      <div
        ref={cardRef}
        className="value-card"
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        style={{ transform }}
      >
        <div className="value-card-border" />
        <div className="value-card-glare" style={glareStyle} />
        <div className="value-card__icon"><Icon /></div>
        <h3 className="value-card__title">{title}</h3>
        <p className="value-card__text">{text}</p>
      </div>
    </AnimatedItem>
  );
};

// --- Timeline Item ---
const TimelineItem = ({ date, title, description, delay }) => {
  return (
    <AnimatedItem delay={delay} className="timeline-item">
      <div className="timeline-dot" />
      <div className="timeline-content">
        <span className="timeline-date">{date}</span>
        <h3 className="timeline-title">{title}</h3>
        <p className="timeline-description">{description}</p>
      </div>
    </AnimatedItem>
  );
};

// --- Testimonial Data ---
const testimonials = [
  {
    content: "Cramer đã giúp mình cải thiện kỹ năng Listening đáng kể. Giao diện rất dễ sử dụng và các bài test rất sát với đề thi thật!",
    author: "Minh Anh",
    role: "Sinh viên, Đại học Bách Khoa",
    avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop"
  },
  {
    content: "Mình đặc biệt thích tính năng review sau khi làm bài. Nó giúp mình hiểu rõ những lỗi sai và cải thiện nhanh hơn.",
    author: "Hoàng Long",
    role: "IELTS 7.5",
    avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop"
  },
  {
    content: "Từ khi dùng Cramer, mình thấy tự tin hơn nhiều khi luyện tập. Cảm ơn team đã tạo ra một nền tảng tuyệt vời như vậy!",
    author: "Thu Hà",
    role: "Giáo viên tiếng Anh",
    avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop"
  }
];

// --- Main About Page Component ---
const AboutPage = () => {
  const navigate = useNavigate();
  const [activeTestimonial, setActiveTestimonial] = useState(0);

  // Section visibility tracking for performance optimization
  const [heroRef, heroInView] = useSectionInView({ rootMargin: '100px' });
  const [testimonialsRef, testimonialsInView] = useSectionInView({ rootMargin: '200px' });

  // Auto-rotate testimonials only when section is in view
  useEffect(() => {
    if (!testimonialsInView) return;
    
    const interval = setInterval(() => {
      setActiveTestimonial((prev) => (prev + 1) % testimonials.length);
    }, 5000);
    return () => clearInterval(interval);
  }, [testimonialsInView]);

  const scrollToContent = () => {
    const visionSection = document.querySelector('.about-vision');
    if (visionSection) {
      visionSection.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <main className="about-page">
      {/* Hero Section with 3D Background */}
      <section className="about-hero" ref={heroRef}>
        <div className="about-hero__3d-background">
          <Suspense fallback={
            <div style={{ 
              width: '100%', 
              height: '100%', 
              background: 'linear-gradient(135deg, #0f0a1e 0%, #1a0a3e 50%, #2a1a5e 100%)' 
            }} />
          }>
            <Scene3DAbout 
              style={{ width: '100%', height: '100%' }} 
              isActive={heroInView}
            />
          </Suspense>
        </div>
        
        <div className="about-hero__content">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: 'easeOut' }}
          >
            <span className="about-hero__badge">
              <FiZap /> Được xây dựng bởi sinh viên, cho sinh viên
            </span>
          </motion.div>
          
          <motion.h1 
            className="about-hero__title"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2, ease: 'easeOut' }}
          >
            Hành trình chinh phục<br />IELTS bắt đầu từ đây
          </motion.h1>
          
          <motion.p 
            className="about-hero__subtitle"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4, ease: 'easeOut' }}
          >
            Cramer ra đời từ chính trải nghiệm của hai sinh viên đam mê công nghệ và mong muốn tạo ra một nơi luyện tập IELTS thật sự chất lượng — miễn phí và dành cho tất cả mọi người.
          </motion.p>
        </div>
        
        <motion.div 
          className="about-hero__scroll-indicator"
          onClick={scrollToContent}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.8, delay: 1 }}
        >
          <span>Khám phá thêm</span>
          <FiChevronDown />
        </motion.div>
      </section>

      {/* Vision Section */}
      <section className="about-vision">
        <div className="container">
          <div className="vision-content">
            <div className="vision-text">
              <AnimatedItem>
                <h2>Tầm nhìn của chúng mình</h2>
              </AnimatedItem>
              <AnimatedItem delay={200}>
                <p>
                  Chúng mình tin rằng mọi người đều xứng đáng có cơ hội tiếp cận tài liệu luyện thi chất lượng cao, không bị giới hạn bởi chi phí hay địa lý. Cramer được xây dựng với tâm huyết để trở thành người bạn đồng hành đáng tin cậy trên hành trình chinh phục IELTS của bạn.
                </p>
              </AnimatedItem>
              <AnimatedItem delay={400}>
                <div className="vision-stats">
                  <div className="vision-stat">
                    <div className="vision-stat__number">100%</div>
                    <div className="vision-stat__label">Miễn phí</div>
                  </div>
                  <div className="vision-stat">
                    <div className="vision-stat__number">4</div>
                    <div className="vision-stat__label">Kỹ năng</div>
                  </div>
                  <div className="vision-stat">
                    <div className="vision-stat__number">∞</div>
                    <div className="vision-stat__label">Luyện tập</div>
                  </div>
                </div>
              </AnimatedItem>
            </div>
            <div className="vision-3d">
              {/* Placeholder for future 3D element or illustration */}
              <AnimatedItem delay={300}>
                <div style={{ 
                  width: '100%', 
                  height: '100%', 
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '8rem',
                  opacity: 0.3
                }}>
                  🎯
                </div>
              </AnimatedItem>
            </div>
          </div>
        </div>
      </section>

      {/* Story/Timeline Section */}
      <section className="about-story">
        <div className="container">
          <AnimatedItem>
            <h2 className="section-title">Câu chuyện của Cramer</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="section-subtitle">
              Một hành trình ngắn ngủi nhưng đầy ý nghĩa
            </p>
          </AnimatedItem>
          
          <div className="timeline">
            <TimelineItem
              date="Tháng 10, 2025"
              title="Ý tưởng nảy sinh"
              description="Trong một buổi tối bình thường trên Discord, Hữu và Khoa than thở về việc khó tìm được nguồn luyện IELTS miễn phí và chất lượng. Và thế là, ý tưởng về Cramer ra đời."
              delay={200}
            />
            <TimelineItem
              date="Tháng 11, 2025"
              title="Bắt đầu xây dựng"
              description="Khoa bắt tay vào code trong khi Hữu nghiên cứu về cách thiết kế bài test sao cho sát với đề thi thật nhất. Những ngày làm việc cùng nhau qua Discord thật sự rất vui."
              delay={400}
            />
            <TimelineItem
              date="Hiện tại"
              title="Không ngừng phát triển"
              description="Cramer vẫn đang trong giai đoạn phát triển với nhiều tính năng mới được thêm vào liên tục. Mục tiêu của chúng mình là tạo ra trải nghiệm luyện thi tốt nhất cho cộng đồng."
              delay={600}
            />
          </div>
        </div>
      </section>

      {/* Founders Section */}
      <section className="about-founders">
        <div className="container">
          <AnimatedItem>
            <h2 className="section-title">Về hai đứa mình</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="section-subtitle">
              Hai sinh viên với một đam mê chung: làm việc học trở nên dễ dàng hơn
            </p>
          </AnimatedItem>
          
          <div className="founders-grid">
            <FounderCard
              image="/pictures/about/HuynhQuocHuu.png"
              name="Huỳnh Quốc Hữu (Jacob)"
              role="Đồng sáng lập"
              bio="Là người vạch ra những ý tưởng và phương pháp học tập cho Cramer. Hữu luôn tìm cách để việc học IELTS trở nên thú vị và hiệu quả nhất, dựa trên chính kinh nghiệm 'cày cuốc' của mình."
              hobby="Đọc nghiên cứu, tìm hiểu khoa học, và tất nhiên là... tiếng Anh."
              github={{ url: "https://github.com/huuunleashed", username: "huuunleashed" }}
              delay={200}
            />
            <FounderCard
              image="/pictures/about/LeMinhKhoa.jpg"
              name="Lê Minh Khoa"
              role="Đồng sáng lập"
              bio="Khoa là người biến những ý tưởng của Hữu thành hiện thực bằng những dòng code. Với kiến thức về kỹ thuật phần mềm, cậu ấy chăm chút cho Cramer từng chút một để bạn có được trải nghiệm học tập mượt mà nhất."
              hobby="Sáng tác nhạc, tập gym và nghe những bản nhạc... không cùng gu với Hữu."
              github={{ url: "https://github.com/leminhkhoa117", username: "leminhkhoa117" }}
              delay={400}
            />
          </div>
        </div>
      </section>

      {/* Values Section */}
      <section className="about-values">
        <div className="container">
          <AnimatedItem>
            <h2 className="section-title">Kim chỉ nam của Cramer</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="section-subtitle">
              Những điều chúng mình luôn tin tưởng và theo đuổi
            </p>
          </AnimatedItem>
          
          <div className="values-grid">
            <ValueCard
              icon={FiTarget}
              title="Chất lượng là trên hết"
              text="Mỗi một bài test trên Cramer đều được chọn lọc và xây dựng một cách kỹ lưỡng, bám sát cấu trúc thi thật của IELTS."
              delay={200}
            />
            <ValueCard
              icon={FiUsers}
              title="Dành cho tất cả mọi người"
              text="Chúng mình tin rằng ai cũng xứng đáng có một nơi luyện tập tốt mà không phải lo lắng quá nhiều về chi phí."
              delay={400}
            />
            <ValueCard
              icon={FiHeart}
              title="Từ người học, cho người học"
              text="Vì cũng là người học, chúng mình thiết kế Cramer dựa trên chính những gì mình cần và mong muốn ở một nền tảng luyện thi."
              delay={600}
            />
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section className="about-testimonials" ref={testimonialsRef}>
        <div className="container">
          <AnimatedItem>
            <h2 className="section-title">Học viên nói gì về Cramer</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="section-subtitle">
              Những phản hồi chân thực từ cộng đồng người dùng
            </p>
          </AnimatedItem>
          
          <div className="testimonials-wrapper">
            <AnimatePresence mode="wait">
              <motion.div
                key={activeTestimonial}
                className="testimonial-card"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.5 }}
              >
                <p className="testimonial-content">
                  {testimonials[activeTestimonial].content}
                </p>
                <div className="testimonial-author">
                  <img 
                    src={testimonials[activeTestimonial].avatar} 
                    alt={testimonials[activeTestimonial].author}
                    className="testimonial-avatar"
                  />
                  <div className="testimonial-info">
                    <h4>{testimonials[activeTestimonial].author}</h4>
                    <p>{testimonials[activeTestimonial].role}</p>
                  </div>
                </div>
              </motion.div>
            </AnimatePresence>
            
            <div className="testimonial-nav">
              {testimonials.map((_, index) => (
                <button
                  key={index}
                  className={`testimonial-dot ${index === activeTestimonial ? 'active' : ''}`}
                  onClick={() => setActiveTestimonial(index)}
                  aria-label={`Xem phản hồi ${index + 1}`}
                />
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="about-cta">
        <div className="about-cta__background" />
        <div className="container">
          <AnimatedItem>
            <h2 className="cta-title">Sẵn sàng bắt đầu chưa?</h2>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="cta-text">
              Tham gia cùng hàng nghìn người học khác và bắt đầu hành trình chinh phục IELTS của bạn ngay hôm nay.
            </p>
          </AnimatedItem>
          <AnimatedItem delay={400}>
            <button onClick={() => navigate('/dashboard')} className="cta-button">
              Bắt đầu luyện tập ngay <FiArrowRight />
            </button>
          </AnimatedItem>
        </div>
      </section>
    </main>
  );
};

export default AboutPage;
