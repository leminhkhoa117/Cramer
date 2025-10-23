import React from 'react'

export default function Home() {
  return (
    <main className="home-page">
      <div className="hero">
        <h1>Welcome to Cramer</h1>
        <p>Your place to practice and improve your IELTS skills.</p>
        <div className="hero-actions">
          <a href="/courses" className="btn btn-secondary">Browse Courses</a>
          <a href="/auth" className="btn btn-outline">Get Started</a>
        </div>
      </div>

      <section className="features">
        <h2>What you'll get</h2>
        <ul>
          <li>Personalized practice</li>
          <li>Real exam simulations</li>
          <li>Expert feedback</li>
        </ul>
      </section>
    </main>
  )
}
