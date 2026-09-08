import publicClient from "./publicClient";

export function getPublicInvoice(token) {
  return publicClient.get(`/invoices/${token}`).then((res) => res.data);
}

export async function downloadPublicInvoicePdf(token, invoiceNumber) {
  const res = await publicClient.get(`/invoices/${token}/pdf`, { responseType: "blob" });
  const url = window.URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `${invoiceNumber || "invoice"}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
