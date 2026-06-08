package com.acme.payment.command;

/**
 * Outcome of a batch submission.
 */
public class BatchResult {

    private final int totalSubmitted;
    private final int processed;
    private final int failed;

    public BatchResult(int totalSubmitted, int processed, int failed) {
        this.totalSubmitted = totalSubmitted;
        this.processed = processed;
        this.failed = failed;
    }

    public int getTotalSubmitted() {
        return totalSubmitted;
    }

    public int getProcessed() {
        return processed;
    }

    public int getFailed() {
        return failed;
    }
}
