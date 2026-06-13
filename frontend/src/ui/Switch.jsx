import { cn } from '../lib/cn';

/**
 * Toggle switch primitive (SPEC-F00 §6). Replaces the legacy ToggleSwitch component.
 */
export function Switch({ checked = false, onChange, disabled = false, label, description, id, className }) {
  return (
    <label
      htmlFor={id}
      className={cn('flex items-center gap-3', disabled ? 'opacity-60' : 'cursor-pointer', className)}
    >
      <button
        type="button"
        role="switch"
        id={id}
        aria-checked={checked}
        disabled={disabled}
        onClick={() => !disabled && onChange?.(!checked)}
        className={cn(
          'relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors duration-150',
          'focus-visible:outline-none disabled:pointer-events-none',
          checked ? 'bg-brand-600' : 'bg-line'
        )}
      >
        <span
          className={cn(
            'inline-block h-5 w-5 transform rounded-full bg-white shadow-sm transition-transform duration-150',
            checked ? 'translate-x-[22px]' : 'translate-x-0.5'
          )}
        />
      </button>
      {(label || description) && (
        <span className="flex flex-col">
          {label && <span className="text-base font-semibold text-ink-2">{label}</span>}
          {description && <span className="text-xs text-muted">{description}</span>}
        </span>
      )}
    </label>
  );
}

export default Switch;
