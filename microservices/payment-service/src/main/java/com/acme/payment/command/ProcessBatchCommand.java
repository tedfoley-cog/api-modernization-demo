package com.acme.payment.command;

import java.util.List;

/**
 * CQRS command: encapsulates a batch of payment submissions.
 */
public class ProcessBatchCommand {

    private final List<SubmitPaymentCommand> payments;

    public ProcessBatchCommand(List<SubmitPaymentCommand> payments) {
        this.payments = payments;
    }

    public List<SubmitPaymentCommand> getPayments() { return payments; }
}
