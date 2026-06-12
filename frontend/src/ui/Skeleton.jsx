import { cn } from '../lib/cn';

/** Skeleton loading block (SPEC-F00 §6). */
export function Skeleton({ className, rounded = 'rounded-md' }) {
  return <span className={cn('block bg-line/70 animate-[cr-pulse_1.4s_ease-in-out_infinite]', rounded, className)} />;
}

/** Convenience: a stack of text-line skeletons. */
export function SkeletonText({ lines = 3, className }) {
  return (
    <div className={cn('flex flex-col gap-2', className)}>
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} className={cn('h-3.5', i === lines - 1 ? 'w-2/3' : 'w-full')} />
      ))}
    </div>
  );
}

export default Skeleton;
