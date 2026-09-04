package com.smartinvoice.controller;

import com.smartinvoice.dto.PaymentRequest;
import com.smartinvoice.dto.PaymentResponse;
import com.smartinvoice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@PathVariable Long invoiceId,
                                                    @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(invoiceId, request));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAll(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(paymentService.getAllForInvoice(invoiceId));
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> delete(@PathVariable Long invoiceId, @PathVariable Long paymentId) {
        paymentService.delete(invoiceId, paymentId);
        return ResponseEntity.noContent().build();
    }
}
