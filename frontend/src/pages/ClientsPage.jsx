import { useEffect, useState } from "react";
import * as clientsApi from "../api/clients";
import { apiErrorMessage } from "../api/client";
import Button from "../components/Button";
import { Field, Input, Textarea } from "../components/FormFields";
import Modal from "../components/Modal";
import { useConfirm } from "../context/ConfirmContext";
import { useToast } from "../context/ToastContext";
import { formatDate } from "../lib/format";

const EMPTY_FORM = { name: "", email: "", phone: "", address: "" };

export default function ClientsPage() {
  const confirm = useConfirm();
  const { showSuccess, showError } = useToast();
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    clientsApi
      .listClients()
      .then(setClients)
      .catch(() => showError("Could not load clients."))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const openCreate = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setError("");
    setModalOpen(true);
  };

  const openEdit = (client) => {
    setEditing(client);
    setForm({
      name: client.name,
      email: client.email || "",
      phone: client.phone || "",
      address: client.address || "",
    });
    setError("");
    setModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (editing) {
        await clientsApi.updateClient(editing.id, form);
        showSuccess(`Updated ${form.name}.`);
      } else {
        await clientsApi.createClient(form);
        showSuccess(`Added ${form.name}.`);
      }
      setModalOpen(false);
      load();
    } catch (err) {
      setError(apiErrorMessage(err, "Could not save this client."));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (client) => {
    const ok = await confirm({
      title: "Delete client",
      message: `Delete ${client.name}? This can't be undone.`,
      confirmLabel: "Delete",
    });
    if (!ok) return;
    try {
      await clientsApi.deleteClient(client.id);
      showSuccess(`Deleted ${client.name}.`);
      load();
    } catch (err) {
      showError(apiErrorMessage(err, "Could not delete this client."));
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl">Clients</h1>
          <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">
            Everyone you bill, in one place.
          </p>
        </div>
        <Button onClick={openCreate}>Add client</Button>
      </div>

      <div className="overflow-x-auto rounded-sm border border-[color:var(--color-border)] bg-[color:var(--color-surface)]">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[color:var(--color-border)] text-left text-xs uppercase tracking-wide text-[color:var(--color-ink-muted)]">
              <th className="px-4 py-3 font-medium">Name</th>
              <th className="px-4 py-3 font-medium">Email</th>
              <th className="px-4 py-3 font-medium">Phone</th>
              <th className="px-4 py-3 font-medium">Client since</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {clients.map((c) => (
              <tr key={c.id} className="border-b border-[color:var(--color-border)] last:border-0">
                <td className="px-4 py-3 font-medium">{c.name}</td>
                <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">{c.email || "—"}</td>
                <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">{c.phone || "—"}</td>
                <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">
                  {formatDate(c.createdAt)}
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => openEdit(c)}
                    className="mr-3 text-[color:var(--color-pine)] hover:underline"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(c)}
                    className="text-[color:var(--color-overdue)] hover:underline"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {!loading && clients.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-[color:var(--color-ink-muted)]">
                  No clients yet. Add your first one to start billing.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Modal open={modalOpen} title={editing ? "Edit client" : "Add client"} onClose={() => setModalOpen(false)}>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <Field label="Name" htmlFor="name">
            <Input
              id="name"
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          </Field>
          <Field label="Email" htmlFor="c-email">
            <Input
              id="c-email"
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </Field>
          <Field label="Phone" htmlFor="phone">
            <Input
              id="phone"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </Field>
          <Field label="Address" htmlFor="address">
            <Textarea
              id="address"
              rows={2}
              value={form.address}
              onChange={(e) => setForm({ ...form, address: e.target.value })}
            />
          </Field>

          {error && <p className="text-sm text-[color:var(--color-overdue)]">{error}</p>}

          <div className="mt-2 flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? "Saving…" : "Save client"}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
