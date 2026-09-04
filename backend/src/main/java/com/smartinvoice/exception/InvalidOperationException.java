package com.smartinvoice.exception;

/**
 * Thrown when a request is well-formed but violates a business rule about the current
 * state of a resource (e.g. recording a payment against a cancelled invoice).
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
