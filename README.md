# Reactive Ticketing Platform

Reactive backend for event ticketing with temporary reservations, asynchronous order processing, inventory consistency under concurrency, and automatic expiration release.

## 1. Implemented Requirements

### Functional requirements

- Event management:
  - Create events (`POST /api/events`)
  - List events (`GET /api/events`)
- Temporary ticket reservations:
  - Orders reserve inventory immediately for a configurable hold period (default: 10 minutes)
- Asynchronous order processing:
  - Orders are published to a queue adapter and processed asynchronously by a consumer
  - Processing flow: `RESERVED -> PENDING_CONFIRMATION -> SOLD`
- Order status query:
  - `GET /api/orders/{orderId}` returns full order state and audit trail
- Concurrency control:
  - Optimistic locking + compare-and-set updates over event and order version counters
  - Prevents oversell under high concurrent demand
- Automatic release of expired reservations:
  - Scheduled task scans `RESERVED` and `PENDING_CONFIRMATION` orders and restores inventory
- Reactive availability query:
  - `GET /api/events/{eventId}/availability`
  - Returns total, available, reserved, sold, and complimentary counters

### Technical requirements

- Spring Boot + Spring WebFlux (`Mono` / `Flux`)
- Clean architecture organization:
  - `domain`
  - `application` (ports + use cases/services)
  - `infrastructure` (web, persistence, messaging, scheduler, config)
- Reactive retries:
  - Queue processing retry policy
  - Optimistic-lock retry loops for concurrent updates
- Inventory consistency:
  - Conditional writes via version-based compare-and-set
- Dockerized delivery:
  - `Dockerfile`
  - `docker-compose.yml` with app + MongoDB + LocalStack (SQS service)

## 2. Architecture

```text
WebFlux Controllers
    -> Application Use Cases
        -> Domain Rules (Event, Order, transitions, audit)
        -> Ports (EventRepository, OrderRepository, OrderQueuePort, ClockPort)
            -> Infrastructure Adapters
                - MongoDB repositories with conditional writes
                - SQS adapter on LocalStack + async consumer with ack
                - Scheduled expiration releaser
```

### Core domain model

- `TicketState`: `AVAILABLE`, `RESERVED`, `PENDING_CONFIRMATION`, `SOLD`, `COMPLIMENTARY`
- `Event` aggregate:
  - `reserve`, `confirmSale`, `releaseReservation`, `grantComplimentary`
- `Order` aggregate:
  - immutable transitions
  - audit trail entries for atomic/auditable changes

## 3. How to Run

## Prerequisites

- Java 21
- Maven 3.8+
- Docker + Docker Compose (optional, for containerized run)

## Local run

```bash
mvn clean verify
mvn spring-boot:run
```

App URL: `http://localhost:8080`

## Docker run

```bash
docker compose up --build
```

Services exposed:

- App: `localhost:8080`
- MongoDB: `localhost:27017`
- LocalStack (SQS): `localhost:4566`

## 4. Main Endpoints

### Create event

`POST /api/events`

```json
{
  "name": "Rock Festival",
  "date": "2026-12-01T20:00:00Z",
  "venue": "National Stadium",
  "totalCapacity": 5000
}
```

### List events

`GET /api/events`

### Event availability

`GET /api/events/{eventId}/availability`

### Create order (async flow)

`POST /api/orders`

```json
{
  "eventId": "replace-with-event-id",
  "customerId": "customer-123",
  "quantity": 2
}
```

### Query order status

`GET /api/orders/{orderId}`

## 5. cURL Collection

See `scripts/curl-collection.sh`.

## 6. Testing

The project includes:

- Unit tests for use cases with mocks
- Reactive WebFlux component tests for controllers
- Concurrency integration test to validate no oversell
- Coverage gate at **90% line coverage** enforced by JaCoCo

Run:

```bash
mvn verify
```

## 7. Design Decisions

- **Immutable domain models** simplify reasoning about state transitions
- **Optimistic locking** is used to protect inventory consistency in concurrent scenarios
- **Asynchronous order pipeline** decouples request latency from heavy processing
- **Scheduled release process** ensures temporary reservations never lock inventory indefinitely
- **Port-driven architecture** keeps infrastructure replaceable (for example, switching between MongoDB/SQS and in-memory adapters by configuration)
