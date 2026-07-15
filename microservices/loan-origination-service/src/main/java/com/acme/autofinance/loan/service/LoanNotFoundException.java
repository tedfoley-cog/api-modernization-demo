package com.acme.autofinance.loan.service;

/** Thrown when a loan application cannot be located by id or application number. */
public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(String message) {
        super(message);
    }
}
