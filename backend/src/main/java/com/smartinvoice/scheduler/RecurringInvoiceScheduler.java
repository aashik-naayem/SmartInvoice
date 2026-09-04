package com.smartinvoice.scheduler;

import com.smartinvoice.entity.RecurringInvoice;
import com.smartinvoice.enums.RecurringInvoiceStatus;
import com.smartinvoice.repository.RecurringInvoiceRepository;
import com.smartinvoice.service.InvoiceService;
import com.smartinvoice.util.RecurrenceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily job that turns each due {@link RecurringInvoice} template into a real invoice and
 * advances the schedule. One template's failure (e.g. a since-deleted client) is logged and
 * skipped rather than aborting the whole run.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringInvoiceScheduler {

    private final RecurringInvoiceRepository recurringInvoiceRepository;
    private final InvoiceService invoiceService;

    @Scheduled(cron = "${smartinvoice.recurring.cron:0 0 2 * * *}")
    @Transactional
    public void generateDueInvoices() {
        LocalDate today = LocalDate.now();
        List<RecurringInvoice> due = recurringInvoiceRepository
                .findByStatusAndNextRunDateLessThanEqual(RecurringInvoiceStatus.ACTIVE, today);

        for (RecurringInvoice template : due) {
            try {
                invoiceService.createFromRecurringTemplate(template, template.getNextRunDate());
                advance(template);
            } catch (Exception e) {
                log.error("Failed to generate invoice from recurring schedule {}: {}", template.getId(), e.getMessage(), e);
            }
        }
    }

    private void advance(RecurringInvoice template) {
        LocalDate next = RecurrenceCalculator.next(template.getNextRunDate(), template.getFrequency());
        template.setOccurrencesGenerated(template.getOccurrencesGenerated() + 1);

        if (template.getEndDate() != null && next.isAfter(template.getEndDate())) {
            template.setStatus(RecurringInvoiceStatus.COMPLETED);
        } else {
            template.setNextRunDate(next);
        }

        recurringInvoiceRepository.save(template);
    }
}
