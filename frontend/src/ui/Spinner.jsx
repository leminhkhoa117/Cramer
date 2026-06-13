import { cn } from '../lib/cn';

const SIZES = { xs: 'h-3 w-3 border', sm: 'h-4 w-4 border-2', md: 'h-5 w-5 border-2', lg: 'h-7 w-7 border-2' };

/**
 * Spinner primitive (SPEC-F00 §6). Inherits currentColor for the visible arc.
 */
export function Spinner({ size = 'md', className, label = 'Loading' }) {
  return (
    <span
      role="status"
      aria-label={label}
      className={cn(
        'inline-block rounded-full border-current border-r-transparent animate-[cr-spin_0.6s_linear_infinite]',
        SIZES[size],
        className
      )}
    />
  );
}

export default Spinner;
