import { Link } from 'react-router-dom';
import { FiGithub, FiMail } from 'react-icons/fi';
import logoIcon from '../../pictures/logo/Icon.png';

const LOGO_FILTER_BRAND = 'brightness(0) saturate(100%) invert(24%) sepia(94%) saturate(2388%) hue-rotate(253deg) brightness(93%) contrast(93%)';

const LINKS = [
  { to: '/', label: 'Trang chủ' },
  { to: '/courses', label: 'Khóa học' },
  { to: '/pricing', label: 'Gói Cramer' },
  { to: '/about', label: 'Về chúng tôi' },
];

export default function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer className="site-footer border-t border-line bg-surface">
      <div className="mx-auto grid max-w-[1200px] gap-8 px-4 py-10 sm:px-6 md:grid-cols-3">
        <div>
          <Link to="/" className="flex items-center">
            <img src={logoIcon} alt="Cramer" className="h-7 w-auto" style={{ filter: LOGO_FILTER_BRAND }} />
          </Link>
          <p className="mt-3 max-w-xs text-base text-muted">
            Góc nhỏ thân thiện để luyện thi IELTS — Reading, Listening, Writing &amp; Speaking.
          </p>
        </div>

        <div>
          <h4 className="text-sm font-bold uppercase tracking-wide text-ink">Điều hướng</h4>
          <ul className="mt-3 space-y-2">
            {LINKS.map((l) => (
              <li key={l.to}>
                <Link to={l.to} className="text-base text-muted hover:text-brand-700">{l.label}</Link>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h4 className="text-sm font-bold uppercase tracking-wide text-ink">Liên hệ</h4>
          <a href="mailto:hello@cramer.vn" className="mt-3 inline-flex items-center gap-2 text-base text-muted hover:text-brand-700">
            <FiMail size={16} /> hello@cramer.vn
          </a>
          <div className="mt-3 flex items-center gap-3">
            <a href="https://github.com" target="_blank" rel="noreferrer" aria-label="GitHub" className="text-muted hover:text-brand-700">
              <FiGithub size={18} />
            </a>
          </div>
        </div>
      </div>
      <div className="border-t border-line">
        <div className="mx-auto max-w-[1200px] px-4 py-4 text-center text-sm text-faint sm:px-6">
          © {year} Cramer. Made with care for Vietnamese IELTS learners.
        </div>
      </div>
    </footer>
  );
}
