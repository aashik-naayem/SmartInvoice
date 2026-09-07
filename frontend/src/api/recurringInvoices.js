import client from "./client";

export function listRecurringInvoices() {
  return client.get("/recurring-invoices").then((res) => res.data);
}

export function getRecurringInvoice(id) {
  return client.get(`/recurring-invoices/${id}`).then((res) => res.data);
}

export function createRecurringInvoice(payload) {
  return client.post("/recurring-invoices", payload).then((res) => res.data);
}

export function pauseRecurringInvoice(id) {
  return client.patch(`/recurring-invoices/${id}/pause`).then((res) => res.data);
}

export function resumeRecurringInvoice(id) {
  return client.patch(`/recurring-invoices/${id}/resume`).then((res) => res.data);
}

export function cancelRecurringInvoice(id) {
  return client.delete(`/recurring-invoices/${id}`).then((res) => res.data);
}

export function generateNowRecurringInvoice(id) {
  return client.post(`/recurring-invoices/${id}/generate-now`).then((res) => res.data);
}
