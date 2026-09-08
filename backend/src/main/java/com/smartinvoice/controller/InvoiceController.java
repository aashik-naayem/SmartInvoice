package com.smartinvoice.controller;

import com.smartinvoice.dto.InvoiceRequest;
import com.smartinvoice.dto.InvoiceResponse;
import com.smartinvoice.dto.InvoiceStatusUpdateRequest;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.pdf.InvoicePdfService;
import com.smartinvoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAll() {
        return ResponseEntity.ok(invoiceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody InvoiceStatusUpdateRequest request) {
        return ResponseEntity.ok(invoiceService.updateStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<InvoiceResponse> send(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.sendToClient(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Invoice invoice = invoiceService.getOwnedInvoiceEntity(id);
        byte[] pdf = invoicePdfService.renderInvoicePdf(invoice);

        String filename = invoice.getInvoiceNumber() + ".pdf";
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
