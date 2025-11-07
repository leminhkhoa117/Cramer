import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaGithub } from 'react-icons/fa';
import { FiHeart, FiTarget, FiUsers } from 'react-icons/fi';

// --- Custom Hook for Scroll Animation ---
const useInView = (options) => {
  const ref = useRef(null);
  const [isInView, setIsInView] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setIsInView(true);
        observer.unobserve(entry.target);
      }
    }, options);

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => {
      if (ref.current) {
        observer.unobserve(ref.current);
      }
    };
  }, [ref, options]);

  return [ref, isInView];
};

// --- Animated Component Wrapper ---
const AnimatedItem = ({ children, delay = 0 }) => {
  const [ref, isInView] = useInView({ threshold: 0.1, triggerOnce: true });
  return (
    <div 
      ref={ref} 
      className={`animated-item ${isInView ? 'in-view' : ''}`}
      style={{ transitionDelay: `${delay}ms` }}
    >
      {children}
    </div>
  );
};


// Placeholder for founder images
const placeholderImg = 'https://st3.depositphotos.com/6672868/13701/v/450/depositphotos_137014128-stock-illustration-user-profile-icon.jpg';

const AboutPage = () => {
  const navigate = useNavigate();
  return (
    <main className="about-page">
      {/* Section 1: Hero */}
      <section className="about-hero">
        <div className="about-hero__overlay"></div>
        <div className="container text-center">
          <AnimatedItem>
            <h1 className="about-hero__title">Câu chuyện của Cramer bắt đầu từ đâu?</h1>
          </AnimatedItem>
          <AnimatedItem delay={200}>
            <p className="about-hero__subtitle">
              Là sinh viên, chúng mình hiểu hơn ai hết cảm giác loay hoay trước một kỳ thi quan trọng như IELTS. Cramer ra đời từ chính trăn trở đó - một mong muốn tạo ra một góc luyện tập thật chất lượng, một nơi bạn có thể mài giũa kỹ năng một cách thoải mái trước khi bước vào kỳ thi thật.
            </p>
          </AnimatedItem>
        </div>
      </section>

      {/* Section 2: The Founders */}
      <section className="about-founders">
        <div className="container">
          <AnimatedItem>
            <h2 className="section-title">Về hai đứa mình</h2>
          </AnimatedItem>
          <div className="founders-grid">
            <AnimatedItem delay={200}>
              <div className="founder-card">
                <div className="founder-card__image-wrapper">
                  <img src={placeholderImg} alt="Huynh Quoc Huu" className="founder-card__image" />
                </div>
                <div className="founder-card__content">
                  <h3 className="founder-card__name">Huỳnh Quốc Hữu (Jacob)</h3>
                  <p className="founder-card__role">Co-founder</p>
                  <p className="founder-card__bio">
                    Là người vạch ra những ý tưởng và phương pháp học tập cho Cramer, Hữu luôn tìm cách để việc học IELTS trở nên thú vị và hiệu quả nhất, dựa trên chính kinh nghiệm 'cày cuốc' của mình.
                  </p>
                  <p className="founder-card__hobby"><strong>Sở thích:</strong> Đọc nghiên cứu, tìm hiểu khoa học, và tất nhiên là... tiếng Anh.</p>
                  <a href="https://github.com/huuunleashed" target="_blank" rel="noopener noreferrer" className="founder-card__github">
                    <FaGithub /> huuunleashed
                  </a>
                </div>
              </div>
            </AnimatedItem>

            <AnimatedItem delay={400}>
              <div className="founder-card">
                <div className="founder-card__image-wrapper">
                  <img src={placeholderImg} alt="Le Minh Khoa" className="founder-card__image" />
                </div>
                <div className="founder-card__content">
                  <h3 className="founder-card__name">Lê Minh Khoa</h3>
                  <p className="founder-card__role">Co-founder</p>
                  <p className="founder-card__bio">
                    Khoa là người biến những ý tưởng của Hữu thành hiện thực bằng những dòng code. Với kiến thức về kỹ thuật phần mềm, cậu ấy chăm chút cho Cramer từng chút một, để bạn có được trải nghiệm học tập mượt mà nhất.
                  </p>
                  <p className="founder-card__hobby"><strong>Sở thích:</strong> Sáng tác nhạc, tập gym và nghe những bản nhạc... không cùng gu với Hữu.</p>
                  <a href="https://github.com/leminhkhoa117" target="_blank" rel="noopener noreferrer" className="founder-card__github">
                    <FaGithub /> leminhkhoa117
                  </a>
                </div>
              </div>
            </AnimatedItem>
          </div>
        </div>
      </section>

      {/* Section 3: Our Values */}
      <section className="about-values">
        <div className="container">
          <AnimatedItem>
            <h2 className="section-title">Kim chỉ nam của Cramer</h2>
            <p className="section-subtitle">Những điều chúng mình luôn tin tưởng và theo đuổi</p>
          </AnimatedItem>
          <div className="values-grid">
            <AnimatedItem delay={200}>
              <div className="value-card">
                <div className="value-card__icon"><FiTarget /></div>
                <h3 className="value-card__title">Chất lượng là trên hết</h3>
                <p className="value-card__text">Mỗi một bài test trên Cramer đều được chúng mình chọn lọc và xây dựng một cách kỹ lưỡng, bám sát cấu trúc thi thật.</p>
              </div>
            </AnimatedItem>
            <AnimatedItem delay={400}>
              <div className="value-card">
                <div className="value-card__icon"><FiUsers /></div>
                <h3 className="value-card__title">Dành cho tất cả mọi người</h3>
                <p className="value-card__text">Chúng mình tin rằng ai cũng xứng đáng có một nơi luyện tập tốt mà không phải quá lo lắng về chi phí.</p>
              </div>
            </AnimatedItem>
            <AnimatedItem delay={600}>
              <div className="value-card">
                <div className="value-card__icon"><FiHeart /></div>
                <h3 className="value-card__title">Từ người học, cho người học</h3>
                <p className="value-card__text">Vì cũng là người học, chúng mình thiết kế Cramer dựa trên chính những gì chúng mình cần và mong muốn ở một nền tảng luyện thi.</p>
              </div>
            </AnimatedItem>
          </div>
        </div>
      </section>

      {/* Section 4: CTA */}
      <section className="about-cta">
        <div className="container text-center">
          <AnimatedItem>
            <h2 className="cta-title">Cùng đồng hành nhé?</h2>
            <p className="cta-text">Bắt đầu hành trình chinh phục IELTS của bạn ngay hôm nay.</p>
            <button onClick={() => navigate('/dashboard')} className="cta-button">
              Bắt đầu luyện tập ngay
            </button>
          </AnimatedItem>
        </div>
      </section>
    </main>
  );
};

export default AboutPage;
