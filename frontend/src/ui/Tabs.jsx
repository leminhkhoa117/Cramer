import { cn } from '../lib/cn';

/**
 * Tabs primitive (SPEC-F00 §6). Controlled: pass items [{value,label,icon?,badge?}], active, onChange.
 */
export function Tabs({ items = [], value, onChange, variant = 'underline', className }) {
  if (variant === 'pill') {
    return (
      <div className={cn('inline-flex items-center gap-1 rounded-lg bg-surface-2 p-1', className)} role="tablist">
        {items.map((it) => {
          const active = it.value === value;
          return (
            <button
              key={it.value}
              role="tab"
              aria-selected={active}
              onClick={() => onChange?.(it.value)}
              className={cn(
                'inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-base font-semibold transition-colors',
                active ? 'bg-surface text-brand-700 shadow-xs' : 'text-muted hover:text-ink-2'
              )}
            >
              {it.icon}
              {it.label}
              {it.badge != null && (
                <span className="ml-0.5 rounded-full bg-brand-100 px-1.5 text-xs text-brand-700">{it.badge}</span>
              )}
            </button>
          );
        })}
      </div>
    );
  }
  return (
    <div className={cn('flex items-center gap-1 border-b border-line', className)} role="tablist">
      {items.map((it) => {
        const active = it.value === value;
        return (
          <button
            key={it.value}
            role="tab"
            aria-selected={active}
            onClick={() => onChange?.(it.value)}
            className={cn(
              'inline-flex items-center gap-1.5 px-3 py-2.5 text-base font-semibold transition-colors -mb-px border-b-2',
              active ? 'border-brand-600 text-brand-700' : 'border-transparent text-muted hover:text-ink-2'
            )}
          >
            {it.icon}
            {it.label}
            {it.badge != null && (
              <span className="ml-0.5 rounded-full bg-surface-2 px-1.5 text-xs text-ink-2">{it.badge}</span>
            )}
          </button>
        );
      })}
    </div>
  );
}

export default Tabs;
