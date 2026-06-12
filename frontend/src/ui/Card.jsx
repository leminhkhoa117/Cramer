import { cn } from '../lib/cn';

const VARIANTS = {
  solid: 'bg-surface border border-line shadow-sm',
  glass: 'glass',
  outline: 'bg-transparent border border-line',
  ghost: 'bg-surface-2 border border-transparent',
};

/**
 * Card primitive (SPEC-F00 §6). Default radius xl, padding via `padded`.
 */
export function Card({ variant = 'solid', interactive = false, padded = true, className, children, ...props }) {
  return (
    <div
      className={cn(
        'rounded-xl',
        VARIANTS[variant],
        padded && 'p-5',
        interactive && 'transition-shadow duration-150 hover:shadow-md cursor-pointer',
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}

Card.Header = function CardHeader({ className, children, ...props }) {
  return (
    <div className={cn('flex items-center justify-between gap-3 pb-3 mb-3 border-b border-line', className)} {...props}>
      {children}
    </div>
  );
};

Card.Title = function CardTitle({ className, children, ...props }) {
  return <h3 className={cn('text-lg font-bold text-ink', className)} {...props}>{children}</h3>;
};

Card.Body = function CardBody({ className, children, ...props }) {
  return <div className={cn(className)} {...props}>{children}</div>;
};

Card.Footer = function CardFooter({ className, children, ...props }) {
  return (
    <div className={cn('flex items-center justify-end gap-2 pt-3 mt-3 border-t border-line', className)} {...props}>
      {children}
    </div>
  );
};

export default Card;
