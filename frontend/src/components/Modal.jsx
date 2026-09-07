export default function Modal({ open, title, onClose, children, wide }) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-[color:var(--color-ink)]/40 p-4 pt-12 sm:pt-20">
      <div
        className={`w-full ${wide ? "max-w-2xl" : "max-w-md"} rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] shadow-xl`}
      >
        <div className="flex items-center justify-between border-b border-[color:var(--color-border)] px-5 py-4">
          <h2 className="text-lg text-[color:var(--color-ink)]">{title}</h2>
          <button
            onClick={onClose}
            aria-label="Close"
            className="text-[color:var(--color-ink-muted)] hover:text-[color:var(--color-ink)]"
          >
            ✕
          </button>
        </div>
        <div className="px-5 py-5">{children}</div>
      </div>
    </div>
  );
}
