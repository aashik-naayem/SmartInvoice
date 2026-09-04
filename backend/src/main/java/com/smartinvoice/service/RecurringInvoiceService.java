package com.smartinvoice.service;

import com.smartinvoice.dto.InvoiceResponse;
import com.smartinvoice.dto.RecurringInvoiceItemRequest;
import com.smartinvoice.dto.RecurringInvoiceItemResponse;
import com.smartinvoice.dto.RecurringInvoiceRequest;
import com.smartinvoice.dto.RecurringInvoiceResponse;
import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.RecurringInvoice;
import com.smartinvoice.entity.RecurringInvoiceItem;
import com.smartinvoice.entity.User;
import com.smartinvoice.enums.RecurringInvoiceStatus;
import com.smartinvoice.exception.InvalidOperationException;
import com.smartinvoice.exception.ResourceNotFoundException;
import com.smartinvoice.repository.ClientRepository;
import com.smartinvoice.repository.RecurringInvoiceRepository;
import com.smartinvoice.repository.UserRepository;
import com.smartinvoice.security.SecurityUtils;
import com.smartinvoice.util.RecurrenceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringInvoiceService {

    private final RecurringInvoiceRepository recurringInvoiceRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public RecurringInvoiceResponse create(RecurringInvoiceRequest request) {
        User currentUser = getCurrentUser();

        Client client = clientRepository.findByIdAndUser(request.getClientId(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.getClientId()));

        if (request.getEndDate() != null && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new InvalidOperationException("End date must be after the start date");
        }

        RecurringInvoice template = RecurringInvoice.builder()
                .user(currentUser)
                .client(client)
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .nextRunDate(request.getStartDate())
                .endDate(request.getEndDate())
                .daysDueAfterIssue(request.getDaysDueAfterIssue())
                .currency(request.getCurrency().toUpperCase())
                .taxRate(request.getTaxRate())
                .notes(request.getNotes())
                .autoSend(request.isAutoSend())
                .status(RecurringInvoiceStatus.ACTIVE)
                .occurrencesGenerated(0)
                .build();

        for (RecurringInvoiceItemRequest itemRequest : request.getItems()) {
            template.addItem(RecurringInvoiceItem.builder()
                    .description(itemRequest.getDescription())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build());
        }

        return toResponse(recurringInvoiceRepository.save(template));
    }

    public List<RecurringInvoiceResponse> getAll() {
        return recurringInvoiceRepository.findByUser(getCurrentUser()).stream()
                .map(this::toResponse)
                .toList();
    }

    public RecurringInvoiceResponse getById(Long id) {
        return toResponse(findOwned(id));
    }

    @Transactional
    public RecurringInvoiceResponse pause(Long id) {
        RecurringInvoice template = findOwned(id);
        if (template.getStatus() != RecurringInvoiceStatus.ACTIVE) {
            throw new InvalidOperationException("Only an active recurring invoice can be paused");
        }
        template.setStatus(RecurringInvoiceStatus.PAUSED);
        return toResponse(recurringInvoiceRepository.save(template));
    }

    /**
     * Resumes a paused schedule. If one or more run dates were missed while paused, the schedule
     * is fast-forwarded to the next occurrence still in the future rather than immediately
     * generating a burst of back-dated invoices for every missed period.
     */
    @Transactional
    public RecurringInvoiceResponse resume(Long id) {
        RecurringInvoice template = findOwned(id);
        if (template.getStatus() != RecurringInvoiceStatus.PAUSED) {
            throw new InvalidOperationException("Only a paused recurring invoice can be resumed");
        }

        LocalDate today = LocalDate.now();
        LocalDate next = template.getNextRunDate();
        while (next.isBefore(today)) {
            next = RecurrenceCalculator.next(next, template.getFrequency());
        }

        template.setNextRunDate(next);
        template.setStatus(RecurringInvoiceStatus.ACTIVE);
        return toResponse(recurringInvoiceRepository.save(template));
    }

    /** Cancellation is a soft-stop (not a delete) so occurrence history and past invoices remain intact. */
    @Transactional
    public RecurringInvoiceResponse cancel(Long id) {
        RecurringInvoice template = findOwned(id);
        template.setStatus(RecurringInvoiceStatus.CANCELLED);
        return toResponse(recurringInvoiceRepository.save(template));
    }

    /**
     * Generates one invoice from the template immediately, without touching {@code nextRunDate}
     * or {@code occurrencesGenerated} - it's an extra, on-demand invoice alongside the regular
     * schedule, not a replacement for the next scheduled run.
     */
    @Transactional
    public InvoiceResponse generateNow(Long id) {
        RecurringInvoice template = findOwned(id);
        if (template.getStatus() == RecurringInvoiceStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot generate an invoice from a cancelled recurring schedule");
        }

        Invoice invoice = invoiceService.createFromRecurringTemplate(template, LocalDate.now());
        return invoiceService.getById(invoice.getId());
    }

    private RecurringInvoice findOwned(Long id) {
        User currentUser = getCurrentUser();
        return recurringInvoiceRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring invoice not found with id: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private RecurringInvoiceResponse toResponse(RecurringInvoice template) {
        List<RecurringInvoiceItemResponse> items = template.getItems().stream()
                .map(item -> RecurringInvoiceItemResponse.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .toList();

        return RecurringInvoiceResponse.builder()
                .id(template.getId())
                .clientId(template.getClient().getId())
                .clientName(template.getClient().getName())
                .frequency(template.getFrequency())
                .startDate(template.getStartDate())
                .nextRunDate(template.getNextRunDate())
                .endDate(template.getEndDate())
                .daysDueAfterIssue(template.getDaysDueAfterIssue())
                .currency(template.getCurrency())
                .taxRate(template.getTaxRate())
                .notes(template.getNotes())
                .autoSend(template.isAutoSend())
                .status(template.getStatus())
                .occurrencesGenerated(template.getOccurrencesGenerated())
                .items(items)
                .createdAt(template.getCreatedAt())
                .build();
    }
}
