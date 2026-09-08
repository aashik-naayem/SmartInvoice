import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { apiErrorMessage } from "../api/client";
import * as invoicesApi from "../api/invoices";
import * as paymentsApi from "../api/payments";
import Button from "../components/Button";
import { Field, Input, Select } from "../components/FormFields";
import StatusBadge from "../components/StatusBadge";
import { useConfirm } from "../context/ConfirmContext";
import { useToast } from "../context/ToastContext";
import { formatDate, formatMoney } from "../lib/format";

const STATUS_OPTIONS = ["DRAFT", "SENT", "PAID", "OVERDUE", "CANCELLED"];
const PAYMENT_METHODS = ["CASH", "BANK_TRANSFER", "CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "OTHER"];

const EMPTY_PAYMENT = {
  amount: "",
  paymentDate: new Date().toISOString().slice(0, 10),
  method: "BANK_TRANSFER",
  reference: "",
  notes: "",
};

export default function InvoiceDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const confirm = useConfirm();
  const { showSuccess, showError } = useToast();
  const [invoice, setInvoice] = useState(null);
  const [payments, setPayments] = useState([]);
  const [error, setError] = useState("");
  const [paymentForm, setPaymentForm] = useState(EMPTY_PAYMENT);
  const [paymentError, setPaymentError] = useState("");
  const [savingPayment, setSavingPayment] = useState(false);
  const [sending, setSending] = useState(false);

  const load = () => {
    invoicesApi
      .getInvoice(id)
      .then(setInvoice)
      .catch(() => setError("Could not load this invoice."));
    paymentsApi.listPayments(id).then(setPayments).catch(() => {});
  };

  useEffect(load, [id]);

  const handleStatusChange = async (status) => {
    try {
      const updated = await invoicesApi.updateInvoiceStatus(id, status);
      setInvoice(updated);
      showSuccess(`Status updated to ${status}.`);
    } catch (err) {
      showError(apiErrorMessage(err, "Could not update the status."));
    }
  };

  const handleSend = async () => {
    setSending(true);
    try {
      const updated = await invoicesApi.sendInvoice(id);
      setInvoice(updated);
      showSuccess("Invoice sent to the client.");
    } catch (err) {
      showError(apiErrorMessage(err, "Could not send this invoice."));
    } finally {
      setSending(false);
    }
  };

  const handleCopyLink = async (shareLink) => {
    try {
      await navigator.clipboard.writeText(shareLink);
      showSuccess("Link copied.");
    } catch {
      showError("Could not copy the link.");
    }
  };

  const handleDelete = async () => {
    const ok = await confirm({
      title: "Delete invoice",
      message: "Delete this invoice? This can't be undone.",
      confirmLabel: "Delete",
    });
    if (!ok) return;
    try {
      await invoicesApi.deleteInvoice(id);
      showSuccess("Invoice deleted.");
      navigate("/invoices");
    } catch (err) {
      showError(apiErrorMessage(err, "Could not delete this invoice."));
    }
  };

  const handleAddPayment = async (e) => {
    e.preventDefault();
    setPaymentError("");
    setSavingPayment(true);
    try {
      await paymentsApi.createPayment(id, {
        ...paymentForm,
        amount: Number(paymentForm.amount),
      });
      setPaymentForm(EMPTY_PAYMENT);
      showSuccess("Payment recorded.");
      load();
    } catch (err) {
      setPaymentError(apiErrorMessage(err, "Could not record this payment."));
    } finally {
      setSavingPayment(false);
    }
  };

  const handleDeletePayment = async (paymentId) => {
    const ok = await confirm({
      title: "Remove payment",
      message: "Remove this payment? This can't be undone.",
      confirmLabel: "Remove",
    });
    if (!ok) return;
    try {
      await paymentsApi.deletePayment(id, paymentId);
      showSuccess("Payment removed.");
      load();
    } catch (err) {
      showError(apiErrorMessage(err, "Could not remove this payment."));
    }
  };

  if (error) return <p className="text-sm text-[color:var(--color-overdue)]">{error}</p>;
  if (!invoice) return <p className="text-sm text-[color:var(--color-ink-muted)]">Loading…</p>;

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl">{invoice.invoiceNumber}</h1>
            <StatusBadge status={invoice.status} />
          </div>
          <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">
            {invoice.clientName} · Issued {formatDate(invoice.issueDate)} · Due{" "}
            {formatDate(invoice.dueDate)}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Select
            value={invoice.status}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="w-auto"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
          <Button
            variant="secondary"
            onClick={() =>
              invoicesApi
                .downloadInvoicePdf(id, invoice.invoiceNumber)
                .catch(() => showError("Could not generate the PDF."))
            }
          >
            Download PDF
          </Button>
          <Button onClick={handleSend} disabled={sending}>
            {sending ? "Sending…" : invoice.sentAt ? "Resend to client" : "Send to client"}
          </Button>
          <Button variant="danger" onClick={handleDelete}>
            Delete
          </Button>
        </div>
      </div>

      {invoice.publicToken && (
        <div className="flex flex-wrap items-center gap-3 rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] px-4 py-3 text-sm">
          <span className="text-[color:var(--color-ink-muted)]">Client link</span>
          <code className="flex-1 truncate text-xs">
            {window.location.origin}/invoice/{invoice.publicToken}
          </code>
          <button
            onClick={() => handleCopyLink(`${window.location.origin}/invoice/${invoice.publicToken}`)}
            className="text-xs font-medium text-[color:var(--color-pine)] hover:underline"
          >
            Copy link
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
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

      <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] p-5">
        <h2 className="text-base">Payments</h2>

        {payments.length > 0 && (
          <table className="mt-4 w-full text-sm">
            <tbody>
              {payments.map((p) => (
                <tr key={p.id} className="border-b border-[color:var(--color-border)] last:border-0">
                  <td className="py-2">{formatDate(p.paymentDate)}</td>
                  <td className="py-2 text-[color:var(--color-ink-muted)]">
                    {p.method.replace("_", " ")}
                    {p.reference ? ` · ${p.reference}` : ""}
                  </td>
                  <td className="tabular py-2 text-right">
                    {formatMoney(p.amount, invoice.currency)}
                  </td>
                  <td className="py-2 pl-3 text-right">
                    <button
                      onClick={() => handleDeletePayment(p.id)}
                      className="text-xs text-[color:var(--color-overdue)] hover:underline"
                    >
                      Remove
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <form onSubmit={handleAddPayment} className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-5">
          <Field label="Amount" htmlFor="p-amount">
            <Input
              id="p-amount"
              type="number"
              min="0.01"
              step="0.01"
              required
              value={paymentForm.amount}
              onChange={(e) => setPaymentForm({ ...paymentForm, amount: e.target.value })}
            />
          </Field>
          <Field label="Date" htmlFor="p-date">
            <Input
              id="p-date"
              type="date"
              required
              value={paymentForm.paymentDate}
              onChange={(e) => setPaymentForm({ ...paymentForm, paymentDate: e.target.value })}
            />
          </Field>
          <Field label="Method" htmlFor="p-method">
            <Select
              id="p-method"
              value={paymentForm.method}
              onChange={(e) => setPaymentForm({ ...paymentForm, method: e.target.value })}
            >
              {PAYMENT_METHODS.map((m) => (
                <option key={m} value={m}>
                  {m.replace("_", " ")}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Reference" htmlFor="p-ref">
            <Input
              id="p-ref"
              value={paymentForm.reference}
              onChange={(e) => setPaymentForm({ ...paymentForm, reference: e.target.value })}
            />
          </Field>
          <div className="flex items-end">
            <Button type="submit" disabled={savingPayment} className="w-full">
              {savingPayment ? "Recording…" : "Record payment"}
            </Button>
          </div>
        </form>
        {paymentError && (
          <p className="mt-2 text-sm text-[color:var(--color-overdue)]">{paymentError}</p>
        )}
      </div>
    </div>
  );
}
