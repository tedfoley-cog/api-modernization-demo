package com.acme.payment.command;

import javax.validation.Valid;

import java.util.ArrayList;
import java.util.List;

/**
 * Write-side request wrapping a batch of {@link SubmitPaymentCommand}s.
 */
public class ProcessBatchCommand {

    @Valid
    private List<SubmitPaymentCommand> payments = new ArrayList<>();

    public ProcessBatchCommand() {
    }

    public ProcessBatchCommand(List<SubmitPaymentCommand> payments) {
        if (payments != null) {
            this.payments = payments;
        }
    }

    public List<SubmitPaymentCommand> getPayments() {
        return payments;
    }

    public void setPayments(List<SubmitPaymentCommand> payments) {
        this.payments = payments == null ? new ArrayList<>() : payments;
    }
}
