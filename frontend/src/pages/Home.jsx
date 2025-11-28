import React from 'react';
import HeroSection from '../components/home/HeroSection';
import FeaturesSection from '../components/home/FeaturesSection';
import GuideSection from '../components/home/GuideSection';
import SignupSection from '../components/home/SignupSection';
import '../css/Home.css';

export default function Home() {
  return (
    <main className="home-page">
      <HeroSection />
      <FeaturesSection />
      <GuideSection />
      <SignupSection />
    </main>
  );
}
