import React, { useState, useEffect } from 'react';
import { Navbar, Nav, Container, Button, Dropdown } from 'react-bootstrap';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore, useProfileStore } from '../stores';
import { FaUserCircle } from 'react-icons/fa';
import '../css/Header.css';

export default function Header() {
  const navigate = useNavigate();
  const user = useAuthStore(state => state.user);
  const signOut = useAuthStore(state => state.signOut);
  const profile = useProfileStore(state => state.profile);
  const profileLoading = useProfileStore(state => state.loading);
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 10) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };

    window.addEventListener('scroll', handleScroll);

    // Cleanup function to remove the event listener
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  const handleLogout = async () => {
    await signOut();
    navigate('/login');
  };

  // Display name priority: fullName > username > email prefix
  const displayName = profileLoading
    ? 'Loading...'
    : profile?.fullName || profile?.full_name || profile?.username || user?.email?.split('@')[0] || 'User';

  // Avatar URL from profile
  const avatarUrl = profile?.avatarUrl || profile?.avatar_url;

  return (
    <Navbar expand="lg" className={`w-100 ${isScrolled ? 'scrolled' : ''}`}>
      <Container fluid className="header-container">
        <Navbar.Brand as={Link} to="/" className="header-brand">
          <img
            src="/pictures/logo/Icon.png"
            alt="Cramer Logo"
            className="header-logo"
          />
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" className="header-toggler" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="ms-auto">
            <Nav.Link as={NavLink} end to="/" className="header-nav-link">Trang chủ</Nav.Link>
            <Nav.Link as={NavLink} to="/courses" className="header-nav-link">Khóa học</Nav.Link>
            <Nav.Link as={NavLink} to="/pricing" className="header-nav-link">Gói Cramer</Nav.Link>
            <Nav.Link as={NavLink} to="/about" className="header-nav-link">Về chúng tôi</Nav.Link>

            {user ? (
              <Dropdown align="end">
                <Dropdown.Toggle
                  variant="link"
                  id="dropdown-user"
                  className="header-user-dropdown"
                >
                  {avatarUrl ? (
                    <img
                      src={avatarUrl}
                      alt="Avatar"
                      className="header-avatar"
                    />
                  ) : (
                    <FaUserCircle size={20} />
                  )}
                  {displayName}
                </Dropdown.Toggle>

                <Dropdown.Menu>
                  <Dropdown.Item onClick={() => navigate('/dashboard')}>
                    Bảng điều khiển
                  </Dropdown.Item>
                  <Dropdown.Item onClick={() => navigate('/subscription')}>
                    Gói đăng ký
                  </Dropdown.Item>
                  <Dropdown.Item onClick={() => navigate('/vocabulary')}>
                    Sổ tay Từ vựng
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
              <Button onClick={() => navigate('/login')} className="header-login-btn">
                Đăng nhập
              </Button>
            )}
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
