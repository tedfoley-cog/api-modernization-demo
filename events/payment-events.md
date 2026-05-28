# Payment Processing Domain Events

Events published by the `payment-processing-service` bounded context.
These replace the synchronous cross-domain calls in the monolith.

## PaymentReceived

**Trigger**: Payment submitted by borrower
**Producers**: payment-processing-service
**Consumers**: account-servicing-service, reporting-service

| Field | Type | Description |
|---|---|---|
| paymentId | Long | Unique payment identifier |
| loanId | Long | Associated loan reference |
| paymentAmount | BigDecimal | Total payment amount |
| paymentMethod | String | ACH, EFT, CHECK, WIRE, DEALER_REMITTANCE |
| confirmationNumber | String | PMT-xxxxxxxx confirmation code |

## PaymentAllocated

**Trigger**: Payment broken down into principal, interest, and fee portions
**Producers**: payment-processing-service
**Consumers**: account-servicing-service

| Field | Type | Description |
|---|---|---|
| paymentId | Long | Payment that was allocated |
| loanId | Long | Associated loan reference |
| principalAmount | BigDecimal | Portion applied to principal |
| interestAmount | BigDecimal | Portion applied to interest |
| feeAmount | BigDecimal | Portion applied to outstanding fees |

## PaymentProcessed

**Trigger**: ACH/EFT payment completed successfully
**Producers**: payment-processing-service
**Consumers**: account-servicing-service, loan-origination-service, reporting-service

| Field | Type | Description |
|---|---|---|
| paymentId | Long | Completed payment identifier |
| loanId | Long | Associated loan reference |
| processedDate | Date | When processing completed |
| status | String | COMPLETED or FAILED |
| principalApplied | BigDecimal | Principal applied to balance |
| totalPaid | BigDecimal | Cumulative total of completed payments for this loan |

## LateFeesAssessed

**Trigger**: Account past due, late fee charged
**Producers**: payment-processing-service
**Consumers**: account-servicing-service, reporting-service

| Field | Type | Description |
|---|---|---|
| loanId | Long | Delinquent loan reference |
| feeAmount | BigDecimal | Calculated late fee amount |
| daysPastDue | int | Number of days past due date |
| assessmentDate | Date | When the fee was assessed |
