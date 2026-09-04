package com.smartinvoice.controller;

import com.smartinvoice.dto.InvoiceResponse;
import com.smartinvoice.dto.RecurringInvoiceRequest;
import com.smartinvoice.dto.RecurringInvoiceResponse;
import com.smartinvoice.service.RecurringInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-invoices")
@RequiredArgsConstructor
public class RecurringInvoiceController {

    private final RecurringInvoiceService recurringInvoiceService;

    @PostMapping
    public ResponseEntity<RecurringInvoiceResponse> create(@Valid @RequestBody RecurringInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringInvoiceService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<RecurringInvoiceResponse>> getAll() {
        return ResponseEntity.ok(recurringInvoiceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringInvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recurringInvoiceService.getById(id));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringInvoiceResponse> pause(@PathVariable Long id) {
        return ResponseEntity.ok(recurringInvoiceService.pause(id));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringInvoiceResponse> resume(@PathVariable Long id) {
        return ResponseEntity.ok(recurringInvoiceService.resume(id));
    }

    /** Soft-stops the schedule (status -> CANCELLED); past invoices and occurrence history are kept. */
    @DeleteMapping("/{id}")
    public ResponseEntity<RecurringInvoiceResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(recurringInvoiceService.cancel(id));
    }

    @PostMapping("/{id}/generate-now")
    public ResponseEntity<InvoiceResponse> generateNow(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringInvoiceService.generateNow(id));
    }
}
