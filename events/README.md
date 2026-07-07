# Domain Event Catalog

Events defined during the Payment bounded context extraction. In production, each
event becomes a Kafka topic (or Avro/Protobuf schema) with `loanId` as the partition
key for ordering guarantees.

## Payment Events

| Event | Publisher | Subscribers | Kafka Topic (prod) |
|---|---|---|---|
| `PaymentReceived` | Payment Service | Account Service, Reporting | `payment.received` |
| `PaymentCompleted` | Payment Service | Account Service, Loan Service | `payment.completed` |
| `PaymentFailed` | Payment Service | Notification Service | `payment.failed` |
| `LateFeeAssessed` | Payment Service | Account Service, Notification | `payment.late-fee` |

## Event Schemas

### PaymentReceived
```json
{
  "eventId": "uuid",
  "eventType": "PaymentReceived",
  "occurredAt": "2025-06-01T09:00:00Z",
  "loanId": 1,
  "paymentId": 42,
  "totalAmount": 525.89,
  "principalAmount": 430.12,
  "interestAmount": 95.77,
  "feeAmount": 0.00,
  "confirmationNumber": "PMT-A1B2C3D4"
}
```

### PaymentCompleted
```json
{
  "eventId": "uuid",
  "eventType": "PaymentCompleted",
  "occurredAt": "2025-06-01T09:05:00Z",
  "loanId": 1,
  "paymentId": 42,
  "principalAmount": 430.12,
  "totalPaidToDate": 1051.78,
  "confirmationNumber": "PMT-A1B2C3D4"
}
```

### PaymentFailed
```json
{
  "eventId": "uuid",
  "eventType": "PaymentFailed",
  "occurredAt": "2025-06-01T09:05:00Z",
  "loanId": 1,
  "paymentId": 42,
  "reason": "Invalid ACH routing number",
  "confirmationNumber": "PMT-X9Y8Z7W6"
}
```

### LateFeeAssessed
```json
{
  "eventId": "uuid",
  "eventType": "LateFeeAssessed",
  "occurredAt": "2025-06-10T00:00:00Z",
  "loanId": 8,
  "feeAmount": 25.00,
  "daysPastDue": 16,
  "confirmationNumber": "FEE-K7L8M9N0"
}
```

## Production Considerations

- **Broker**: Apache Kafka with replication factor >= 3
- **Serialization**: Avro with Schema Registry for contract evolution
- **Ordering**: Partition by `loanId` to guarantee per-loan event ordering
- **Delivery**: At-least-once with idempotent consumers (dedup by `eventId`)
- **Retention**: 7-day log retention + compact topics for state snapshots
