package com.smartinvoice.email;

import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.enums.ReminderType;
import com.smartinvoice.exception.InvalidOperationException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    @Value("${smartinvoice.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Emails the invoice to its client: a short message with the public, no-login link to view
     * it, plus the PDF attached directly. Called from the owner-triggered "Send to client" action.
     * <p>
     * Unlike {@link #sendPaymentReminder}, a missing client email is treated as a real error here
     * (there's nothing sensible to do with an explicit send request otherwise) - but a mail server
     * that isn't configured is still just logged, same as reminders, so the invoice can still be
     * marked SENT and its link shared manually.
     */
    public void sendInvoice(Invoice invoice, byte[] pdfBytes) {
        Client client = invoice.getClient();
        if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
            throw new InvalidOperationException(
                    "This client doesn't have an email address on file. Add one before sending.");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Invoice {} marked as sent, but mail is not configured (set spring.mail.* properties) - "
                    + "share the invoice link with the client manually.", invoice.getInvoiceNumber());
            return;
        }

        String link = frontendUrl + "/invoice/" + invoice.getPublicToken();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(fromAddress);
            helper.setTo(client.getEmail());
            helper.setSubject("Invoice " + invoice.getInvoiceNumber() + " from " + issuerLabel(invoice));
            helper.setText(invoiceBody(invoice, link));
            helper.addAttachment(invoice.getInvoiceNumber() + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(pdfBytes));
            mailSender.send(mimeMessage);
            log.info("Sent invoice {} to {}", invoice.getInvoiceNumber(), client.getEmail());
        } catch (Exception e) {
            log.error("Failed to send invoice {} to {}: {}", invoice.getInvoiceNumber(), client.getEmail(),
                    e.getMessage());
        }
    }

    private String issuerLabel(Invoice invoice) {
        return invoice.getUser() != null && invoice.getUser().getFullName() != null
                ? invoice.getUser().getFullName()
                : "SmartInvoice";
    }

    private String invoiceBody(Invoice invoice, String link) {
        String clientName = invoice.getClient() != null ? invoice.getClient().getName() : "there";
        String amount = invoice.getTotalAmount() + " " + invoice.getCurrency();

        return "Hi " + clientName + ",\n\n"
                + issuerLabel(invoice) + " has sent you invoice " + invoice.getInvoiceNumber()
                + " for " + amount + ", due " + invoice.getDueDate() + ".\n\n"
                + "View and download it here:\n" + link + "\n\n"
                + "The PDF is also attached to this email.\n\nThank you.";
    }

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
