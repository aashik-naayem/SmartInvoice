import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import * as publicInvoiceApi from "../api/publicInvoice";
import Button from "../components/Button";
import StatusBadge from "../components/StatusBadge";
import { formatDate, formatMoney } from "../lib/format";

export default function PublicInvoicePage() {
  const { token } = useParams();
  const [invoice, setInvoice] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    publicInvoiceApi
      .getPublicInvoice(token)
      .then(setInvoice)
      .catch(() => setError("This invoice link is invalid or has expired."));
  }, [token]);

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[color:var(--color-paper)] px-6">
        <p className="text-sm text-[color:var(--color-overdue)]">{error}</p>
      </div>
    );
  }

  if (!invoice) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[color:var(--color-paper)] px-6">
        <p className="text-sm text-[color:var(--color-ink-muted)]">Loading…</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[color:var(--color-paper)] px-6 py-10 text-[color:var(--color-ink)]">
      <div className="mx-auto max-w-3xl">
        <p className="font-serif text-xl">SmartInvoice</p>

        <div className="mt-8 flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl">{invoice.invoiceNumber}</h1>
              <StatusBadge status={invoice.status} />
            </div>
            <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">
              From {invoice.issuerName} ({invoice.issuerEmail}) to {invoice.clientName}
            </p>
            <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">
              Issued {formatDate(invoice.issueDate)} · Due {formatDate(invoice.dueDate)}
            </p>
          </div>

          <Button
            variant="secondary"
            onClick={() =>
              publicInvoiceApi
                .downloadPublicInvoicePdf(token, invoice.invoiceNumber)
                .catch(() => setError("Could not download the PDF right now."))
            }
          >
            Download PDF
          </Button>
        </div>

        <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] lg:col-span-2">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[color:var(--color-border)] text-left text-xs uppercase tracking-wide text-[color:var(--color-ink-muted)]">
                  <th className="px-4 py-3 font-medium">Description</th>
                  <th className="px-4 py-3 text-right font-medium">Qty</th>
                  <th className="px-4 py-3 text-right font-medium">Unit price</th>
                  <th className="px-4 py-3 text-right font-medium">Line total</th>
                </tr>
              </thead>
              <tbody>
                {invoice.items?.map((item) => (
                  <tr key={item.id} className="border-b border-[color:var(--color-border)] last:border-0">
                    <td className="px-4 py-3">{item.description}</td>
                    <td className="tabular px-4 py-3 text-right">{item.quantity}</td>
                    <td className="tabular px-4 py-3 text-right">
                      {formatMoney(item.unitPrice, invoice.currency)}
                    </td>
                    <td className="tabular px-4 py-3 text-right">
                      {formatMoney(item.lineTotal, invoice.currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {invoice.notes && (
              <div className="border-t border-[color:var(--color-border)] px-4 py-3 text-sm text-[color:var(--color-ink-muted)]">
                {invoice.notes}
              </div>
            )}
          </div>

          <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] p-4 text-sm">
            <div className="flex justify-between">
              <span className="text-[color:var(--color-ink-muted)]">Subtotal</span>
              <span className="tabular">{formatMoney(invoice.subtotal, invoice.currency)}</span>
            </div>
            <div className="mt-1 flex justify-between">
              <span className="text-[color:var(--color-ink-muted)]">
                Tax ({Number(invoice.taxRate)}%)
              </span>
              <span className="tabular">{formatMoney(invoice.taxAmount, invoice.currency)}</span>
            </div>
            <div className="mt-2 flex justify-between border-t border-[color:var(--color-border)] pt-2 font-medium">
              <span>Total</span>
              <span className="tabular">{formatMoney(invoice.totalAmount, invoice.currency)}</span>
            </div>
            <div className="mt-1 flex justify-between text-[color:var(--color-paid)]">
              <span>Paid</span>
              <span className="tabular">{formatMoney(invoice.amountPaid, invoice.currency)}</span>
            </div>
            <div className="mt-1 flex justify-between text-[color:var(--color-overdue)]">
              <span>Balance due</span>
              <span className="tabular">{formatMoney(invoice.balanceDue, invoice.currency)}</span>
            </div>
          </div>
        </div>

        <p className="mt-10 text-center text-xs text-[color:var(--color-ink-muted)]">
          Generated by SmartInvoice
        </p>
      </div>
    </div>
  );
}
