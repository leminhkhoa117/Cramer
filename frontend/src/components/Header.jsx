import React from 'react';
import { Navbar, Nav, Container, Button, Dropdown } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { FaUserCircle } from 'react-icons/fa';

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
        <Navbar.Brand href="/" className="font-bold text-2xl" style={{ color: 'white', fontFamily: '"Be Vietnam Pro", sans-serif', marginLeft: 0 }}>
          Cramer
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" style={{ borderColor: 'rgba(255,255,255,0.5)' }} />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="ms-auto">
            <Nav.Link href="/" style={{ color: 'white', fontWeight: '500', marginRight: '1.5rem' }}>Home</Nav.Link>
            <Nav.Link href="/courses" style={{ color: 'white', fontWeight: '500', marginRight: '1.5rem' }}>Courses</Nav.Link>
            <Nav.Link href="/about" style={{ color: 'white', fontWeight: '500', marginRight: '1.5rem' }}>About</Nav.Link>
            
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
                    Dashboard
                  </Dropdown.Item>
                  <Dropdown.Item onClick={() => navigate('/profile')}>
                    Profile
                  </Dropdown.Item>
                  <Dropdown.Divider />
                  <Dropdown.Item onClick={handleLogout}>
                    Logout
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
                Login
              </Button>
            )}
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
