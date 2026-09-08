import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as invoicesApi from "../api/invoices";
import Button from "../components/Button";
import StatusBadge from "../components/StatusBadge";
import { useToast } from "../context/ToastContext";
import { formatDate, formatMoney } from "../lib/format";

export default function InvoicesPage() {
  const { showError } = useToast();
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    invoicesApi
      .listInvoices()
      .then(setInvoices)
      .catch(() => showError("Could not load invoices."))
      .finally(() => setLoading(false));
  }, [showError]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl">Invoices</h1>
          <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">
            Every bill you've sent, and what's still owed.
          </p>
        </div>
        <Link to="/invoices/new">
          <Button>New invoice</Button>
        </Link>
      </div>

      <div className="overflow-x-auto rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)]">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[color:var(--color-border)] text-left text-xs uppercase tracking-wide text-[color:var(--color-ink-muted)]">
              <th className="px-4 py-3 font-medium">Invoice</th>
              <th className="px-4 py-3 font-medium">Client</th>
              <th className="px-4 py-3 font-medium">Due</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 text-right font-medium">Total</th>
              <th className="px-4 py-3 text-right font-medium">Balance</th>
            </tr>
          </thead>
          <tbody>
            {invoices.map((inv) => (
              <tr key={inv.id} className="border-b border-[color:var(--color-border)] last:border-0">
                <td className="px-4 py-3">
                  <Link
                    to={`/invoices/${inv.id}`}
                    className="font-medium text-[color:var(--color-pine)] hover:underline"
                  >
                    {inv.invoiceNumber}
                  </Link>
                </td>
                <td className="px-4 py-3">{inv.clientName}</td>
                <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">
                  {formatDate(inv.dueDate)}
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={inv.status} />
                </td>
                <td className="tabular px-4 py-3 text-right">
                  {formatMoney(inv.totalAmount, inv.currency)}
                </td>
                <td className="tabular px-4 py-3 text-right">
                  {formatMoney(inv.balanceDue, inv.currency)}
                </td>
              </tr>
            ))}
            {!loading && invoices.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[color:var(--color-ink-muted)]">
                  No invoices yet. Create your first one.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
