import { cn } from '../lib/cn';

/**
 * Empty / zero-data state (SPEC-F00 §6). Use instead of ad-hoc "no data" markup.
 */
export function EmptyState({ icon, title, description, action, className }) {
  return (
    <div className={cn('flex flex-col items-center justify-center text-center py-12 px-4', className)}>
      {icon && (
        <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-brand-soft text-brand-500 text-2xl">
          {icon}
        </div>
      )}
      {title && <h3 className="text-lg font-bold text-ink">{title}</h3>}
      {description && <p className="mt-1 max-w-md text-base text-muted">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export default EmptyState;
