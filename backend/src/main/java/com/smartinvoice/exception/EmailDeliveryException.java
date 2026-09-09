package com.smartinvoice.exception;

/**
 * Thrown when SmartInvoice actually tried to deliver an email (an explicit "send to client",
 * not a background reminder) and it did not go out - either because {@code spring.mail.*}
 * isn't configured, or the mail server rejected the send. Kept distinct from
 * {@link InvalidOperationException} so the message always tells the owner it's a delivery
 * problem, not something wrong with the invoice itself.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }
}
