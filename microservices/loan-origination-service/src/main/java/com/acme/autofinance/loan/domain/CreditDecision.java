package com.acme.autofinance.loan.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Result of an internal credit check. This is a transient value object used while
 * originating a loan; it is never persisted. In production the decision would come
 * from an external bureau call, but the scoring is inlined here for the demo.
 */
public class CreditDecision {

    private Long loanId;
    private Integer creditScore;
    private String decision;
    private BigDecimal maxApprovedAmount;
    private BigDecimal offeredRate;
    private Integer maxTermMonths;
    private String declineReason;
    private Date decisionDate;
    private String riskTier;

    public CreditDecision() {}

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public BigDecimal getMaxApprovedAmount() { return maxApprovedAmount; }
    public void setMaxApprovedAmount(BigDecimal maxApprovedAmount) { this.maxApprovedAmount = maxApprovedAmount; }

    public BigDecimal getOfferedRate() { return offeredRate; }
    public void setOfferedRate(BigDecimal offeredRate) { this.offeredRate = offeredRate; }

    public Integer getMaxTermMonths() { return maxTermMonths; }
    public void setMaxTermMonths(Integer maxTermMonths) { this.maxTermMonths = maxTermMonths; }

    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }

    public Date getDecisionDate() { return decisionDate; }
    public void setDecisionDate(Date decisionDate) { this.decisionDate = decisionDate; }

    public String getRiskTier() { return riskTier; }
    public void setRiskTier(String riskTier) { this.riskTier = riskTier; }
}
