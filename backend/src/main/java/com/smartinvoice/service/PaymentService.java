package com.smartinvoice.service;

import com.smartinvoice.dto.PaymentRequest;
import com.smartinvoice.dto.PaymentResponse;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.Payment;
import com.smartinvoice.enums.InvoiceStatus;
import com.smartinvoice.exception.InvalidOperationException;
import com.smartinvoice.exception.ResourceNotFoundException;
import com.smartinvoice.repository.InvoiceRepository;
import com.smartinvoice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public PaymentResponse create(Long invoiceId, PaymentRequest request) {
        Invoice invoice = invoiceService.getOwnedInvoiceEntity(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot record a payment against a cancelled invoice");
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .method(request.getMethod())
                .reference(request.getReference())
                .notes(request.getNotes())
                .build();

        payment = paymentRepository.save(payment);

        syncInvoiceStatus(invoice);

        return toResponse(payment);
    }

    public List<PaymentResponse> getAllForInvoice(Long invoiceId) {
        Invoice invoice = invoiceService.getOwnedInvoiceEntity(invoiceId);
        return paymentRepository.findByInvoiceOrderByPaymentDateDescCreatedAtDesc(invoice).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long invoiceId, Long paymentId) {
        Invoice invoice = invoiceService.getOwnedInvoiceEntity(invoiceId);
        Payment payment = paymentRepository.findByIdAndInvoice(paymentId, invoice)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        paymentRepository.delete(payment);
        syncInvoiceStatus(invoice);
    }

    /**
     * Flips the invoice to PAID once payments cover the total, and back off PAID if a
     * payment is later removed and the balance is no longer covered. Leaves DRAFT,
     * SENT (partially paid), OVERDUE and CANCELLED alone in the "not yet fully paid" case,
     * since we shouldn't silently override a status the user hasn't asked to reopen.
     */
    private void syncInvoiceStatus(Invoice invoice) {
        BigDecimal amountPaid = paymentRepository.sumAmountByInvoice(invoice);
        boolean fullyPaid = amountPaid.compareTo(invoice.getTotalAmount()) >= 0;

        if (fullyPaid && invoice.getStatus() != InvoiceStatus.CANCELLED && invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
        } else if (!fullyPaid && invoice.getStatus() == InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.SENT);
            invoiceRepository.save(invoice);
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .method(payment.getMethod())
                .reference(payment.getReference())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
