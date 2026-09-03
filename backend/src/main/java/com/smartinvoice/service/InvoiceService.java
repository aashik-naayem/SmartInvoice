package com.smartinvoice.service;

import com.smartinvoice.dto.*;
import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.InvoiceItem;
import com.smartinvoice.entity.User;
import com.smartinvoice.enums.InvoiceStatus;
import com.smartinvoice.exception.ResourceNotFoundException;
import com.smartinvoice.repository.ClientRepository;
import com.smartinvoice.repository.InvoiceRepository;
import com.smartinvoice.repository.UserRepository;
import com.smartinvoice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        User currentUser = getCurrentUser();

        Client client = clientRepository.findByIdAndUser(request.getClientId(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.getClientId()));

        Invoice invoice = Invoice.builder()
                .user(currentUser)
                .client(client)
                .invoiceNumber(generateInvoiceNumber())
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .status(InvoiceStatus.DRAFT)
                .currency(request.getCurrency().toUpperCase())
                .taxRate(request.getTaxRate())
                .notes(request.getNotes())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (InvoiceItemRequest itemRequest : request.getItems()) {
            BigDecimal lineTotal = itemRequest.getUnitPrice()
                    .multiply(itemRequest.getQuantity())
                    .setScale(2, RoundingMode.HALF_UP);

            InvoiceItem item = InvoiceItem.builder()
                    .description(itemRequest.getDescription())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();

            invoice.addItem(item);
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal taxAmount = subtotal
                .multiply(request.getTaxRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);

        return toResponse(invoiceRepository.save(invoice));
    }

    public List<InvoiceResponse> getAll() {
        User currentUser = getCurrentUser();
        return invoiceRepository.findByUser(currentUser).stream()
                .map(this::toResponse)
                .toList();
    }

    public InvoiceResponse getById(Long id) {
        return toResponse(findOwnedInvoice(id));
    }

    @Transactional
    public InvoiceResponse updateStatus(Long id, InvoiceStatus status) {
        Invoice invoice = findOwnedInvoice(id);
        invoice.setStatus(status);
        return toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void delete(Long id) {
        Invoice invoice = findOwnedInvoice(id);
        invoiceRepository.delete(invoice);
    }

    private Invoice findOwnedInvoice(Long id) {
        User currentUser = getCurrentUser();
        return invoiceRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    private String generateInvoiceNumber() {
        String candidate;
        do {
            candidate = "INV-" + System.currentTimeMillis();
        } while (invoiceRepository.existsByInvoiceNumber(candidate));
        return candidate;
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceItemResponse> items = invoice.getItems().stream()
                .map(item -> InvoiceItemResponse.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .clientId(invoice.getClient().getId())
                .clientName(invoice.getClient().getName())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .currency(invoice.getCurrency())
                .subtotal(invoice.getSubtotal())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .notes(invoice.getNotes())
                .items(items)
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
