import client from "./client";

export function listPayments(invoiceId) {
  return client.get(`/invoices/${invoiceId}/payments`).then((res) => res.data);
}

export function createPayment(invoiceId, payload) {
  return client.post(`/invoices/${invoiceId}/payments`, payload).then((res) => res.data);
}

export function deletePayment(invoiceId, paymentId) {
  return client.delete(`/invoices/${invoiceId}/payments/${paymentId}`);
}
