"""Generate event catalog — identify what domain events should exist based on service analysis."""


# State changes that should become domain events in event-driven architecture
EVENT_TEMPLATES = {
    "loan-origination": [
        {
            "event_name": "LoanApplicationSubmitted",
            "trigger": "New loan application created",
            "current_implementation": "Synchronous method call in LoanService.createApplication()",
            "producers": ["loan-origination-service"],
            "consumers": ["credit-decisioning", "dealer-integration-service"],
            "payload_fields": [
                "applicationId", "applicationNumber", "applicantName",
                "requestedAmount", "vehicleVin", "dealerId",
            ],
        },
        {
            "event_name": "CreditDecisionMade",
            "trigger": "Credit check completed (approved or declined)",
            "current_implementation": "Inline in LoanService.performCreditCheck() — synchronous, no event",
            "producers": ["loan-origination-service"],
            "consumers": ["account-servicing-service", "dealer-integration-service", "reporting-service"],
            "payload_fields": [
                "applicationId", "decision", "creditScore",
                "riskTier", "approvedAmount", "offeredRate",
            ],
        },
        {
            "event_name": "LoanFunded",
            "trigger": "Loan moves to FUNDED status",
            "current_implementation": "LoanService.fundLoan() synchronously updates account and deal package",
            "producers": ["loan-origination-service"],
            "consumers": ["account-servicing-service", "dealer-integration-service", "reporting-service"],
            "payload_fields": [
                "loanId", "applicationNumber", "fundingDate",
                "approvedAmount", "interestRate", "termMonths",
            ],
        },
    ],
    "payment-processing": [
        {
            "event_name": "PaymentReceived",
            "trigger": "Payment submitted by borrower",
            "current_implementation": "PaymentService.submitPayment() — synchronous, calls allocatePayment inline",
            "producers": ["payment-processing-service"],
            "consumers": ["account-servicing-service", "reporting-service"],
            "payload_fields": [
                "paymentId", "loanId", "paymentAmount",
                "paymentMethod", "confirmationNumber",
            ],
        },
        {
            "event_name": "PaymentAllocated",
            "trigger": "Payment broken down into principal/interest/fees",
            "current_implementation": "Inline in PaymentService.allocatePayment() — synchronous",
            "producers": ["payment-processing-service"],
            "consumers": ["account-servicing-service"],
            "payload_fields": [
                "paymentId", "loanId", "principalAmount",
                "interestAmount", "feeAmount",
            ],
        },
        {
            "event_name": "PaymentProcessed",
            "trigger": "ACH/EFT payment completed successfully",
            "current_implementation": "PaymentService.processAchPayment() synchronously updates account balance and loan status",
            "producers": ["payment-processing-service"],
            "consumers": ["account-servicing-service", "loan-origination-service", "reporting-service"],
            "payload_fields": [
                "paymentId", "loanId", "processedDate",
                "status", "principalApplied", "newBalance",
            ],
        },
        {
            "event_name": "LateFeesAssessed",
            "trigger": "Account past due, late fee charged",
            "current_implementation": "PaymentService.assessLateFee() — called synchronously from LoanService.runEndOfDayProcessing()",
            "producers": ["payment-processing-service"],
            "consumers": ["account-servicing-service", "reporting-service"],
            "payload_fields": [
                "loanId", "feeAmount", "daysPastDue", "assessmentDate",
            ],
        },
    ],
    "account-servicing": [
        {
            "event_name": "AccountCreated",
            "trigger": "New account created after loan approval",
            "current_implementation": "Inline in LoanService.createApplication() — wrong service creates the account",
            "producers": ["account-servicing-service"],
            "consumers": ["reporting-service"],
            "payload_fields": [
                "accountId", "accountNumber", "loanId",
                "customerName", "originalBalance",
            ],
        },
        {
            "event_name": "AccountBalanceUpdated",
            "trigger": "Balance changes after payment, adjustment, or payoff",
            "current_implementation": "PaymentService.updateAccountBalance() — payment service directly mutates account state",
            "producers": ["account-servicing-service"],
            "consumers": ["reporting-service", "loan-origination-service"],
            "payload_fields": [
                "accountId", "previousBalance", "newBalance",
                "changeAmount", "changeReason",
            ],
        },
        {
            "event_name": "AccountDelinquent",
            "trigger": "Account crosses delinquency threshold",
            "current_implementation": "LoanService.runEndOfDayProcessing() — loan service manages account delinquency",
            "producers": ["account-servicing-service"],
            "consumers": ["payment-processing-service", "reporting-service"],
            "payload_fields": [
                "accountId", "loanId", "daysPastDue",
                "delinquencyBucket", "currentBalance",
            ],
        },
    ],
    "dealer-integration": [
        {
            "event_name": "DealPackageSubmitted",
            "trigger": "Dealer submits a deal package",
            "current_implementation": "DealerService.submitDealPackage() synchronously creates loan application",
            "producers": ["dealer-integration-service"],
            "consumers": ["loan-origination-service"],
            "payload_fields": [
                "dealNumber", "dealerId", "vehicleVin",
                "salePrice", "downPayment", "tradeInValue",
            ],
        },
        {
            "event_name": "DealerSettlementCalculated",
            "trigger": "Settlement amounts computed for dealer",
            "current_implementation": "DealerService.getDealerSettlement() — computed on-demand, not event-driven",
            "producers": ["dealer-integration-service"],
            "consumers": ["reporting-service"],
            "payload_fields": [
                "dealerId", "dealerCode", "totalReserves",
                "totalHoldbacks", "netSettlement",
            ],
        },
    ],
    "reporting": [],
}


def generate_event_catalog(
    services: list[dict],
    boundaries: list[dict],
) -> list[dict]:
    """Generate the full event catalog for all identified domains."""
    catalog = []
    for boundary in boundaries:
        domain = boundary["domain"]
        events = EVENT_TEMPLATES.get(domain, [])
        for event in events:
            catalog.append({
                **event,
                "domain": domain,
                "bounded_context": boundary["proposed_service"],
            })
    return catalog
