export default function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="min-h-screen bg-[color:var(--color-paper)] text-[color:var(--color-ink)] lg:flex">
      <div className="hidden flex-1 flex-col justify-between border-r border-[color:var(--color-border)] px-12 py-10 lg:flex">
        <p className="font-serif text-xl">SmartInvoice</p>
        <div className="max-w-sm">
          <p className="font-serif text-3xl leading-snug">
            Every invoice, payment, and overdue balance — one ledger.
          </p>
          <p className="mt-4 text-sm text-[color:var(--color-ink-muted)]">
            Track client billing, chase payments automatically, and see exactly where your
            business stands.
          </p>
        </div>
        <p className="text-xs text-[color:var(--color-ink-muted)]">SmartInvoice &copy; 2026</p>
      </div>

      <div className="flex flex-1 items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <p className="font-serif text-xl lg:hidden">SmartInvoice</p>
          <h1 className="mt-6 font-serif text-2xl">{title}</h1>
          {subtitle && (
            <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">{subtitle}</p>
          )}
          <div className="mt-6">{children}</div>
        </div>
      </div>
    </div>
  );
}
