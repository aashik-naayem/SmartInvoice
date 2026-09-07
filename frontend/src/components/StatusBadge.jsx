const STYLES = {
  PAID: "bg-[#e4f0e9] text-[color:var(--color-paid)] border-[color:var(--color-paid)]/30",
  SENT: "bg-[#eaf1f5] text-[#2b5a73] border-[#2b5a73]/25",
  OVERDUE: "bg-[#f6e8e3] text-[color:var(--color-overdue)] border-[color:var(--color-overdue)]/30",
  CANCELLED: "bg-[#efece7] text-[color:var(--color-draft)] border-[color:var(--color-draft)]/30",
  DRAFT: "bg-[#efece7] text-[color:var(--color-draft)] border-[color:var(--color-draft)]/30",
  ACTIVE: "bg-[#e4f0e9] text-[color:var(--color-paid)] border-[color:var(--color-paid)]/30",
  PAUSED: "bg-[#f3ecd9] text-[#8a6a1f] border-[#8a6a1f]/25",
  COMPLETED: "bg-[#eaf1f5] text-[#2b5a73] border-[#2b5a73]/25",
};

export default function StatusBadge({ status }) {
  const classes = STYLES[status] || STYLES.DRAFT;
  return (
    <span
      className={`inline-flex items-center rounded-sm border px-2 py-0.5 text-xs font-medium ${classes}`}
    >
      {status?.replace("_", " ")}
    </span>
  );
}
