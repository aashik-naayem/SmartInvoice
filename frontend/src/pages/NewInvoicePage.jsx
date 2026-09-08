import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiErrorMessage } from "../api/client";
import * as clientsApi from "../api/clients";
import * as invoicesApi from "../api/invoices";
import Button from "../components/Button";
import { Field, Input, Select, Textarea } from "../components/FormFields";
import { useToast } from "../context/ToastContext";
import { formatMoney } from "../lib/format";

const EMPTY_ITEM = { description: "", quantity: "1", unitPrice: "" };

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function in30DaysIso() {
  const d = new Date();
  d.setDate(d.getDate() + 30);
  return d.toISOString().slice(0, 10);
}

export default function NewInvoicePage() {
  const navigate = useNavigate();
  const { showSuccess } = useToast();
  const [clients, setClients] = useState([]);
  const [clientId, setClientId] = useState("");
  const [issueDate, setIssueDate] = useState(todayIso());
  const [dueDate, setDueDate] = useState(in30DaysIso());
  const [currency, setCurrency] = useState("USD");
  const [taxRate, setTaxRate] = useState("0");
  const [notes, setNotes] = useState("");
  const [items, setItems] = useState([{ ...EMPTY_ITEM }]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    clientsApi.listClients().then((data) => {
      setClients(data);
      if (data.length) setClientId(String(data[0].id));
    });
  }, []);

  const updateItem = (index, field, value) => {
    setItems((prev) => prev.map((it, i) => (i === index ? { ...it, [field]: value } : it)));
  };

  const addItem = () => setItems((prev) => [...prev, { ...EMPTY_ITEM }]);
  const removeItem = (index) => setItems((prev) => prev.filter((_, i) => i !== index));

  const subtotal = items.reduce(
    (sum, it) => sum + (Number(it.quantity) || 0) * (Number(it.unitPrice) || 0),
    0,
  );
  const taxAmount = subtotal * ((Number(taxRate) || 0) / 100);
  const total = subtotal + taxAmount;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!clientId) {
      setError("Choose a client first.");
      return;
    }

    setSaving(true);
    try {
      const payload = {
        clientId: Number(clientId),
        issueDate,
        dueDate,
        currency,
        taxRate: Number(taxRate) || 0,
        notes,
        items: items.map((it) => ({
          description: it.description,
          quantity: Number(it.quantity),
          unitPrice: Number(it.unitPrice),
        })),
      };
      const created = await invoicesApi.createInvoice(payload);
      showSuccess(`Invoice ${created.invoiceNumber} created.`);
      navigate(`/invoices/${created.id}`);
    } catch (err) {
      setError(apiErrorMessage(err, "Could not create this invoice."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-3xl">
      <h1 className="text-2xl">New invoice</h1>
      <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">
        Bill a client for work delivered.
      </p>

      <form className="mt-6 flex flex-col gap-6" onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Client" htmlFor="clientId">
            <Select id="clientId" value={clientId} onChange={(e) => setClientId(e.target.value)} required>
              {clients.length === 0 && <option value="">No clients yet</option>}
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Currency" htmlFor="currency">
            <Input id="currency" value={currency} onChange={(e) => setCurrency(e.target.value.toUpperCase())} required />
          </Field>
          <Field label="Issue date" htmlFor="issueDate">
            <Input id="issueDate" type="date" value={issueDate} onChange={(e) => setIssueDate(e.target.value)} required />
          </Field>
          <Field label="Due date" htmlFor="dueDate">
            <Input id="dueDate" type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} required />
          </Field>
          <Field label="Tax rate (%)" htmlFor="taxRate">
            <Input id="taxRate" type="number" min="0" max="100" step="0.01" value={taxRate} onChange={(e) => setTaxRate(e.target.value)} required />
          </Field>
        </div>

        <div>
          <div className="flex items-center justify-between">
            <h2 className="text-base">Line items</h2>
            <button type="button" onClick={addItem} className="text-sm text-[color:var(--color-pine)] hover:underline">
              + Add item
            </button>
          </div>

          <div className="mt-3 flex flex-col gap-3">
            {items.map((item, index) => (
              <div key={index} className="grid grid-cols-12 items-end gap-2">
                <div className="col-span-6">
                  <Field label={index === 0 ? "Description" : ""} htmlFor={`desc-${index}`}>
                    <Input
                      id={`desc-${index}`}
                      required
                      value={item.description}
                      onChange={(e) => updateItem(index, "description", e.target.value)}
                    />
                  </Field>
                </div>
                <div className="col-span-2">
                  <Field label={index === 0 ? "Qty" : ""} htmlFor={`qty-${index}`}>
                    <Input
                      id={`qty-${index}`}
                      type="number"
                      min="0.01"
                      step="0.01"
                      required
                      value={item.quantity}
                      onChange={(e) => updateItem(index, "quantity", e.target.value)}
                    />
                  </Field>
                </div>
                <div className="col-span-3">
                  <Field label={index === 0 ? "Unit price" : ""} htmlFor={`price-${index}`}>
                    <Input
                      id={`price-${index}`}
                      type="number"
                      min="0"
                      step="0.01"
                      required
                      value={item.unitPrice}
                      onChange={(e) => updateItem(index, "unitPrice", e.target.value)}
                    />
                  </Field>
                </div>
                <div className="col-span-1 pb-2 text-right">
                  {items.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeItem(index)}
                      className="text-[color:var(--color-overdue)]"
                      aria-label="Remove item"
                    >
                      ✕
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <Field label="Notes" htmlFor="notes">
          <Textarea id="notes" rows={3} value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>

        <div className="rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)] p-4 text-sm">
          <div className="flex justify-between">
            <span className="text-[color:var(--color-ink-muted)]">Subtotal</span>
            <span className="tabular">{formatMoney(subtotal, currency)}</span>
          </div>
          <div className="mt-1 flex justify-between">
            <span className="text-[color:var(--color-ink-muted)]">Tax</span>
            <span className="tabular">{formatMoney(taxAmount, currency)}</span>
          </div>
          <div className="mt-2 flex justify-between border-t border-[color:var(--color-border)] pt-2 font-medium">
            <span>Total</span>
            <span className="tabular">{formatMoney(total, currency)}</span>
          </div>
        </div>

        {error && <p className="text-sm text-[color:var(--color-overdue)]">{error}</p>}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Creating…" : "Create invoice"}
          </Button>
        </div>
      </form>
    </div>
  );
}
