const VARIANTS = {
  primary: "bg-[color:var(--color-pine)] text-white hover:bg-[color:var(--color-pine-dark)]",
  secondary:
    "bg-transparent text-[color:var(--color-ink)] border border-[color:var(--color-border)] hover:border-[color:var(--color-pine)]",
  ghost: "bg-transparent text-[color:var(--color-ink-muted)] hover:text-[color:var(--color-ink)]",
  danger: "bg-transparent text-[color:var(--color-overdue)] hover:bg-[#f6e8e3]",
  dangerSolid: "bg-[color:var(--color-overdue)] text-white hover:opacity-90",
};

export default function Button({
  children,
  variant = "primary",
  className = "",
  disabled,
  ...props
}) {
  return (
    <button
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-1.5 rounded-sm px-3.5 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${VARIANTS[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
