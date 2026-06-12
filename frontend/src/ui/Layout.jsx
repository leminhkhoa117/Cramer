import { cn } from '../lib/cn';

/** Page background wrapper (SPEC-F00 §6). */
export function Page({ className, children, ...props }) {
  return (
    <div className={cn('min-h-[calc(100vh-var(--header-height))] bg-page', className)} {...props}>
      {children}
    </div>
  );
}

/** Centered max-width container with responsive gutters. */
export function Container({ size = 'default', className, children, ...props }) {
  const max = size === 'narrow' ? 'max-w-3xl' : size === 'wide' ? 'max-w-[1320px]' : 'max-w-[1200px]';
  return (
    <div className={cn('mx-auto w-full px-4 sm:px-6', max, className)} {...props}>
      {children}
    </div>
  );
}

/** Vertical section rhythm (dense: ≤64px desktop). */
export function Section({ className, children, ...props }) {
  return (
    <section className={cn('py-8 md:py-12', className)} {...props}>
      {children}
    </section>
  );
}

/** Standard page header: title + subtitle + actions. */
export function PageHeader({ title, subtitle, actions, breadcrumbs, className }) {
  return (
    <div className={cn('flex flex-col gap-3 md:flex-row md:items-end md:justify-between', className)}>
      <div className="min-w-0">
        {breadcrumbs && <div className="mb-1 text-sm text-muted">{breadcrumbs}</div>}
        <h1 className="text-2xl font-bold text-ink truncate">{title}</h1>
        {subtitle && <p className="mt-1 text-base text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex items-center gap-2 shrink-0">{actions}</div>}
    </div>
  );
}

export default Page;
