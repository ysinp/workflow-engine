import { ReactNode } from 'react';

interface FieldProps {
  id: string;
  label: string;
  error?: string;
  hint?: ReactNode;
  optional?: boolean;
  span?: 1 | 2;
  children: ReactNode;
}

/**
 * Label + control + hint/error wrapper used by every form. The control itself
 * is passed as children so native <input>/<select> semantics are preserved;
 * callers should set `aria-invalid` and `aria-describedby={`${id}-msg`}`.
 */
export default function Field({ id, label, error, hint, optional, span = 1, children }: FieldProps) {
  return (
    <div className={`field${span === 2 ? ' field-span-2' : ''}${error ? ' field-invalid' : ''}`}>
      <label htmlFor={id} className="field-label">
        {label}
        {optional && <span className="field-optional">Optional</span>}
      </label>
      {children}
      {error ? (
        <p id={`${id}-msg`} className="field-error" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p id={`${id}-msg`} className="field-hint">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
