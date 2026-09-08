package com.smartinvoice.controller;

import com.smartinvoice.dto.PublicInvoiceResponse;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.pdf.InvoicePdfService;
import com.smartinvoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client-facing endpoints - no login required. Access is gated entirely by knowledge of the
 * invoice's unguessable {@code publicToken} (a UUID), which the client only gets from the
 * link SmartInvoice emails them. See {@link com.smartinvoice.config.SecurityConfig} for the
 * matching permitAll rule.
 */
@RestController
@RequestMapping("/api/v1/public/invoices")
@RequiredArgsConstructor
public class PublicInvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicInvoiceResponse> getByToken(@PathVariable String token) {
        Invoice invoice = invoiceService.getByPublicToken(token);
        return ResponseEntity.ok(invoiceService.toPublicResponse(invoice));
    }

    @GetMapping("/{token}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String token) {
        Invoice invoice = invoiceService.getByPublicToken(token);
        byte[] pdf = invoicePdfService.renderInvoicePdf(invoice);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(invoice.getInvoiceNumber() + ".pdf")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
