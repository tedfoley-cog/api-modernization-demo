package com.acme.payment.command;

import java.util.Date;

/**
 * Outcome summary of a batch payment run.
 */
public final class BatchResult {

    private final int totalSubmitted;
    private final int processed;
    private final int failed;
    private final Date batchDate;

    public BatchResult(int totalSubmitted, int processed, int failed) {
        this.totalSubmitted = totalSubmitted;
        this.processed = processed;
        this.failed = failed;
        this.batchDate = new Date();
    }

    public int getTotalSubmitted() { return totalSubmitted; }
    public int getProcessed() { return processed; }
    public int getFailed() { return failed; }
    public Date getBatchDate() { return batchDate; }
}
