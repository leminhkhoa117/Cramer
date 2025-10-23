import React from 'react';

export default function Footer() {
  return (
    <footer style={{
      background: 'linear-gradient(135deg, #6b21a8 0%, #7c3aed 50%, #6366f1 100%)',
      color: 'white',
      padding: '3rem 0',
      marginTop: 'auto'
    }}>
      <div className="container">
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
          gap: '2rem',
          marginBottom: '2rem'
        }}>
          {/* Brand Section */}
          <div>
            <h3 className="fw-bold mb-3" style={{ fontSize: '1.25rem' }}>Cramer</h3>
            <p style={{ color: 'rgba(255, 255, 255, 0.9)', lineHeight: '1.6' }}>
              Master IELTS with personalized practice, real exam simulations, and expert feedback.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="fw-bold mb-3" style={{ fontSize: '1rem' }}>Quick Links</h4>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
              <li className="mb-2"><a href="/" style={{ color: 'rgba(255, 255, 255, 0.9)', textDecoration: 'none', transition: 'opacity 0.3s' }} onMouseEnter={(e) => e.target.style.opacity = '0.7'} onMouseLeave={(e) => e.target.style.opacity = '1'}>Home</a></li>
              <li className="mb-2"><a href="/courses" style={{ color: 'rgba(255, 255, 255, 0.9)', textDecoration: 'none', transition: 'opacity 0.3s' }} onMouseEnter={(e) => e.target.style.opacity = '0.7'} onMouseLeave={(e) => e.target.style.opacity = '1'}>Courses</a></li>
              <li className="mb-2"><a href="/about" style={{ color: 'rgba(255, 255, 255, 0.9)', textDecoration: 'none', transition: 'opacity 0.3s' }} onMouseEnter={(e) => e.target.style.opacity = '0.7'} onMouseLeave={(e) => e.target.style.opacity = '1'}>About</a></li>
            </ul>
          </div>

          {/* Contact Section */}
          <div>
            <h4 className="fw-bold mb-3" style={{ fontSize: '1rem' }}>Contact Us</h4>
            <p style={{ color: 'rgba(255, 255, 255, 0.9)', margin: '0.5rem 0' }}>
              <strong>Email:</strong> hello@cramer.example
            </p>
            <p style={{ color: 'rgba(255, 255, 255, 0.9)', margin: '0.5rem 0' }}>
              <strong>Phone:</strong> +84 123 456 789
            </p>
          </div>
        </div>

        {/* Divider */}
        <div style={{
          borderTop: '1px solid rgba(255, 255, 255, 0.2)',
          paddingTop: '2rem',
          textAlign: 'center',
          color: 'rgba(255, 255, 255, 0.8)'
        }}>
          <p style={{ margin: 0 }}>
            &copy; {new Date().getFullYear()} Cramer. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
