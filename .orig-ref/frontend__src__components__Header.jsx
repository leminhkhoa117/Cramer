import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Navbar, Nav, Container, Button, Dropdown } from 'react-bootstrap';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore, useProfileStore } from '../stores';
import { FaUserCircle } from 'react-icons/fa';
import '../css/header.css';

export default function Header() {
  const navigate = useNavigate();
  const user = useAuthStore(state => state.user);
  const signOut = useAuthStore(state => state.signOut);
  const profile = useProfileStore(state => state.profile);
  const profileLoading = useProfileStore(state => state.loading);
  const [isScrolled, setIsScrolled] = useState(false);
  const [isHidden, setIsHidden] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 992);
  const lastScrollY = useRef(0);
  const scrollTicking = useRef(false);

  const handleScroll = useCallback(() => {
    if (scrollTicking.current) return;
    scrollTicking.current = true;
    requestAnimationFrame(() => {
      const currentY = window.scrollY;
      setIsScrolled(currentY > 10);
      if (currentY > 80 && currentY > lastScrollY.current) {
        setIsHidden(true);
      } else {
        setIsHidden(false);
      }
      lastScrollY.current = currentY;
      scrollTicking.current = false;
    });
  }, []);

  useEffect(() => {
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [handleScroll]);

  useEffect(() => {
    const check = () => setIsMobile(window.innerWidth < 992);
    window.addEventListener('resize', check);
    return () => window.removeEventListener('resize', check);
  }, []);

  const handleLogout = async () => {
    await signOut();
    navigate('/login');
  };

  const closeMenu = () => setExpanded(false);

  const displayName = profileLoading
    ? 'Loading...'
    : profile?.fullName || profile?.full_name || profile?.username || user?.email?.split('@')[0] || 'User';

  const avatarUrl = profile?.avatarUrl || profile?.avatar_url;
  const isAdmin = import.meta.env.VITE_DEV_ADMIN_BYPASS === 'true' || profile?.isAdmin === true || profile?.is_admin === true;

  const navLinks = (
    <Nav className="ms-auto header-nav">
      <Nav.Link as={NavLink} end to="/" className="header-nav-link" onClick={closeMenu}>Trang chủ</Nav.Link>
      <Nav.Link as={NavLink} to="/courses" className="header-nav-link" onClick={closeMenu}>Khóa học</Nav.Link>
      <Nav.Link as={NavLink} to="/pricing" className="header-nav-link" onClick={closeMenu}>Gói Cramer</Nav.Link>
      <Nav.Link as={NavLink} to="/about" className="header-nav-link" onClick={closeMenu}>Về chúng tôi</Nav.Link>
    </Nav>
  );

  const userSection = user ? (
    <Dropdown align="end">
      <Dropdown.Toggle variant="link" id="dropdown-user" className="header-user-dropdown">
        {avatarUrl ? (
          <img src={avatarUrl} alt="Avatar" className="header-avatar" />
        ) : (
          <FaUserCircle size={20} />
        )}
        {displayName}
      </Dropdown.Toggle>
      <Dropdown.Menu>
        <Dropdown.Item onClick={() => { closeMenu(); navigate('/dashboard'); }}>Bảng điều khiển</Dropdown.Item>
        <Dropdown.Item onClick={() => { closeMenu(); navigate('/subscription'); }}>Gói đăng ký</Dropdown.Item>
        <Dropdown.Item onClick={() => { closeMenu(); navigate('/vocabulary'); }}>Sổ tay Từ vựng</Dropdown.Item>
        <Dropdown.Item onClick={() => { closeMenu(); navigate('/profile'); }}>Hồ sơ</Dropdown.Item>
        {isAdmin && (
          <>
            <Dropdown.Divider />
            <Dropdown.Item
              href="/admin"
              onClick={(event) => {
                event.preventDefault();
                closeMenu();
                window.location.assign('/admin');
              }}
              className="header-admin-link"
            >
              <span className="header-admin-icon">⚙️</span> Quản trị Admin
            </Dropdown.Item>
          </>
        )}
        <Dropdown.Divider />
        <Dropdown.Item onClick={() => { closeMenu(); handleLogout(); }}>Đăng xuất</Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  ) : (
    <Button onClick={() => navigate('/login')} className="header-login-btn">Đăng nhập</Button>
  );

  return (
    <Navbar
      expand="lg"
      expanded={!isMobile ? undefined : expanded}
      onToggle={!isMobile ? undefined : setExpanded}
      className={`header ${isScrolled ? 'header--scrolled' : ''} ${isHidden ? 'header--hidden' : ''}`}
    >
      <Container fluid className="header-container">
        <Navbar.Brand as={Link} to="/" className="header-brand">
          <img src="/pictures/logo/Icon.png" alt="Cramer Logo" className="header-logo" />
        </Navbar.Brand>

        {/* Mobile: manual toggle + plain div collapse */}
        {isMobile ? (
          <>
            <button
              type="button"
              className="header-toggler navbar-toggler"
              aria-label="Toggle navigation"
              onClick={() => setExpanded(v => !v)}
            >
              <span className="navbar-toggler-icon" />
            </button>
            {expanded && (
              <div className="navbar-collapse header-mobile-menu">
                {navLinks}
                {userSection}
              </div>
            )}
          </>
        ) : (
          <>
            <Navbar.Toggle aria-controls="header-nav" className="header-toggler" />
            <Navbar.Collapse id="header-nav">
              {navLinks}
              {userSection}
            </Navbar.Collapse>
          </>
        )}
      </Container>
    </Navbar>
  );
}