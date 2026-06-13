import { cn } from '../lib/cn';

/**
 * Sidebar layout primitive (SPEC-F00 §6) — replaces the legacy `.sl-*` system.
 * Sticky left sidebar (280–300px) + fluid content. Stacks on mobile.
 */
export function SidebarLayout({ sidebar, children, className }) {
  return (
    <div className={cn('flex flex-col gap-5 lg:flex-row lg:items-start', className)}>
      <aside className="w-full lg:w-72 lg:shrink-0 lg:sticky lg:top-[calc(var(--header-height)+1rem)]">
        {sidebar}
      </aside>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}

export default SidebarLayout;
