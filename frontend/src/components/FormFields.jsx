const fieldClasses =
  "w-full rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] px-3 py-2 text-sm text-[color:var(--color-ink)] outline-none focus:border-[color:var(--color-pine)]";

export function Field({ label, htmlFor, children, hint }) {
  return (
    <label htmlFor={htmlFor} className="flex flex-col gap-1.5">
      <span className="text-sm font-medium text-[color:var(--color-ink)]">{label}</span>
      {children}
      {hint && <span className="text-xs text-[color:var(--color-ink-muted)]">{hint}</span>}
    </label>
  );
}

export function Input({ className = "", ...props }) {
  return <input className={`${fieldClasses} ${className}`} {...props} />;
}

export function Select({ className = "", children, ...props }) {
  return (
    <select className={`${fieldClasses} ${className}`} {...props}>
      {children}
    </select>
  );
}

export function Textarea({ className = "", ...props }) {
  return <textarea className={`${fieldClasses} ${className}`} {...props} />;
}
