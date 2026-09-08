import client from "./client";

export function listInvoices() {
  return client.get("/invoices").then((res) => res.data);
}

export function getInvoice(id) {
  return client.get(`/invoices/${id}`).then((res) => res.data);
}

export function createInvoice(payload) {
  return client.post("/invoices", payload).then((res) => res.data);
}

export function updateInvoiceStatus(id, status) {
  return client.patch(`/invoices/${id}/status`, { status }).then((res) => res.data);
}

export function deleteInvoice(id) {
  return client.delete(`/invoices/${id}`);
}

export function sendInvoice(id) {
  return client.post(`/invoices/${id}/send`).then((res) => res.data);
}

export async function downloadInvoicePdf(id, invoiceNumber) {
  const res = await client.get(`/invoices/${id}/pdf`, { responseType: "blob" });
  const url = window.URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `${invoiceNumber || "invoice"}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
