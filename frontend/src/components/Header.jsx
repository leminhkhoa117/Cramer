import React from 'react';
import { Navbar, Nav, Container, Button, Dropdown } from 'react-bootstrap';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { FaUserCircle } from 'react-icons/fa';
import '../css/Header.css';

export default function Header() {
  const navigate = useNavigate();
  const { user, profile, profileLoading, signOut } = useAuth();
  
  const handleLogout = async () => {
    await signOut();
    navigate('/');
  };

  // Display username with proper loading state
  const displayName = profileLoading 
    ? 'Loading...' 
    : profile?.username || user?.email?.split('@')[0] || 'User';

  return (
    <Navbar expand="lg" className="w-100" style={{
      background: 'linear-gradient(135deg, #7c3aed 0%, #6366f1 50%, #8b5cf6 100%)',
      boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
      padding: '1rem 0'
    }}>
      <Container fluid style={{ paddingLeft: '2rem', paddingRight: '2rem' }}>
        <Navbar.Brand as={Link} to="/" className="font-bold text-2xl" style={{ marginLeft: 0, display: 'flex', alignItems: 'center' }}>
          <img 
            src="/pictures/logo/Icon.png" 
            alt="Cramer Logo" 
            style={{ height: '40px', objectFit: 'contain' }}
          />
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" style={{ borderColor: 'rgba(255,255,255,0.5)' }} />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="ms-auto">
            <Nav.Link as={NavLink} end to="/" style={{ color: 'white', fontWeight: '500', marginRight: '1.5rem' }} className="header-nav-link">Trang chủ</Nav.Link>
            <Nav.Link as={NavLink} to="/courses" style={{ color: 'white', fontWeight: '500', marginRight: '1.5rem' }} className="header-nav-link">Khóa học</Nav.Link>
            <Nav.Link as={NavLink} to="/about" style={{ color: 'white', fontWeight: '500', marginRight: '1.5rem' }} className="header-nav-link">Về chúng tôi</Nav.Link>
            
            {user ? (
              <Dropdown align="end">
                <Dropdown.Toggle 
                  variant="light" 
                  id="dropdown-user"
                  style={{
                    color: '#7c3aed',
                    fontWeight: '600',
                    fontSize: '0.95rem',
                    marginRight: 0,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}
                >
                  <FaUserCircle size={20} />
                  {displayName}
                </Dropdown.Toggle>

                <Dropdown.Menu>
                  <Dropdown.Item onClick={() => navigate('/dashboard')}>
                    Bảng điều khiển
                  </Dropdown.Item>
                  <Dropdown.Item onClick={() => navigate('/profile')}>
                    Hồ sơ
                  </Dropdown.Item>
                  <Dropdown.Divider />
                  <Dropdown.Item onClick={handleLogout}>
                    Đăng xuất
                  </Dropdown.Item>
                </Dropdown.Menu>
              </Dropdown>
            ) : (
              <Button onClick={() => navigate('/login')}
                variant="light" 
                style={{ 
                  color: '#7c3aed',
                  fontWeight: '600',
                  fontSize: '0.95rem',
                  marginRight: 0
                }}
              >
                Đăng nhập
              </Button>
            )}
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
