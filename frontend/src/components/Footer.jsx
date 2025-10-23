import React from 'react'

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div className="contact">
          <strong>Contact</strong>
          <div>Email: hello@cramer.example</div>
          <div>Phone: +84 123 456 789</div>
        </div>
        <div className="copyright">© {new Date().getFullYear()} Cramer</div>
      </div>
    </footer>
  )
}
