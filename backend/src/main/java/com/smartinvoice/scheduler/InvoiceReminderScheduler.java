package com.smartinvoice.scheduler;

import com.smartinvoice.email.EmailService;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.ReminderLog;
import com.smartinvoice.enums.InvoiceStatus;
import com.smartinvoice.enums.ReminderType;
import com.smartinvoice.repository.InvoiceRepository;
import com.smartinvoice.repository.ReminderLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Daily job that keeps invoice status in sync with due dates and emails clients about payments
 * coming up, due today, or overdue. Each invoice's failure is logged and skipped so one bad
 * record can't block reminders for the rest.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceReminderScheduler {

    private final InvoiceRepository invoiceRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final EmailService emailService;

    @Value("${smartinvoice.reminders.upcoming-days-before:3}")
    private int upcomingDaysBefore;

    @Value("${smartinvoice.reminders.overdue-repeat-days:7}")
    private int overdueRepeatDays;

    @Scheduled(cron = "${smartinvoice.reminders.cron:0 0 8 * * *}")
    @Transactional
    public void processReminders() {
        LocalDate today = LocalDate.now();
        markOverdueInvoices(today);

        List<Invoice> invoices = invoiceRepository.findByStatusIn(List.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE));
        for (Invoice invoice : invoices) {
            try {
                evaluateInvoice(invoice, today);
            } catch (Exception e) {
                log.error("Failed to process reminder for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            }
        }
    }

    private void markOverdueInvoices(LocalDate today) {
        List<Invoice> overdue = invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.SENT, today);
        for (Invoice invoice : overdue) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
        }
    }

    private void evaluateInvoice(Invoice invoice, LocalDate today) {
        if (invoice.getStatus() == InvoiceStatus.OVERDUE) {
            maybeSendReminder(invoice, ReminderType.OVERDUE, today);
            return;
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, invoice.getDueDate());
        if (daysUntilDue == 0) {
            maybeSendReminder(invoice, ReminderType.DUE_TODAY, today);
        } else if (daysUntilDue == upcomingDaysBefore) {
            maybeSendReminder(invoice, ReminderType.UPCOMING_DUE, today);
        }
    }

    /**
     * UPCOMING_DUE and DUE_TODAY are one-shot per invoice. OVERDUE repeats every
     * {@code overdueRepeatDays} days so an unpaid invoice keeps getting chased.
     */
    private void maybeSendReminder(Invoice invoice, ReminderType type, LocalDate today) {
        if (type == ReminderType.OVERDUE) {
            Optional<ReminderLog> last = reminderLogRepository
                    .findTopByInvoiceAndReminderTypeOrderBySentAtDesc(invoice, type);
            if (last.isPresent() && !last.get().getSentAt().toLocalDate().isBefore(today.minusDays(overdueRepeatDays - 1L))) {
                return;
            }
        } else if (reminderLogRepository.existsByInvoiceAndReminderType(invoice, type)) {
            return;
        }

        emailService.sendPaymentReminder(invoice, type);
        reminderLogRepository.save(ReminderLog.builder()
                .invoice(invoice)
                .reminderType(type)
                .sentAt(LocalDateTime.now())
                .build());
    }
}
