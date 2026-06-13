import { cn } from '../lib/cn';

const SIZES = { xs: 'h-6 w-6 text-xs', sm: 'h-8 w-8 text-sm', md: 'h-10 w-10 text-base', lg: 'h-14 w-14 text-lg', xl: 'h-20 w-20 text-2xl' };

function initials(name = '') {
  const parts = String(name).trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/** Avatar primitive (SPEC-F00 §6): image with initials fallback. */
export function Avatar({ src, name, size = 'md', className }) {
  return (
    <span
      className={cn(
        'inline-flex items-center justify-center overflow-hidden rounded-full bg-brand-100 text-brand-700 font-bold shrink-0 select-none',
        SIZES[size],
        className
      )}
    >
      {src ? (
        <img src={src} alt={name || 'avatar'} className="h-full w-full object-cover" />
      ) : (
        <span>{initials(name)}</span>
      )}
    </span>
  );
}

export default Avatar;
