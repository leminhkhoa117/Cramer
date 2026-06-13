import { cn } from '../lib/cn';

/** Progress bar (SPEC-F00 §6). `value` 0–100. */
export function Progress({ value = 0, max = 100, className, barClassName, label }) {
  const pct = Math.max(0, Math.min(100, (value / max) * 100));
  return (
    <div className={cn('w-full', className)}>
      {label && (
        <div className="mb-1 flex items-center justify-between text-xs text-muted">
          <span>{label}</span>
          <span className="font-semibold text-ink-2">{Math.round(pct)}%</span>
        </div>
      )}
      <div className="h-2 w-full overflow-hidden rounded-full bg-line">
        <div
          className={cn('h-full rounded-full bg-brand-600 transition-[width] duration-500', barClassName)}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

export default Progress;
