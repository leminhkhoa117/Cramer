import { cn } from '../lib/cn';
import { Card } from './Card';

/**
 * Stat / metric card (SPEC-F00 §6). label + value + optional icon, delta, hint.
 */
export function StatCard({ icon, label, value, delta, hint, className }) {
  return (
    <Card padded className={cn('flex h-full items-center gap-3', className)}>
      {icon && (
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-brand-soft text-brand-600 text-lg">
          {icon}
        </div>
      )}
      <div className="min-w-0">
        <div className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</div>
        <div className="mt-0.5 text-2xl font-bold text-ink leading-none">{value}</div>
        {(delta || hint) && (
          <div className="mt-1 text-xs text-muted">
            {delta && <span className={cn('font-semibold', delta.positive ? 'text-success' : 'text-danger')}>{delta.text} </span>}
            {hint}
          </div>
        )}
      </div>
    </Card>
  );
}

export default StatCard;
