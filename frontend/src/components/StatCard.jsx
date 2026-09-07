export default function StatCard({ label, value, accent, footnote }) {
  return (
    <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] px-5 py-4">
      <p className="text-xs uppercase tracking-wide text-[color:var(--color-ink-muted)]">
        {label}
      </p>
      <p
        className="tabular mt-2 font-serif text-2xl"
        style={accent ? { color: accent } : undefined}
      >
        {value}
      </p>
      {footnote && (
        <p className="mt-1 text-xs text-[color:var(--color-ink-muted)]">{footnote}</p>
      )}
    </div>
  );
}
