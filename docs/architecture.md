# Architecture

## Purpose

Beans on Time uses Domain-Driven Design and Hexagonal Architecture to keep business
rules independent from frameworks, persistence technologies, and delivery mechanisms.

The architecture should optimize for:

- clear domain boundaries,
- explicit dependency direction,
- replaceable infrastructure,
- testability,
- maintainability,
- incremental evolution.

Architecture is not an end in itself.

Prefer the simplest design that preserves these boundaries and correctly expresses the
business requirement.

---

# Architectural Style

The project follows Hexagonal Architecture.

The conceptual dependency flow is:

Adapter In
↓
Input Port
↓
Application Service
↓
Domain
↓
Output Port
↑
Adapter Out

Dependencies must point toward the application and domain core.

Outer layers may depend on inner layers.

Inner layers must not depend on outer infrastructure.

---

# Package Structure

The project is organized primarily by business capability.

Example:

com.bluetoya.beansontime
├─ subscription
│  ├─ domain
│  ├─ application
│  │  ├─ port
│  │  │  ├─ in
│  │  │  └─ out
│  │  └─ service
│  └─ adapter
│     ├─ in
│     │  └─ web
│     └─ out
│        └─ persistence
│
├─ product
│  ├─ domain
│  ├─ application
│  └─ adapter
│
├─ customer
│  └─ domain
│
├─ seller
│  └─ domain
│
└─ security
├─ application
├─ adapter
├─ annotation
├─ aspect
└─ config

Do not introduce packages based only on Java implementation types.

Avoid structures such as:

record/
enum/
interface/
impl/

Prefer business ownership and architectural responsibility.

---

# Domain Layer

The Domain layer contains business concepts, state, behavior, and invariants.

The Domain layer must not depend on:

- Spring Framework,
- Spring Security,
- persistence frameworks,
- HTTP,
- database-specific concepts.

Aggregates and Value Objects should remain plain Java domain models.

Business behavior belongs in the Domain when the rule can be expressed by the domain
object itself.

Prefer:

subscription.pause();

over:

subscription.setStatus(PAUSED);

when pause is an actual business operation.

---

# Application Layer

The Application layer coordinates use cases.

Application Services may:

- load Aggregates,
- invoke Domain behavior,
- save Aggregates,
- coordinate multiple ports,
- acquire authenticated actor information through application-facing abstractions,
- compose read models.

Application Services should not duplicate rules that naturally belong to Domain objects.

The Application layer may define:

- Input Ports,
- Output Ports,
- Commands,
- Query Results,
- Use Case Services.

---

# Adapter Layer

Adapters connect external mechanisms to application ports.

Inbound adapter examples:

- REST Controller

Outbound adapter examples:

- In-memory persistence
- JDBC persistence
- external API clients

Adapters may depend on framework-specific APIs.

Framework-specific types must not leak into the Domain layer.

---

# Ports

## Input Ports

Input Ports represent application use cases.

Use business-oriented terminology whenever possible.

Examples:

RegisterProductUseCase
PauseSubscriptionUseCase
ResumeSubscriptionUseCase
FindProductQuery

Prefer:

register()
subscribe()
pause()
resume()

over generic CRUD terminology when a business operation exists.

---

## Output Ports

Output Ports express capabilities required by the Application layer.

Examples:

SaveProductPort
LoadSubscriptionPort
FindProductQueryPort
ExistsSubscriptionPort

Output Ports must not expose the implementation technology in their name.

Avoid:

MysqlProductPort
RedisSubscriptionPort

---

# Port Naming Convention

Beans on Time currently uses the following convention:

save
- persist an Aggregate

load
- load or reconstruct an Aggregate for command-side domain behavior

find
- retrieve query/read data

exists
- check existence

This is a project convention, not a universal industry standard.

Maintain consistency unless a use case requires a clearer name.

---

# Command and Query Separation

The project follows Command Query Separation.

## Command Side

The Command side changes state.

Typical flow:

Input
→ Application Service
→ Load Aggregate
→ Execute Domain behavior
→ Save Aggregate

Command operations should use real Aggregates when Domain behavior is required.

---

## Query Side

The Query side reads data.

Queries may return dedicated projections instead of Aggregates.

Examples:

ProductQueryResult
SubscriptionQueryResult

A query projection does not need to mirror the Aggregate structure.

Cross-domain information may be composed on the Query side.

Example:

Subscription data
+
Product data
→ SubscriptionQueryResult

Do not add Product as an object inside the Subscription Aggregate merely because a query
needs Product information.

---

# Cross-Aggregate References

Aggregates reference other Aggregates by identity.

Example:

Subscription
- SubscriptionId
- CustomerId
- ProductId

Product
- ProductId
- SellerId

Avoid Aggregate object graphs such as:

Subscription
└─ Product
└─ Seller

unless a deliberate Aggregate boundary redesign justifies it.

---

# Incremental Architecture

Do not introduce abstractions for hypothetical future requirements.

Prefer:

real requirement
→ concrete implementation
→ observe duplication or change pressure
→ introduce abstraction

over:

possible future requirement
→ generic abstraction
→ force current features into the abstraction

Temporary duplication is acceptable when it helps discover the correct abstraction.

Architectural consistency means applying the same principle to equivalent problems,
not making every use case structurally identical.