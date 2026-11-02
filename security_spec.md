# SweetLedger Security Specification

## 1. Data Invariants
1. **Multi-Tenant User Isolation**: Every document in `/users/{userId}/customers/{customerId}`, `/users/{userId}/inventory/{itemId}`, and `/users/{userId}/transactions/{transactionId}` belongs exclusively to `userId` (`request.auth.uid == userId` and `data.userId == request.auth.uid`).
2. **Strict Schema & Key Validation**: Every `create` and `update` operation must pass `isValidCustomer`, `isValidInventoryItem`, or `isValidTransaction` and strictly enforce allowed keys via `hasOnly` and required keys via `hasAll`.
3. **Immutable Metadata**: `userId` and `createdAt` cannot be modified on `update` (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be valid Firestore `timestamp` values (`<= request.time`).

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/Write**: `auth == null` reading `/users/alice/customers` -> REJECTED.
2. **Cross-User Read (PII Leak)**: `bob` reading `/users/alice/customers/c1` -> REJECTED.
3. **Identity Spoofing on Create**: `alice` creating `/users/alice/customers/c1` with `userId: "bob"` -> REJECTED.
4. **Shadow Update (Ghost Field Injection)**: `alice` updating customer with `isVerified: true` -> REJECTED by `hasOnly`.
5. **Immutable Field Tampering**: `alice` updating `createdAt` or `userId` on an existing customer -> REJECTED.
6. **Value Poisoning (Oversized Name)**: `name` with 500 characters -> REJECTED by `.size() <= 100`.
7. **Negative Stock Quantity**: `stockQuantity: -5` on inventory item -> REJECTED by `>= 0`.
8. **Negative Item Price**: `price: -10.0` on inventory item -> REJECTED by `>= 0`.
9. **Invalid Transaction Type Enum**: `type: "HACKED_TYPE"` on transaction -> REJECTED by enum check.
10. **ID Poisoning**: Document ID with special characters or > 128 chars -> REJECTED by `isValidId`.
11. **Missing Required Fields on Create**: Omitting `phone` or `createdAt` when creating a customer -> REJECTED by `hasAll`.
12. **Timestamp Spoofing in the Future**: `createdAt` set to future timestamp `> request.time` -> REJECTED.
