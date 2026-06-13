import { forwardRef } from 'react';
import { cn } from '../lib/cn';
import { Spinner } from './Spinner';

const VARIANTS = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 shadow-sm',
  secondary: 'bg-brand-soft text-brand-700 hover:bg-brand-100',
  outline: 'border border-line bg-surface text-ink-2 hover:bg-surface-2 hover:border-brand-300',
  ghost: 'text-ink-2 hover:bg-surface-2',
  danger: 'bg-danger text-white hover:brightness-95 shadow-sm',
  link: 'text-brand-600 hover:text-brand-700 hover:underline underline-offset-2 px-0 shadow-none',
};

const SIZES = {
  sm: 'text-sm px-3 py-1.5 gap-1.5 rounded-md',
  md: 'text-base px-4 py-2 gap-2 rounded-lg',
  lg: 'text-md px-5 py-2.5 gap-2 rounded-lg',
};

/**
 * Primary button primitive (SPEC-F00 §6). The single button in the app.
 */
export const Button = forwardRef(function Button(
  {
    variant = 'primary',
    size = 'md',
    loading = false,
    disabled = false,
    iconLeft = null,
    iconRight = null,
    fullWidth = false,
    type = 'button',
    className,
    children,
    ...props
  },
  ref
) {
  const isDisabled = disabled || loading;
  return (
    <button
      ref={ref}
      type={type}
      disabled={isDisabled}
      className={cn(
        'inline-flex items-center justify-center font-semibold transition-colors duration-150',
        'focus-visible:outline-none disabled:opacity-50 disabled:pointer-events-none whitespace-nowrap',
        VARIANTS[variant],
        SIZES[size],
        fullWidth && 'w-full',
        className
      )}
      {...props}
    >
      {loading && <Spinner size={size === 'lg' ? 'md' : 'sm'} className="text-current" />}
      {!loading && iconLeft}
      {children}
      {!loading && iconRight}
    </button>
  );
});

export default Button;
