import { useState, useEffect, useRef, useCallback } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { FiChevronDown, FiMenu, FiX, FiGrid, FiCreditCard, FiBookOpen, FiUser, FiLogOut, FiSettings } from 'react-icons/fi';
import { useAuthStore, useProfileStore } from '../stores';
import { Avatar, Button } from '../ui';
import { cn } from '../lib/cn';
import logoIcon from '../../pictures/logo/Icon.png';

const LOGO_FILTER_WHITE = 'brightness(0) invert(1)';
const LOGO_FILTER_BRAND = 'brightness(0) saturate(100%) invert(24%) sepia(94%) saturate(2388%) hue-rotate(253deg) brightness(93%) contrast(93%)';

const NAV = [
  { to: '/', label: 'Trang chủ', end: true },
  { to: '/courses', label: 'Khóa học' },
  { to: '/pricing', label: 'Gói Cramer' },
  { to: '/about', label: 'Về chúng tôi' },
];

const MENU = [
  { to: '/dashboard', label: 'Bảng điều khiển', icon: <FiGrid size={16} /> },
  { to: '/subscription', label: 'Gói đăng ký', icon: <FiCreditCard size={16} /> },
  { to: '/vocabulary', label: 'Sổ tay từ vựng', icon: <FiBookOpen size={16} /> },
  { to: '/profile', label: 'Hồ sơ', icon: <FiUser size={16} /> },
];

