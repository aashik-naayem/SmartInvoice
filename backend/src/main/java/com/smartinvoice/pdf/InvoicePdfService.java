package com.smartinvoice.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.InvoiceItem;
import com.smartinvoice.entity.User;
import com.smartinvoice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders an {@link Invoice} as a PDF document.
 * <p>
 * Builds a self-contained XHTML template (inline CSS, no external resources) and hands it to
 * openhtmltopdf, which lays it out and writes it straight to PDF bytes. Keeping the HTML strictly
 * well-formed matters here since openhtmltopdf parses it as XML by default.
 */
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final PaymentRepository paymentRepository;

    public byte[] renderInvoicePdf(Invoice invoice) {
        String html = buildHtml(invoice);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render invoice PDF for invoice id " + invoice.getId(), e);
        }
    }

    private String buildHtml(Invoice invoice) {
        User issuer = invoice.getUser();
        Client client = invoice.getClient();

        StringBuilder itemRows = new StringBuilder();
        for (InvoiceItem item : invoice.getItems()) {
            itemRows.append("<tr>")
                    .append("<td>").append(escape(item.getDescription())).append("</td>")
                    .append("<td class=\"num\">").append(formatQuantity(item.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(formatMoney(item.getUnitPrice())).append("</td>")
                    .append("<td class=\"num\">").append(formatMoney(item.getLineTotal())).append("</td>")
                    .append("</tr>");
        }

        String notesSection = "";
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            notesSection = "<div class=\"notes\"><h3>Notes</h3><p>" + escape(invoice.getNotes()) + "</p></div>";
        }

        String currency = invoice.getCurrency() == null ? "" : escape(invoice.getCurrency());

        BigDecimal amountPaid = paymentRepository.sumAmountByInvoice(invoice);
        BigDecimal balanceDue = invoice.getTotalAmount().subtract(amountPaid);
        String paidRows = "";
        if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            paidRows = "    <div class=\"totals-row\"><span>Amount Paid</span><span>" + formatMoney(amountPaid) + " " + currency + "</span></div>"
                    + "    <div class=\"totals-row\"><span>Balance Due</span><span>" + formatMoney(balanceDue) + " " + currency + "</span></div>";
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE html>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                + "<head><meta charset=\"UTF-8\"/><title>Invoice " + escape(invoice.getInvoiceNumber()) + "</title>"
                + "<style>" + css() + "</style></head>"
                + "<body>"
                + "<div class=\"sheet\">"
                + "  <div class=\"header\">"
                + "    <div>"
                + "      <div class=\"brand\">SmartInvoice</div>"
                + "      <div class=\"issuer\">" + escape(issuer != null ? issuer.getFullName() : "") + "</div>"
                + "      <div class=\"issuer-sub\">" + escape(issuer != null ? issuer.getEmail() : "") + "</div>"
                + "    </div>"
                + "    <div class=\"invoice-meta\">"
                + "      <div class=\"invoice-title\">INVOICE</div>"
                + "      <div class=\"invoice-number\">" + escape(invoice.getInvoiceNumber()) + "</div>"
                + "      <div class=\"status status-" + escape(invoice.getStatus().name().toLowerCase(Locale.ENGLISH)) + "\">"
                +          escape(invoice.getStatus().name()) + "</div>"
                + "    </div>"
                + "  </div>"

                + "  <div class=\"parties\">"
                + "    <div class=\"party\">"
                + "      <h3>Bill To</h3>"
                + "      <div class=\"party-name\">" + escape(client != null ? client.getName() : "") + "</div>"
                +          renderIfPresent(client != null ? client.getEmail() : null)
                +          renderIfPresent(client != null ? client.getPhone() : null)
                +          renderIfPresent(client != null ? client.getAddress() : null)
                + "    </div>"
                + "    <div class=\"party dates\">"
                + "      <div class=\"date-row\"><span>Issue Date</span><strong>" + formatDate(invoice.getIssueDate()) + "</strong></div>"
                + "      <div class=\"date-row\"><span>Due Date</span><strong>" + formatDate(invoice.getDueDate()) + "</strong></div>"
                + "      <div class=\"date-row\"><span>Currency</span><strong>" + currency + "</strong></div>"
                + "    </div>"
                + "  </div>"

                + "  <table class=\"items\">"
                + "    <thead><tr><th>Description</th><th class=\"num\">Qty</th><th class=\"num\">Unit Price</th><th class=\"num\">Amount</th></tr></thead>"
                + "    <tbody>" + itemRows + "</tbody>"
                + "  </table>"

                + "  <div class=\"totals\">"
                + "    <div class=\"totals-row\"><span>Subtotal</span><span>" + formatMoney(invoice.getSubtotal()) + " " + currency + "</span></div>"
                + "    <div class=\"totals-row\"><span>Tax (" + formatQuantity(invoice.getTaxRate()) + "%)</span><span>" + formatMoney(invoice.getTaxAmount()) + " " + currency + "</span></div>"
                + "    <div class=\"totals-row grand-total\"><span>Total Due</span><span>" + formatMoney(invoice.getTotalAmount()) + " " + currency + "</span></div>"
                + paidRows
                + "  </div>"

                + notesSection

                + "  <div class=\"footer\">Generated by SmartInvoice</div>"
                + "</div>"
                + "</body></html>";
    }

    private String renderIfPresent(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<div class=\"party-line\">" + escape(value) + "</div>";
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private String formatMoney(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return "0";
        }
        BigDecimal stripped = quantity.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String css() {
        return "body{font-family:Helvetica,Arial,sans-serif;color:#1f2937;margin:0;}"
                + ".sheet{padding:40px;}"
                + ".header{display:flex;justify-content:space-between;border-bottom:2px solid #1f2937;padding-bottom:16px;margin-bottom:24px;}"
                + ".brand{font-size:20px;font-weight:bold;color:#111827;}"
                + ".issuer{margin-top:8px;font-size:13px;}"
                + ".issuer-sub{font-size:11px;color:#6b7280;}"
                + ".invoice-meta{text-align:right;}"
                + ".invoice-title{font-size:22px;font-weight:bold;letter-spacing:2px;color:#111827;}"
                + ".invoice-number{font-size:13px;color:#374151;margin-top:4px;}"
                + ".status{display:inline-block;margin-top:8px;padding:3px 10px;font-size:10px;font-weight:bold;letter-spacing:1px;border-radius:3px;color:#ffffff;background:#6b7280;}"
                + ".status-paid{background:#16a34a;}"
                + ".status-overdue{background:#dc2626;}"
                + ".status-sent{background:#2563eb;}"
                + ".status-draft{background:#6b7280;}"
                + ".status-cancelled{background:#78716c;}"
                + ".parties{display:flex;justify-content:space-between;margin-bottom:24px;}"
                + ".party{width:48%;font-size:12px;}"
                + ".party h3{margin:0 0 6px 0;font-size:11px;text-transform:uppercase;color:#6b7280;letter-spacing:1px;}"
                + ".party-name{font-weight:bold;font-size:13px;margin-bottom:2px;}"
                + ".party-line{color:#374151;}"
                + ".dates .date-row{display:flex;justify-content:space-between;padding:3px 0;}"
                + "table.items{width:100%;border-collapse:collapse;margin-bottom:16px;}"
                + "table.items th{text-align:left;font-size:10px;text-transform:uppercase;letter-spacing:1px;color:#6b7280;border-bottom:1px solid #d1d5db;padding:8px 6px;}"
                + "table.items td{font-size:12px;padding:8px 6px;border-bottom:1px solid #e5e7eb;}"
                + "table.items .num{text-align:right;}"
                + ".totals{width:280px;margin-left:auto;font-size:12px;}"
                + ".totals-row{display:flex;justify-content:space-between;padding:4px 0;}"
                + ".grand-total{border-top:2px solid #1f2937;margin-top:4px;padding-top:8px;font-size:14px;font-weight:bold;}"
                + ".notes{margin-top:24px;font-size:11px;color:#374151;}"
                + ".notes h3{font-size:11px;text-transform:uppercase;color:#6b7280;letter-spacing:1px;margin-bottom:4px;}"
                + ".footer{margin-top:40px;text-align:center;font-size:10px;color:#9ca3af;}";
    }
}
