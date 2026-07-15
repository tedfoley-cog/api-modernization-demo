package com.acme.autofinance.events;

import java.math.BigDecimal;

/** Emitted when a new loan application is created. */
public final class LoanApplicationSubmitted extends DomainEvent {

    private final Long applicationId;
    private final String applicationNumber;
    private final String applicantName;
    private final BigDecimal requestedAmount;
    private final String vehicleVin;
    private final Long dealerId;

    public LoanApplicationSubmitted(Long applicationId, String applicationNumber, String applicantName,
                                    BigDecimal requestedAmount, String vehicleVin, Long dealerId) {
        super(String.valueOf(applicationId));
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.applicantName = applicantName;
        this.requestedAmount = requestedAmount;
        this.vehicleVin = vehicleVin;
        this.dealerId = dealerId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public String getVehicleVin() {
        return vehicleVin;
    }

    public Long getDealerId() {
        return dealerId;
    }

    @Override
    public String eventType() {
        return "LoanApplicationSubmitted";
    }

    @Override
    public String topic() {
        return EventTopics.LOAN_ORIGINATION;
    }
}
