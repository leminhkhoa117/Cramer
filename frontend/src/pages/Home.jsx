import React, { Suspense, lazy } from 'react';
import '../css/Home.css';

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

export default function Home() {
  return (
    <div className="home-page">
      <Suspense fallback={<SectionLoader />}>
        <HeroSection />
      </Suspense>
      <Suspense fallback={<SectionLoader />}>
        <FeaturesSection />
      </Suspense>
      <Suspense fallback={<SectionLoader />}>
        <GuideSection />
      </Suspense>
      <Suspense fallback={<SectionLoader />}>
        <TestimonialsSection />
      </Suspense>
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
