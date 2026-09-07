import { formatMonth } from "../lib/format";

export default function RevenueBars({ points }) {
  const max = Math.max(1, ...points.map((p) => Number(p.amount)));

  return (
    <div className="flex h-40 items-end gap-2">
      {points.map((point) => {
        const heightPct = Math.max(2, (Number(point.amount) / max) * 100);
        return (
          <div key={point.month} className="flex flex-1 flex-col items-center gap-2">
            <div className="flex h-32 w-full items-end">
              <div
                className="w-full rounded-t-sm bg-[color:var(--color-pine)]/85"
                style={{ height: `${heightPct}%` }}
                title={`${formatMonth(point.month)}: ${point.amount}`}
              />
            </div>
            <span className="text-[10px] text-[color:var(--color-ink-muted)]">
              {formatMonth(point.month)}
            </span>
          </div>
        );
      })}
    </div>
  );
}
