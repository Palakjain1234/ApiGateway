# GateKeeper — Multi-Tenant API Gateway

A production-grade API Gateway built with Spring Boot microservices.
Demonstrates multi-tenancy, JWT-based authentication, rate limiting,
idempotency, configuration versioning, audit logging, and event-driven
architecture.

---

## Architecture

```
                         GateKeeper
              ┌──────────────────────────────┐
              │      API Management          │  Control Plane
              │  Organizations, Routes, Auth │  Port: 8080
              └──────────────┬───────────────┘
                             │ route config
                             ▼
                         MySQL 8.0
                             ▲
                             │ read active routes
              ┌──────────────┴───────────────┐
              │      Smart Gateway           │  Data Plane
              │  Match → Policy → Forward    │  Port: 8081
              └──┬───────────────────────┬───┘
                 │                       │
          Redis (rate limit        Kafka (access events,
           idempotency)             route changes)
                                        │
              ┌─────────────────────────┘
              │      Analytics Service       │  Port: 8082
              │  Consumes events → MongoDB   │
              └──────────────────────────────┘

Demo backends:
  catalog-service  (port 8091)
  order-service    (port 8092)
```

---

## Services

| Service               | Port | Role                                   |
|-----------------------|------|----------------------------------------|
| api-management-service| 8080 | Control plane — orgs, routes, auth     |
| smart-gateway-service | 8081 | Data plane — proxy + policy engine     |
| analytics-service     | 8082 | Event consumer — MongoDB stats         |
| catalog-service       | 8091 | Demo tenant backend (catalog API)      |
| order-service         | 8092 | Demo tenant backend (order API)        |

---

## Infrastructure

| Component   | Port  | Purpose                              |
|-------------|-------|--------------------------------------|
| MySQL 8.0   | 3306  | API Management data                  |
| MongoDB 7.0 | 27017 | Analytics event documents            |
| Redis 7.2   | 6379  | Rate limiting + idempotency state    |
| Kafka       | 9092  | Route-change + access-event topics   |
| Zookeeper   | 2181  | Required by Kafka                    |

---

## Running the Project

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker + Docker Compose

### 1. Build all JARs

```bash
mvn clean package -DskipTests
```

### 2. Start everything

```bash
docker-compose up -d
```

All 5 infra services and 5 application services start automatically.

### 3. Check health

```bash
curl http://localhost:8080/health   # API Management
curl http://localhost:8081/health   # Smart Gateway
curl http://localhost:8082/health   # Analytics
```

### 4. Stop everything

```bash
docker-compose down
docker-compose down -v   # also wipes volumes
```

---

## API Quick Reference

### Authentication

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{ "username": "platform-admin", "password": "admin123" }
```

Returns a JWT. Add it to all subsequent requests:
```
Authorization: Bearer <token>
```

### Organization Management (PLATFORM_ADMIN)

```
GET    /api/organizations                     List all organizations
POST   /api/organizations                     Create organization
PUT    /api/organizations/{id}/activate       Activate
PUT    /api/organizations/{id}/suspend        Suspend
DELETE /api/organizations/{id}                Soft delete
POST   /api/organizations/{id}/admin          Create tenant admin
```

### Route Management (TENANT_ADMIN)

```
GET    /api/organizations/{orgId}/routes                    List routes
POST   /api/organizations/{orgId}/routes                    Create route
PUT    /api/organizations/{orgId}/routes/{routeId}          Update route
PUT    /api/organizations/{orgId}/routes/{routeId}/activate   Activate
PUT    /api/organizations/{orgId}/routes/{routeId}/deactivate Deactivate
DELETE /api/organizations/{orgId}/routes/{routeId}          Delete route
GET    /api/organizations/{orgId}/routes/{routeId}/history  Version history
```

### Gateway Config (GATEWAY_SERVICE)

```
GET    /api/gateway/routes/active    All active routes across all orgs
```

### Analytics

```
GET    /api/analytics/platform                           Platform-wide stats
GET    /api/analytics/org/{slug}?from=...&to=...         Org stats by time range
GET    /api/analytics/route/{routeId}?from=...&to=...    Route stats
```

---

## Roles

| Role             | Can Do                                          |
|------------------|-------------------------------------------------|
| PLATFORM_ADMIN   | Manage organizations and tenant admin accounts  |
| TENANT_ADMIN     | Manage routes for their own organization only   |
| GATEWAY_SERVICE  | Read active route config (read-only)            |

---

## Key Design Decisions

- **Control Plane / Data Plane separation** — API Management owns config; Smart Gateway handles live traffic. They never share a database.
- **Tenant-scoped route identity** — route identity is `(organizationId + routeId)`. Two orgs can both have a route named `orders`.
- **Configuration versioning** — every route mutation creates an immutable snapshot. History survives route deletion (no FK to live route).
- **Audit logging via AOP** — `@Auditable` annotation on service methods; `AuditAspect` saves records without polluting business logic.
- **Fail-closed policies** — if Redis is unavailable, rate-limit and idempotency checks reject requests rather than silently bypassing policy.
- **In-memory route registry** — Smart Gateway loads routes at startup and refreshes on Kafka events. No database call on the hot path.
- **Event-driven analytics** — Gateway publishes access events to Kafka; Analytics consumes asynchronously. The client never waits for analytics.

---

## Seeded Accounts

| Username        | Password     | Role             |
|-----------------|--------------|------------------|
| platform-admin  | admin123     | PLATFORM_ADMIN   |
| gateway-service | gateway123   | GATEWAY_SERVICE  |
