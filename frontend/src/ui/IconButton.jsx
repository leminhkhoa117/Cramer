import { forwardRef } from 'react';
import { cn } from '../lib/cn';
import { Spinner } from './Spinner';

const VARIANTS = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 shadow-sm',
  secondary: 'bg-brand-soft text-brand-700 hover:bg-brand-100',
  outline: 'border border-line bg-surface text-ink-2 hover:bg-surface-2 hover:border-brand-300',
  ghost: 'text-ink-2 hover:bg-surface-2',
  danger: 'bg-danger text-white hover:brightness-95',
};

const SIZES = { sm: 'h-8 w-8 text-base rounded-md', md: 'h-9 w-9 text-md rounded-lg', lg: 'h-11 w-11 text-lg rounded-lg' };

/**
 * Square icon-only button (SPEC-F00 §6). Requires an aria-label.
 */
export const IconButton = forwardRef(function IconButton(
  { variant = 'ghost', size = 'md', loading = false, disabled = false, className, children, 'aria-label': ariaLabel, ...props },
  ref
) {
  return (
    <button
      ref={ref}
      type="button"
      aria-label={ariaLabel}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center transition-colors duration-150 shrink-0',
        'focus-visible:outline-none disabled:opacity-50 disabled:pointer-events-none',
        VARIANTS[variant],
        SIZES[size],
        className
      )}
      {...props}
    >
      {loading ? <Spinner size="sm" className="text-current" /> : children}
    </button>
  );
});

export default IconButton;
