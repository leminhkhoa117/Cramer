import { forwardRef, useId } from 'react';
import { cn } from '../lib/cn';

const baseField =
  'w-full rounded-lg border bg-surface text-ink placeholder:text-faint transition-colors duration-150 ' +
  'focus-visible:outline-none focus:border-brand-400 focus:shadow-[0_0_0_3px_rgba(124,58,237,0.18)] ' +
  'disabled:opacity-60 disabled:bg-surface-2';

function FieldShell({ label, hint, error, required, htmlFor, className, children }) {
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      {label && (
        <label htmlFor={htmlFor} className="text-sm font-semibold text-ink-2">
          {label}
          {required && <span className="text-danger ml-0.5">*</span>}
        </label>
      )}
      {children}
      {error ? (
        <span className="text-xs text-danger">{error}</span>
      ) : hint ? (
        <span className="text-xs text-muted">{hint}</span>
      ) : null}
    </div>
  );
}

/** Text input primitive (SPEC-F00 §6). */
export const Input = forwardRef(function Input(
  { label, hint, error, required, className, inputClassName, iconLeft, type = 'text', id, ...props },
  ref
) {
  const autoId = useId();
  const fieldId = id || autoId;
  return (
    <FieldShell label={label} hint={hint} error={error} required={required} htmlFor={fieldId} className={className}>
      <div className="relative flex items-center">
        {iconLeft && <span className="absolute left-3 text-faint pointer-events-none">{iconLeft}</span>}
        <input
          ref={ref}
          id={fieldId}
          type={type}
          className={cn(
            baseField,
            'h-[38px] px-3 text-base',
            iconLeft && 'pl-9',
            error && 'border-danger focus:border-danger focus:shadow-[0_0_0_3px_rgba(239,68,68,0.18)]',
            !error && 'border-line',
            inputClassName
          )}
          {...props}
        />
      </div>
    </FieldShell>
  );
});

/** Textarea primitive. */
export const Textarea = forwardRef(function Textarea(
  { label, hint, error, required, className, textareaClassName, rows = 4, id, ...props },
  ref
) {
  const autoId = useId();
  const fieldId = id || autoId;
  return (
    <FieldShell label={label} hint={hint} error={error} required={required} htmlFor={fieldId} className={className}>
      <textarea
        ref={ref}
        id={fieldId}
        rows={rows}
        className={cn(
          baseField,
          'px-3 py-2 text-base resize-y leading-relaxed',
          error ? 'border-danger' : 'border-line',
          textareaClassName
        )}
        {...props}
      />
    </FieldShell>
  );
});

/** Select primitive. */
export const Select = forwardRef(function Select(
  { label, hint, error, required, className, selectClassName, children, id, ...props },
  ref
) {
  const autoId = useId();
  const fieldId = id || autoId;
  return (
    <FieldShell label={label} hint={hint} error={error} required={required} htmlFor={fieldId} className={className}>
      <select
        ref={ref}
        id={fieldId}
        className={cn(
          baseField,
          'h-[38px] px-3 text-base appearance-none bg-no-repeat pr-9',
          error ? 'border-danger' : 'border-line',
          selectClassName
        )}
        style={{
          backgroundImage:
            "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='none' stroke='%2364748b' stroke-width='2' viewBox='0 0 24 24'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E\")",
          backgroundPosition: 'right 0.75rem center',
        }}
        {...props}
      >
        {children}
      </select>
    </FieldShell>
  );
});

export default Input;
