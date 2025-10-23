import React from 'react'

export default function Header() {
  return (
    <header className="site-header">
      <div className="header-inner">
        <div className="brand">Cramer</div>
        <nav className="nav">
          <a href="/" className="nav-link">Home</a>
          <a href="/courses" className="nav-link">Courses</a>
          <a href="/about" className="nav-link">About</a>
        </nav>
        <div className="auth">
          <a href="/auth" className="btn btn-primary">Login</a>
        </div>
      </div>
    </header>
  )
}
