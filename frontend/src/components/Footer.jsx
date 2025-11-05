import React from 'react';
import { Link } from 'react-router-dom';
import { FaGithub } from 'react-icons/fa';
import '../css/Footer.css';

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container">
        <div className="footer-content">
          {/* Left Section: Brand */}
          <div className="footer-brand">
            <Link to="/" className="footer-brand__logo">Cramer</Link>
            <p className="footer-brand__tagline">Your friendly corner for IELTS practice.</p>
          </div>

          {/* Middle Section: Navigation & Contact */}
          <div className="footer-links-wrapper">
            <nav className="footer-nav">
              <h4 className="footer-links__title">Navigate</h4>
              <Link to="/" className="footer-nav__link">Home</Link>
              <Link to="/about" className="footer-nav__link">About</Link>
              <Link to="/dashboard" className="footer-nav__link">Dashboard</Link>
            </nav>

            <div className="footer-contact">
              <h4 className="footer-links__title">Contact</h4>
              <a href="mailto:hello@cramer.vn" className="footer-nav__link">hello@cramer.vn</a>
            </div>
          </div>

          {/* Right Section: Social Links */}
          <div className="footer-socials">
            <a href="https://github.com/huuunleashed" target="_blank" rel="noopener noreferrer" className="footer-socials__link">
              <FaGithub />
            </a>
            <a href="https://github.com/leminhkhoa117" target="_blank" rel="noopener noreferrer" className="footer-socials__link">
              <FaGithub />
            </a>
          </div>
        </div>

        <div className="footer-bottom">
          <p>&copy; {new Date().getFullYear()} Cramer. Project by students, for students.</p>
        </div>
      </div>
    </footer>
  );
}
