# Payment Processing — Example Event Instances

One concrete example per payment event. Each instance is valid against its
JSON Schema in this directory. Values follow a single illustrative scenario: a
$450.00 ACH payment on loan `1001` that is allocated, processed, and (in a
separate past-due scenario) assessed a late fee.

## PaymentReceived

Validates against [`payment.received.schema.json`](./payment.received.schema.json).

```json
{
  "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "eventType": "PaymentReceived",
  "occurredAt": "2026-06-08T14:30:00Z",
  "version": 1,
  "data": {
    "paymentId": 50001,
    "loanId": 1001,
    "paymentAmount": 450.00,
    "paymentMethod": "ACH",
    "confirmationNumber": "PMT-20260608-50001"
  }
}
```

## PaymentAllocated

Validates against [`payment.allocated.schema.json`](./payment.allocated.schema.json).

```json
{
  "eventId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
  "eventType": "PaymentAllocated",
  "occurredAt": "2026-06-08T14:30:01Z",
  "version": 1,
  "data": {
    "paymentId": 50001,
    "loanId": 1001,
    "principalAmount": 312.45,
    "interestAmount": 137.55,
    "feeAmount": 0.00
  }
}
```

## PaymentProcessed

Validates against [`payment.processed.schema.json`](./payment.processed.schema.json).

```json
{
  "eventId": "b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e",
  "eventType": "PaymentProcessed",
  "occurredAt": "2026-06-08T14:30:05Z",
  "version": 1,
  "data": {
    "paymentId": 50001,
    "loanId": 1001,
    "processedDate": "2026-06-08T14:30:05Z",
    "status": "COMPLETED",
    "principalApplied": 312.45,
    "newBalance": 18250.30
  }
}
```

## LateFeesAssessed

Validates against [`payment.late_fees_assessed.schema.json`](./payment.late_fees_assessed.schema.json).

```json
{
  "eventId": "c3d4e5f6-a7b8-4c9d-8e0f-2a3b4c5d6e7f",
  "eventType": "LateFeesAssessed",
  "occurredAt": "2026-06-08T00:05:00Z",
  "version": 1,
  "data": {
    "loanId": 1001,
    "feeAmount": 35.00,
    "daysPastDue": 17,
    "assessmentDate": "2026-06-08T00:05:00Z"
  }
}
```