export default function Header() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const signOut = useAuthStore((s) => s.signOut);
  const profile = useProfileStore((s) => s.profile);

  const [scrolled, setScrolled] = useState(false);
  const [hidden, setHidden] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const lastY = useRef(0);
  const ticking = useRef(false);
  const menuRef = useRef(null);

  const onScroll = useCallback(() => {
    if (ticking.current) return;
    ticking.current = true;
    requestAnimationFrame(() => {
      const y = window.scrollY;
      setScrolled(y > 8);
      setHidden(y > 80 && y > lastY.current);
      lastY.current = y;
      ticking.current = false;
    });
  }, []);

  useEffect(() => {
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, [onScroll]);

  useEffect(() => {
    const onClick = (e) => { if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false); };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const displayName = profile?.fullName || profile?.username || user?.email?.split('@')[0] || 'Bạn';
  const isAdmin = import.meta.env.VITE_DEV_ADMIN_BYPASS === 'true' || profile?.isAdmin === true;

  const handleLogout = async () => {
    setMenuOpen(false);
    await signOut();
    navigate('/login');
  };

  const navLinkClass = ({ isActive }) =>
    cn(
      'px-3 py-2 rounded-lg text-base font-semibold transition-colors',
      scrolled
        ? (isActive ? 'text-brand-700 bg-brand-soft' : 'text-ink-2 hover:text-brand-700 hover:bg-surface-2')
        : (isActive ? 'text-white bg-white/20' : 'text-white/90 hover:text-white hover:bg-white/12')
    );

  return (
    <header
      className={cn(
        'fixed inset-x-0 top-0 transition-transform duration-300',
        hidden ? '-translate-y-full' : 'translate-y-0'
      )}
      style={{ zIndex: 'var(--z-header)', height: 'var(--header-height)' }}
    >
      <div
        className={cn(
          'h-full border-b backdrop-blur-md transition-colors duration-300',
          scrolled ? 'border-line' : 'border-white/10'
        )}
        style={{ background: scrolled ? 'rgba(255,255,255,0.95)' : 'rgba(124,58,237,0.90)' }}
      >
        <div className="mx-auto flex h-full max-w-[1200px] items-center gap-4 px-4 sm:px-6">
          <Link to="/" className="flex items-center shrink-0" aria-label="Cramer">
            <img
              src={logoIcon}
              alt="Cramer"
              className="h-7 w-auto transition-[filter] duration-300"
              style={{ filter: scrolled ? LOGO_FILTER_BRAND : LOGO_FILTER_WHITE }}
            />
          </Link>

          <nav className="ml-2 hidden items-center gap-1 lg:flex">
            {NAV.map((n) => (
              <NavLink key={n.to} to={n.to} end={n.end} className={navLinkClass}>
                {n.label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-2">
            {user ? (
              <div className="relative hidden lg:block" ref={menuRef}>
                <button
                  onClick={() => setMenuOpen((v) => !v)}
                  className={cn(
                    'flex items-center gap-2 rounded-full border py-1 pl-1 pr-2.5 transition-colors',
                    scrolled ? 'border-line bg-surface hover:bg-surface-2' : 'border-white/25 bg-white/15 hover:bg-white/25'
                  )}
                >
                  <Avatar src={profile?.avatarUrl} name={displayName} size="sm" />
                  <span className={cn('max-w-[140px] truncate text-base font-semibold', scrolled ? 'text-ink-2' : 'text-white')}>{displayName}</span>
                  <FiChevronDown size={16} className={cn('transition-transform', scrolled ? 'text-muted' : 'text-white/80', menuOpen && 'rotate-180')} />
                </button>
                {menuOpen && (
                  <div className="absolute right-0 mt-2 w-56 overflow-hidden rounded-xl border border-line bg-surface shadow-lg animate-[cr-slide-up_0.15s_ease-out]">
                    <div className="p-1">
                      {MENU.map((m) => (
                        <button
                          key={m.to}
                          onClick={() => { setMenuOpen(false); navigate(m.to); }}
                          className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-base font-medium text-ink-2 hover:bg-surface-2"
                        >
                          {m.icon}{m.label}
                        </button>
                      ))}
                    </div>
                    {isAdmin && (
                      <div className="border-t border-line p-1">
                        <button
                          onClick={() => { setMenuOpen(false); window.location.assign('/admin'); }}
                          className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-base font-medium text-brand-700 hover:bg-brand-soft"
                        >
                          <FiSettings size={16} />Quản trị Admin
                        </button>
                      </div>
                    )}
                    <div className="border-t border-line p-1">
                      <button
                        onClick={handleLogout}
                        className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-base font-medium text-danger hover:bg-danger-soft"
                      >
                        <FiLogOut size={16} />Đăng xuất
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <Button
                size="sm"
                variant={scrolled ? 'primary' : 'secondary'}
                className={cn('hidden lg:inline-flex', !scrolled && 'bg-white text-brand-700 hover:bg-white/90')}
                onClick={() => navigate('/login')}
              >
                Đăng nhập
              </Button>
            )}

            <button
              className={cn(
                'inline-flex h-9 w-9 items-center justify-center rounded-lg lg:hidden transition-colors',
                scrolled ? 'text-ink-2 hover:bg-surface-2' : 'text-white hover:bg-white/15'
              )}
              aria-label="Menu"
              onClick={() => setMobileOpen((v) => !v)}
            >
              {mobileOpen ? <FiX size={20} /> : <FiMenu size={20} />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {mobileOpen && (
        <div className="border-b border-line bg-surface lg:hidden shadow-md">
          <div className="mx-auto flex max-w-[1200px] flex-col gap-1 px-4 py-3">
            {NAV.map((n) => (
              <NavLink key={n.to} to={n.to} end={n.end} onClick={() => setMobileOpen(false)} className={navLinkClass}>
                {n.label}
              </NavLink>
            ))}
            <div className="my-1 border-t border-line" />
            {user ? (
              <>
                {MENU.map((m) => (
                  <button key={m.to} onClick={() => { setMobileOpen(false); navigate(m.to); }}
                    className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-base font-medium text-ink-2 hover:bg-surface-2">
                    {m.icon}{m.label}
                  </button>
                ))}
                {isAdmin && (
                  <button onClick={() => window.location.assign('/admin')}
                    className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-base font-medium text-brand-700 hover:bg-brand-soft">
                    <FiSettings size={16} />Quản trị Admin
                  </button>
                )}
                <button onClick={handleLogout}
                  className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-base font-medium text-danger hover:bg-danger-soft">
                  <FiLogOut size={16} />Đăng xuất
                </button>
              </>
            ) : (
              <Button fullWidth onClick={() => { setMobileOpen(false); navigate('/login'); }}>Đăng nhập</Button>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
