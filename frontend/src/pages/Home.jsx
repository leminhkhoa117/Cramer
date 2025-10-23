import React from 'react';
import { Container, Row, Col, Form, Button, Dropdown } from 'react-bootstrap';

export default function Home() {
  return (
    <main className="home-page font-sans">
      {/* Hero Section */}
      <div style={{
        background: 'linear-gradient(135deg, rgba(124, 58, 237, 0.85) 0%, rgba(99, 102, 241, 0.85) 50%, rgba(139, 92, 246, 0.85) 100%), url("https://thumbs.dreamstime.com/b/diverse-group-adult-students-having-conversations-english-speaking-club-diverse-group-people-talking-to-each-other-251584879.jpg")',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundAttachment: 'fixed',
        minHeight: '600px',
        paddingTop: '2rem'
      }}>
        <Container fluid className="p-0" style={{ paddingLeft: '2rem', paddingRight: '2rem' }}>
          <Row className="m-0 align-items-center h-100">
            {/* Welcome Text */}
            <Col lg={6} className="d-flex flex-column justify-content-center" style={{ paddingLeft: '2rem', paddingRight: '1rem' }}>
              <h1 className="display-4 fw-bold mb-3 text-white" style={{ textShadow: '0 2px 8px rgba(0,0,0,0.2)' }}>
                Welcome to Cramer
              </h1>
              <p className="fs-5 mb-4 text-white" style={{ textShadow: '0 1px 3px rgba(0,0,0,0.15)', lineHeight: '1.6' }}>
                Your place to practice and improve your English skills for the IELTS exam.
              </p>
              <div className="d-flex gap-3 flex-wrap">
                <Button 
                  size="lg" 
                  style={{
                    background: 'white',
                    color: '#7c3aed',
                    border: 'none',
                    fontWeight: '600',
                    padding: '0.6rem 2rem',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    transition: 'transform 0.2s, box-shadow 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.transform = 'translateY(-2px)';
                    e.target.style.boxShadow = '0 8px 16px rgba(0,0,0,0.2)';
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.transform = 'translateY(0)';
                    e.target.style.boxShadow = 'none';
                  }}
                >
                  Browse Courses
                </Button>
                <Button 
                  size="lg"
                  style={{
                    background: 'transparent',
                    color: 'white',
                    border: '2px solid white',
                    fontWeight: '600',
                    padding: '0.6rem 2rem',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    transition: 'all 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.background = 'white';
                    e.target.style.color = '#7c3aed';
                    e.target.style.transform = 'translateY(-2px)';
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.background = 'transparent';
                    e.target.style.color = 'white';
                    e.target.style.transform = 'translateY(0)';
                  }}
                >
                  Get Started
                </Button>
              </div>
            </Col>
            {/* Search Box */}
            <Col lg={6} className="d-flex flex-column justify-content-center" style={{ paddingLeft: '1rem', paddingRight: '2rem' }}>
              <div 
                className="rounded-lg shadow-lg"
                style={{
                  background: 'rgba(255, 255, 255, 0.98)',
                  backdropFilter: 'blur(10px)',
                  borderRadius: '12px',
                  padding: '1.5rem'
                }}
              >
                <h2 className="fw-bold mb-4" style={{ color: '#1f2937', fontSize: '1.3rem', fontFamily: '"Be Vietnam Pro", sans-serif' }}>Find a Test</h2>
                <Form>
                  <Form.Group className="mb-3">
                    <Form.Control
                      type="text"
                      placeholder="Search for a test..."
                      size="lg"
                      style={{
                        backgroundColor: '#f3f4f6',
                        border: '2px solid #e5e7eb',
                        color: '#1f2937',
                        borderRadius: '8px',
                        padding: '0.6rem 0.8rem',
                        fontSize: '0.9rem',
                        transition: 'border-color 0.3s',
                        fontFamily: '"Be Vietnam Pro", sans-serif'
                      }}
                      onFocus={(e) => e.target.style.borderColor = '#7c3aed'}
                      onBlur={(e) => e.target.style.borderColor = '#e5e7eb'}
                    />
                  </Form.Group>
                  <Row className="gap-0 mb-3">
                    <Col xs={12} sm={6}>
                      <Dropdown className="w-100">
                        <Dropdown.Toggle
                          variant="outline-secondary"
                          id="dropdown-cambridge"
                          className="w-100"
                          style={{
                            color: '#4b5563',
                            borderColor: '#d1d5db',
                            fontSize: '0.9rem',
                            fontWeight: '500',
                            padding: '0.5rem 0.8rem',
                            fontFamily: '"Be Vietnam Pro", sans-serif'
                          }}
                        >
                          IELTS Cambridge
                        </Dropdown.Toggle>
                        <Dropdown.Menu className="w-100">
                          <Dropdown.Item href="#/action-1">Cambridge 18</Dropdown.Item>
                          <Dropdown.Item href="#/action-2">Cambridge 17</Dropdown.Item>
                          <Dropdown.Item href="#/action-3">Cambridge 16</Dropdown.Item>
                        </Dropdown.Menu>
                      </Dropdown>
                    </Col>
                    <Col xs={12} sm={6}>
                      <Dropdown className="w-100">
                        <Dropdown.Toggle
                          variant="outline-secondary"
                          id="dropdown-skill"
                          className="w-100"
                          style={{
                            color: '#4b5563',
                            borderColor: '#d1d5db',
                            fontSize: '0.9rem',
                            fontWeight: '500',
                            padding: '0.5rem 0.8rem',
                            fontFamily: '"Be Vietnam Pro", sans-serif'
                          }}
                        >
                          Skill
                        </Dropdown.Toggle>
                        <Dropdown.Menu className="w-100">
                          <Dropdown.Item href="#/action-1">Speaking</Dropdown.Item>
                          <Dropdown.Item href="#/action-2">Reading</Dropdown.Item>
                          <Dropdown.Item href="#/action-3">Listening</Dropdown.Item>
                          <Dropdown.Item href="#/action-4">Writing</Dropdown.Item>
                        </Dropdown.Menu>
                      </Dropdown>
                    </Col>
                  </Row>
                  <Button
                    type="submit"
                    className="w-100 fw-bold"
                    size="lg"
                    style={{
                      background: 'linear-gradient(135deg, #7c3aed 0%, #6366f1 100%)',
                      border: 'none',
                      borderRadius: '8px',
                      padding: '0.6rem',
                      fontSize: '0.9rem',
                      transition: 'transform 0.2s, box-shadow 0.2s',
                      cursor: 'pointer',
                      fontFamily: '"Be Vietnam Pro", sans-serif',
                      fontWeight: '600'
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.transform = 'translateY(-2px)';
                      e.currentTarget.style.boxShadow = '0 8px 16px rgba(124, 58, 237, 0.3)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.transform = 'translateY(0)';
                      e.currentTarget.style.boxShadow = 'none';
                    }}
                  >
                    Search
                  </Button>
                </Form>
              </div>
            </Col>
          </Row>
        </Container>
      </div>

      {/* Features Section */}
      <section className="py-5" style={{ background: '#f9fafb' }}>
        <Container fluid style={{ paddingLeft: '2rem', paddingRight: '2rem' }}>
          <h2 className="text-center fw-bold mb-5" style={{ fontSize: '2.5rem', color: '#1f2937' }}>What you'll get</h2>
          <Row className="g-4">
            <Col md={4}>
              <div
                className="p-5 h-100 rounded-lg"
                style={{
                  background: 'white',
                  border: '1px solid #e5e7eb',
                  transition: 'all 0.3s',
                  cursor: 'pointer',
                  borderRadius: '12px',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-4px)';
                  e.currentTarget.style.boxShadow = '0 12px 24px rgba(0,0,0,0.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.05)';
                }}
              >
                <div className="mb-3" style={{
                  width: '60px',
                  height: '60px',
                  background: 'linear-gradient(135deg, #7c3aed 0%, #6366f1 100%)',
                  borderRadius: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <span style={{ fontSize: '2rem' }}>📚</span>
                </div>
                <h3 className="fw-bold mb-3" style={{ fontSize: '1.25rem', color: '#1f2937' }}>Personalized practice</h3>
                <p style={{ color: '#6b7280', lineHeight: '1.6' }}>Tailored exercises to fit your learning style and pace.</p>
              </div>
            </Col>
            <Col md={4}>
              <div
                className="p-5 h-100 rounded-lg"
                style={{
                  background: 'white',
                  border: '1px solid #e5e7eb',
                  transition: 'all 0.3s',
                  cursor: 'pointer',
                  borderRadius: '12px',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-4px)';
                  e.currentTarget.style.boxShadow = '0 12px 24px rgba(0,0,0,0.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.05)';
                }}
              >
                <div className="mb-3" style={{
                  width: '60px',
                  height: '60px',
                  background: 'linear-gradient(135deg, #7c3aed 0%, #6366f1 100%)',
                  borderRadius: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <span style={{ fontSize: '2rem' }}>🎯</span>
                </div>
                <h3 className="fw-bold mb-3" style={{ fontSize: '1.25rem', color: '#1f2937' }}>Real exam simulations</h3>
                <p style={{ color: '#6b7280', lineHeight: '1.6' }}>Experience the pressure and timing of the actual IELTS test.</p>
              </div>
            </Col>
            <Col md={4}>
              <div
                className="p-5 h-100 rounded-lg"
                style={{
                  background: 'white',
                  border: '1px solid #e5e7eb',
                  transition: 'all 0.3s',
                  cursor: 'pointer',
                  borderRadius: '12px',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-4px)';
                  e.currentTarget.style.boxShadow = '0 12px 24px rgba(0,0,0,0.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.05)';
                }}
              >
                <div className="mb-3" style={{
                  width: '60px',
                  height: '60px',
                  background: 'linear-gradient(135deg, #7c3aed 0%, #6366f1 100%)',
                  borderRadius: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <span style={{ fontSize: '2rem' }}>⭐</span>
                </div>
                <h3 className="fw-bold mb-3" style={{ fontSize: '1.25rem', color: '#1f2937' }}>Expert feedback</h3>
                <p style={{ color: '#6b7280', lineHeight: '1.6' }}>Receive detailed feedback from certified English instructors.</p>
              </div>
            </Col>
          </Row>
        </Container>
      </section>
    </main>
  );
}
