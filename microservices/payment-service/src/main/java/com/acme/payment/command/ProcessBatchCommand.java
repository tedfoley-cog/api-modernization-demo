package com.acme.payment.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command wrapping a batch of {@link SubmitPaymentCommand}s to be processed together.
 */
public final class ProcessBatchCommand {

    private final List<SubmitPaymentCommand> payments;

    public ProcessBatchCommand(List<SubmitPaymentCommand> payments) {
        this.payments = payments == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(payments));
    }

    public List<SubmitPaymentCommand> getPayments() {
        return payments;
    }
}
