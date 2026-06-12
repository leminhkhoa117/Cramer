import { cn } from '../lib/cn';

const VARIANTS = {
  neutral: 'bg-surface-2 text-ink-2 border-line',
  brand: 'bg-brand-soft text-brand-700 border-brand-100',
  success: 'bg-success-soft text-success border-transparent',
  warning: 'bg-warning-soft text-warning border-transparent',
  danger: 'bg-danger-soft text-danger border-transparent',
  info: 'bg-info-soft text-info border-transparent',
};

const SIZES = { sm: 'text-xs px-1.5 py-0.5', md: 'text-xs px-2 py-1' };

/** Badge / status pill primitive (SPEC-F00 §6). */
export function Badge({ variant = 'neutral', size = 'md', dot = false, className, children, ...props }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full border font-semibold leading-none whitespace-nowrap',
        VARIANTS[variant],
        SIZES[size],
        className
      )}
      {...props}
    >
      {dot && <span className="h-1.5 w-1.5 rounded-full bg-current" />}
      {children}
    </span>
  );
}

export default Badge;
