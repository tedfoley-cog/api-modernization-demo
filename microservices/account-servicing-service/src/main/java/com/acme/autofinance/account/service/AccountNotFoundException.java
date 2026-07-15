package com.acme.autofinance.account.service;

/** Thrown when an account cannot be located by id or account number. */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
