import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { cn } from '../lib/cn';

/** Build a compact page list with ellipses. `page` is 0-based. */
function pageWindow(page, totalPages) {
  const cur = page + 1;
  const items = [];
  const push = (v) => items.push(v);
  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) push(i);
    return items;
  }
  push(1);
  if (cur > 3) push('…');
  for (let i = Math.max(2, cur - 1); i <= Math.min(totalPages - 1, cur + 1); i++) push(i);
  if (cur < totalPages - 2) push('…');
  push(totalPages);
  return items;
}

/**
 * Pagination primitive (SPEC-F00 §6). `page` 0-based; emits 0-based onPageChange.
 */
export function Pagination({ page = 0, totalPages = 1, onPageChange, className }) {
  if (totalPages <= 1) return null;
  const go = (p) => p >= 0 && p < totalPages && p !== page && onPageChange?.(p);
  const btn = 'inline-flex h-8 min-w-8 items-center justify-center rounded-md px-2 text-base font-semibold transition-colors disabled:opacity-40 disabled:pointer-events-none';
  return (
    <nav className={cn('flex items-center justify-center gap-1', className)} aria-label="Pagination">
      <button className={cn(btn, 'text-ink-2 hover:bg-surface-2')} onClick={() => go(page - 1)} disabled={page === 0} aria-label="Previous">
        <FiChevronLeft size={16} />
      </button>
      {pageWindow(page, totalPages).map((it, i) =>
        it === '…' ? (
          <span key={`e${i}`} className="px-1 text-muted">…</span>
        ) : (
          <button
            key={it}
            onClick={() => go(it - 1)}
            aria-current={it - 1 === page ? 'page' : undefined}
            className={cn(btn, it - 1 === page ? 'bg-brand-600 text-white' : 'text-ink-2 hover:bg-surface-2')}
          >
            {it}
          </button>
        )
      )}
      <button className={cn(btn, 'text-ink-2 hover:bg-surface-2')} onClick={() => go(page + 1)} disabled={page === totalPages - 1} aria-label="Next">
        <FiChevronRight size={16} />
      </button>
    </nav>
  );
}

export default Pagination;
