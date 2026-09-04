package com.smartinvoice.email;

import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.enums.ReminderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends payment reminder emails to clients.
 * <p>
 * Uses {@link ObjectProvider} rather than a direct {@link JavaMailSender} dependency so the
 * scheduler can keep running (and logging what it would have sent) even in an environment where
 * {@code spring.mail.*} hasn't been configured yet, instead of failing application startup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.from:no-reply@smartinvoice.app}")
    private String fromAddress;

    public void sendPaymentReminder(Invoice invoice, ReminderType type) {
        Client client = invoice.getClient();
        if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
            log.warn("Skipping {} reminder for invoice {} - client has no email on file",
                    type, invoice.getInvoiceNumber());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Skipping {} reminder for invoice {} - mail is not configured (set spring.mail.* properties)",
                    type, invoice.getInvoiceNumber());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(client.getEmail());
            message.setSubject(subjectFor(invoice, type));
            message.setText(bodyFor(invoice, type));
            mailSender.send(message);
            log.info("Sent {} reminder for invoice {} to {}", type, invoice.getInvoiceNumber(), client.getEmail());
        } catch (Exception e) {
            log.error("Failed to send {} reminder for invoice {}: {}", type, invoice.getInvoiceNumber(), e.getMessage());
        }
    }

    private String subjectFor(Invoice invoice, ReminderType type) {
        return switch (type) {
            case UPCOMING_DUE -> "Upcoming payment due - Invoice " + invoice.getInvoiceNumber();
            case DUE_TODAY -> "Payment due today - Invoice " + invoice.getInvoiceNumber();
            case OVERDUE -> "Overdue payment - Invoice " + invoice.getInvoiceNumber();
        };
    }

    private String bodyFor(Invoice invoice, ReminderType type) {
        String clientName = invoice.getClient() != null ? invoice.getClient().getName() : "there";
        String amount = invoice.getTotalAmount() + " " + invoice.getCurrency();
        String dueDate = invoice.getDueDate().toString();

        return switch (type) {
            case UPCOMING_DUE -> "Hi " + clientName + ",\n\nThis is a friendly reminder that invoice "
                    + invoice.getInvoiceNumber() + " for " + amount + " is due on " + dueDate
                    + ".\n\nThank you.";
            case DUE_TODAY -> "Hi " + clientName + ",\n\nInvoice " + invoice.getInvoiceNumber() + " for " + amount
                    + " is due today (" + dueDate + ").\n\nThank you.";
            case OVERDUE -> "Hi " + clientName + ",\n\nInvoice " + invoice.getInvoiceNumber() + " for " + amount
                    + " was due on " + dueDate + " and is now overdue. Please arrange payment at your earliest "
                    + "convenience.\n\nThank you.";
        };
    }
}
