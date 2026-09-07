import { useEffect, useState } from "react";
import { getDashboard } from "../api/analytics";
import RevenueBars from "../components/RevenueBars";
import StatCard from "../components/StatCard";
import StatusBadge from "../components/StatusBadge";
import { formatDate, formatMoney } from "../lib/format";

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getDashboard()
      .then(setData)
      .catch(() => setError("Could not load the dashboard right now."));
  }, []);

  if (error) {
    return <p className="text-sm text-[color:var(--color-overdue)]">{error}</p>;
  }

  if (!data) {
    return <p className="text-sm text-[color:var(--color-ink-muted)]">Loading ledger…</p>;
  }

  const statusEntries = Object.entries(data.invoiceCountsByStatus || {});

  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="text-2xl">Dashboard</h1>
        <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">
          Where your billing stands right now.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Total revenue" value={formatMoney(data.totalRevenue)} />
        <StatCard label="Outstanding" value={formatMoney(data.outstandingBalance)} />
        <StatCard
          label="Overdue"
          value={formatMoney(data.overdueBalance)}
          accent="var(--color-overdue)"
        />
        <StatCard label="Clients" value={data.totalClients} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] p-5 lg:col-span-2">
          <h2 className="text-base">Revenue, last 12 months</h2>
          <div className="mt-4">
            <RevenueBars points={data.monthlyRevenue || []} />
          </div>
        </div>

        <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] p-5">
          <h2 className="text-base">Invoices by status</h2>
          <ul className="mt-4 flex flex-col gap-3">
            {statusEntries.map(([status, count]) => (
              <li key={status} className="flex items-center justify-between text-sm">
                <StatusBadge status={status} />
                <span className="tabular text-[color:var(--color-ink-muted)]">{count}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] p-5">
          <h2 className="text-base">Top clients</h2>
          {data.topClients?.length ? (
            <table className="mt-4 w-full text-sm">
              <tbody>
                {data.topClients.map((c) => (
                  <tr key={c.clientId} className="border-t border-[color:var(--color-border)]">
                    <td className="py-2">{c.clientName}</td>
                    <td className="tabular py-2 text-right">{formatMoney(c.totalBilled)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="mt-4 text-sm text-[color:var(--color-ink-muted)]">
              No billed clients yet.
            </p>
          )}
        </div>

        <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] p-5">
          <h2 className="text-base">Recent payments</h2>
          {data.recentPayments?.length ? (
            <table className="mt-4 w-full text-sm">
              <tbody>
                {data.recentPayments.map((p) => (
                  <tr key={p.paymentId} className="border-t border-[color:var(--color-border)]">
                    <td className="py-2">
                      <p>{p.clientName}</p>
                      <p className="text-xs text-[color:var(--color-ink-muted)]">
                        {p.invoiceNumber} · {formatDate(p.paymentDate)}
                      </p>
                    </td>
                    <td className="tabular py-2 text-right">{formatMoney(p.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="mt-4 text-sm text-[color:var(--color-ink-muted)]">
              No payments recorded yet.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
