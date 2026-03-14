import React, { Suspense, lazy, useEffect, useRef } from 'react';
import '../css/home/index.css';

// Lazy load all sections for better performance
const HeroSection = lazy(() => import('../components/home/HeroSection'));
const FeaturesSection = lazy(() => import('../components/home/FeaturesSection'));
const GuideSection = lazy(() => import('../components/home/GuideSection'));
const TestimonialsSection = lazy(() => import('../components/home/TestimonialsSection'));
const DemoSection = lazy(() => import('../components/home/DemoSection'));
const FAQSection = lazy(() => import('../components/home/FAQSection'));
const SignupSection = lazy(() => import('../components/home/SignupSection'));

// Minimal loading placeholder
const SectionLoader = () => (
  <div className="section-loader" style={{
    minHeight: '50vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  }}>
    <div className="section-loader-spinner" />
  </div>
);

// Parallax decorative divider between sections
const ParallaxDivider = ({ variant = 1 }) => (
  <div className={`parallax-divider parallax-divider--${variant}`} aria-hidden="true">
    <div className="parallax-orb parallax-orb--1" />
    <div className="parallax-orb parallax-orb--2" />
  </div>
);

export default function Home() {
  const homeRef = useRef(null);

  // Single scroll listener that drives parallax via CSS custom property
  useEffect(() => {
    const handleScroll = () => {
      if (homeRef.current) {
        homeRef.current.style.setProperty('--scroll', window.scrollY + 'px');
      }
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="home-page" ref={homeRef}>
      <Suspense fallback={<SectionLoader />}>
        <HeroSection />
      </Suspense>

      <ParallaxDivider variant={1} />

      <Suspense fallback={<SectionLoader />}>
        <FeaturesSection />
      </Suspense>

      <ParallaxDivider variant={2} />

      <Suspense fallback={<SectionLoader />}>
        <GuideSection />
      </Suspense>
      <Suspense fallback={<SectionLoader />}>
        <TestimonialsSection />
      </Suspense>

      <ParallaxDivider variant={3} />

      <Suspense fallback={<SectionLoader />}>
        <DemoSection />
      </Suspense>
      <Suspense fallback={<SectionLoader />}>
        <FAQSection />
      </Suspense>
      <Suspense fallback={<SectionLoader />}>
        <SignupSection />
      </Suspense>
    </div>
  );
}
