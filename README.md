# Digital Banking & Fraud Detection System

A distributed digital banking backend built with **Java, Spring Boot, Microservices, Apache Kafka, Redis, MySQL, and Razorpay**.

The system demonstrates how a banking platform can process account transfers through an event-driven workflow, perform **real-time fraud detection**, require **OTP verification for suspicious transactions**, execute **SAGA-style compensation/refunds**, process external payments through Razorpay, and send asynchronous email notifications.

---

## 🚀 Key Features

- Microservices-based banking architecture
- API Gateway as the entry point for backend services
- Account management with Savings, Current, and Fixed Deposit account types
- Account balance management with account blocking support
- SAGA-style transaction workflow
- Apache Kafka for asynchronous event-driven communication
- Redis-powered real-time fraud detection
- Velocity-based fraud detection
- Unusual transaction amount detection
- Balance-percentage fraud detection
- OTP-based verification for suspicious transactions
- Automatic account blocking after rejected verification
- SAGA compensation/refund when a transaction is cancelled
- Razorpay payment order creation and webhook processing
- Event-driven email notifications
- Transaction history and transaction status tracking
- Docker-based service deployment

---

## 🏗️ High-Level Architecture

```text
                                ┌─────────────────┐
                                │      User       │
                                └────────┬────────┘
                                         │
                                         ▼
                                ┌─────────────────┐
                                │   API Gateway   │
                                └────────┬────────┘
                                         │
                         ┌───────────────┴────────────────┐
                         │                                │
                         ▼                                ▼
                ┌─────────────────┐              ┌─────────────────┐
                │ Transaction     │              │ Payment Service │
                │    Service      │              └────────┬────────┘
                └────────┬────────┘                       │
                         │                                ▼
                         │                           ┌──────────┐
                         │                           │ Razorpay │
                         │                           └────┬─────┘
                         │                                │
                         │                              Webhook
                         │
                         │ transaction.initiated
                         ▼
                ┌─────────────────┐
                │ Fraud Detection │
                │    Service      │
                └────────┬────────┘
                         │
                         ▼
                    ┌─────────┐
                    │  Redis  │
                    └─────────┘
                         │
                ┌────────┴────────┐
                │                 │
              CLEAN           SUSPICIOUS
                │                 │
                ▼                 ▼
       fraud.check.clean   verification.required
                │                 │
                │                 ▼
                │          Transaction Service
                │                 │
                │           Generate OTP
                │                 │
                │                 ▼
                │          Redis OTP Store
                │                 │
                │                 ▼
                │      transaction.otp-generated
                │                 │
                │                 ▼
                │        Notification Service
                │                 │
                │                 ▼
                │               Email
                │
                └──────────┬──────────────┐
                           │              │
                           ▼              ▼
                  transaction.completed  fraud.detected
                           │              │
                    ┌──────┴──────┐       ├──► Account Service
                    │             │       │     └── Block account
                    ▼             ▼       │
             Account Service   Notification│
             Credit Receiver      Service   │
                                 │          │
                                 ▼          ▼
                               Email     Compensation
                                           │
                                           ▼
                                      Refund Sender
