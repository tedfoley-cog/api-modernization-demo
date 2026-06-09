package com.acme.payment.command;

/**
 * Thrown when a payment command fails validation.
 */
public class InvalidPaymentException extends RuntimeException {

    public InvalidPaymentException(String message) {
        super(message);
    }
}
