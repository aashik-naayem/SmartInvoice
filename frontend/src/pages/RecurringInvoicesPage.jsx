import { useEffect, useState } from "react";
import { apiErrorMessage } from "../api/client";
import * as clientsApi from "../api/clients";
import * as recurringApi from "../api/recurringInvoices";
import Button from "../components/Button";
import { Field, Input, Select, Textarea } from "../components/FormFields";
import Modal from "../components/Modal";
import StatusBadge from "../components/StatusBadge";
import { useConfirm } from "../context/ConfirmContext";
import { useToast } from "../context/ToastContext";
import { formatDate } from "../lib/format";

const FREQUENCIES = ["WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"];
const EMPTY_ITEM = { description: "", quantity: "1", unitPrice: "" };

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

const EMPTY_FORM = {
  clientId: "",
  frequency: "MONTHLY",
  startDate: todayIso(),
  endDate: "",
  daysDueAfterIssue: "14",
  currency: "USD",
  taxRate: "0",
  notes: "",
  autoSend: false,
};

export default function RecurringInvoicesPage() {
  const confirm = useConfirm();
  const { showSuccess, showError } = useToast();
  const [schedules, setSchedules] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [items, setItems] = useState([{ ...EMPTY_ITEM }]);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState(null);

  const load = () => {
    setLoading(true);
    recurringApi
      .listRecurringInvoices()
      .then(setSchedules)
      .catch(() => showError("Could not load recurring invoices."))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    clientsApi.listClients().then(setClients).catch(() => {});
  }, []);

  const openCreate = () => {
    setForm({ ...EMPTY_FORM, clientId: clients[0] ? String(clients[0].id) : "" });
    setItems([{ ...EMPTY_ITEM }]);
    setFormError("");
    setModalOpen(true);
  };

  const updateItem = (index, field, value) => {
    setItems((prev) => prev.map((it, i) => (i === index ? { ...it, [field]: value } : it)));
  };
  const addItem = () => setItems((prev) => [...prev, { ...EMPTY_ITEM }]);
  const removeItem = (index) => setItems((prev) => prev.filter((_, i) => i !== index));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.clientId) {
      setFormError("Choose a client first.");
      return;
    }
    setSaving(true);
    setFormError("");
    try {
      await recurringApi.createRecurringInvoice({
        clientId: Number(form.clientId),
        frequency: form.frequency,
        startDate: form.startDate,
        endDate: form.endDate || null,
        daysDueAfterIssue: Number(form.daysDueAfterIssue),
        currency: form.currency,
        taxRate: Number(form.taxRate) || 0,
        notes: form.notes,
        autoSend: form.autoSend,
        items: items.map((it) => ({
          description: it.description,
          quantity: Number(it.quantity),
          unitPrice: Number(it.unitPrice),
        })),
      });
      setModalOpen(false);
      showSuccess("Recurring schedule created.");
      load();
    } catch (err) {
      setFormError(apiErrorMessage(err, "Could not create this schedule."));
    } finally {
      setSaving(false);
    }
  };

  const ACTION_MESSAGES = {
    pause: "Schedule paused.",
    resume: "Schedule resumed.",
    cancel: "Schedule cancelled.",
    generate: "Invoice generated.",
  };

  const runAction = async (id, action) => {
    if (action === "cancel") {
      const ok = await confirm({
        title: "Cancel schedule",
        message: "Cancel this recurring schedule? Past invoices are kept, but no new ones will be generated.",
        confirmLabel: "Cancel schedule",
      });
      if (!ok) return;
    }

    setBusyId(id);
    try {
      if (action === "pause") await recurringApi.pauseRecurringInvoice(id);
      if (action === "resume") await recurringApi.resumeRecurringInvoice(id);
      if (action === "cancel") await recurringApi.cancelRecurringInvoice(id);
      if (action === "generate") await recurringApi.generateNowRecurringInvoice(id);
      showSuccess(ACTION_MESSAGES[action]);
      load();
    } catch (err) {
      showError(apiErrorMessage(err, "Could not complete that action."));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl">Recurring invoices</h1>
          <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">
            Billing schedules that generate invoices automatically.
          </p>
        </div>
        <Button onClick={openCreate}>New schedule</Button>
      </div>

      <div className="overflow-x-auto rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)]">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[color:var(--color-border)] text-left text-xs uppercase tracking-wide text-[color:var(--color-ink-muted)]">
              <th className="px-4 py-3 font-medium">Client</th>
              <th className="px-4 py-3 font-medium">Frequency</th>
              <th className="px-4 py-3 font-medium">Next run</th>
              <th className="px-4 py-3 font-medium">Generated</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {schedules.map((s) => (
              <tr key={s.id} className="border-b border-[color:var(--color-border)] last:border-0">
                <td className="px-4 py-3 font-medium">{s.clientName}</td>
                <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">{s.frequency}</td>
                <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">
                  {formatDate(s.nextRunDate)}
                </td>
                <td className="tabular px-4 py-3 text-[color:var(--color-ink-muted)]">
                  {s.occurrencesGenerated}
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={s.status} />
                </td>
                <td className="px-4 py-3 text-right text-xs">
                  <div className="flex justify-end gap-3">
                    {s.status === "ACTIVE" && (
                      <button
                        disabled={busyId === s.id}
                        onClick={() => runAction(s.id, "pause")}
                        className="text-[color:var(--color-ink-muted)] hover:underline"
                      >
                        Pause
                      </button>
                    )}
                    {s.status === "PAUSED" && (
                      <button
                        disabled={busyId === s.id}
                        onClick={() => runAction(s.id, "resume")}
                        className="text-[color:var(--color-pine)] hover:underline"
                      >
                        Resume
                      </button>
                    )}
                    {s.status !== "CANCELLED" && (
                      <button
                        disabled={busyId === s.id}
                        onClick={() => runAction(s.id, "generate")}
                        className="text-[color:var(--color-pine)] hover:underline"
                      >
                        Generate now
                      </button>
                    )}
                    {s.status !== "CANCELLED" && s.status !== "COMPLETED" && (
                      <button
                        disabled={busyId === s.id}
                        onClick={() => runAction(s.id, "cancel")}
                        className="text-[color:var(--color-overdue)] hover:underline"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {!loading && schedules.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[color:var(--color-ink-muted)]">
                  No recurring schedules yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Modal open={modalOpen} title="New recurring schedule" onClose={() => setModalOpen(false)} wide>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Client" htmlFor="r-client">
              <Select
                id="r-client"
                value={form.clientId}
                onChange={(e) => setForm({ ...form, clientId: e.target.value })}
                required
              >
                {clients.length === 0 && <option value="">No clients yet</option>}
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Frequency" htmlFor="r-frequency">
              <Select
                id="r-frequency"
                value={form.frequency}
                onChange={(e) => setForm({ ...form, frequency: e.target.value })}
              >
                {FREQUENCIES.map((f) => (
                  <option key={f} value={f}>
                    {f}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Start date" htmlFor="r-start">
              <Input
                id="r-start"
                type="date"
                required
                value={form.startDate}
                onChange={(e) => setForm({ ...form, startDate: e.target.value })}
              />
            </Field>
            <Field label="End date" htmlFor="r-end" hint="Optional">
              <Input
                id="r-end"
                type="date"
                value={form.endDate}
                onChange={(e) => setForm({ ...form, endDate: e.target.value })}
              />
            </Field>
            <Field label="Days until due" htmlFor="r-due">
              <Input
                id="r-due"
                type="number"
                min="0"
                required
                value={form.daysDueAfterIssue}
                onChange={(e) => setForm({ ...form, daysDueAfterIssue: e.target.value })}
              />
            </Field>
            <Field label="Currency" htmlFor="r-currency">
              <Input
                id="r-currency"
                required
                value={form.currency}
                onChange={(e) => setForm({ ...form, currency: e.target.value.toUpperCase() })}
              />
            </Field>
            <Field label="Tax rate (%)" htmlFor="r-tax">
              <Input
                id="r-tax"
                type="number"
                min="0"
                max="100"
                step="0.01"
                value={form.taxRate}
                onChange={(e) => setForm({ ...form, taxRate: e.target.value })}
              />
            </Field>
            <Field label="Auto-send" htmlFor="r-autosend">
              <label className="flex h-[38px] items-center gap-2 text-sm">
                <input
                  id="r-autosend"
                  type="checkbox"
                  checked={form.autoSend}
                  onChange={(e) => setForm({ ...form, autoSend: e.target.checked })}
                />
                Mark generated invoices as sent
              </label>
            </Field>
          </div>

          <div>
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-medium">Line items</h3>
              <button type="button" onClick={addItem} className="text-sm text-[color:var(--color-pine)] hover:underline">
                + Add item
              </button>
            </div>
            <div className="mt-3 flex flex-col gap-3">
              {items.map((item, index) => (
                <div key={index} className="grid grid-cols-12 items-end gap-2">
                  <div className="col-span-6">
                    <Field label={index === 0 ? "Description" : ""} htmlFor={`ri-desc-${index}`}>
                      <Input
                        id={`ri-desc-${index}`}
                        required
                        value={item.description}
                        onChange={(e) => updateItem(index, "description", e.target.value)}
                      />
                    </Field>
                  </div>
                  <div className="col-span-2">
                    <Field label={index === 0 ? "Qty" : ""} htmlFor={`ri-qty-${index}`}>
                      <Input
                        id={`ri-qty-${index}`}
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
                    <Field label={index === 0 ? "Unit price" : ""} htmlFor={`ri-price-${index}`}>
                      <Input
                        id={`ri-price-${index}`}
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

          <Field label="Notes" htmlFor="r-notes">
            <Textarea
              id="r-notes"
              rows={2}
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
            />
          </Field>

          {formError && <p className="text-sm text-[color:var(--color-overdue)]">{formError}</p>}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? "Creating…" : "Create schedule"}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
