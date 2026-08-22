# AGENTS.md

## Project Overview

Beans on Time is a coffee-bean subscription service designed as a backend engineering
portfolio and learning project.

The primary goal is not only to make features work, but to maintain a high-quality
domain model and architectural boundaries while implementing realistic business use cases.

Core business areas include:

- Product registration and product queries
- Coffee subscription lifecycle management
- Recurring billing and payment
- Per-cycle order generation
- Payment failure retry and recovery
- Subscription pause and resume

The project intentionally prioritizes:

1. Domain-Driven Design
2. Hexagonal Architecture
3. Explicit architectural boundaries
4. Clean and maintainable code
5. Meaningful automated tests
6. Simple designs that can evolve when requirements appear

Do not sacrifice these principles merely to reduce the amount of code.

---

## Technology Stack

- Java 25
- Spring Boot 4
- Gradle 9.x
- Spring MVC
- Spring Security
- JUnit 5
- Mockito
- AssertJ

Persistence is currently implemented or introduced through outbound adapters.

Do not introduce JPA, Hibernate, WebFlux, or another major framework unless explicitly
required by the task.

The Domain layer must remain independent of Spring and persistence frameworks.

---

# Working Principles for AI Agents

Before changing code:

1. Inspect the existing package and nearby implementations.
2. Identify the Aggregate, Use Case, Port, and Adapter involved.
3. Follow existing project conventions before introducing a new pattern.
4. Determine whether the requested behavior is:
    - a domain rule,
    - application orchestration,
    - infrastructure concern,
    - web concern,
    - authentication/authorization concern.
5. Prefer the smallest change that correctly satisfies the requirement.

Do not introduce a new abstraction only because duplication might occur in the future.

Prefer:

- concrete domain language,
- explicit dependencies,
- small vertical slices,
- incremental design evolution.

Avoid:

- speculative generalization,
- unnecessary framework abstractions,
- generic `common`, `util`, `manager`, or `helper` classes,
- large unrelated refactors,
- changing working code outside the requested scope.

When an architectural decision is unclear, preserve the existing architecture and make the
least invasive choice.

---

# Domain-Driven Design Rules

## Aggregate Boundaries

Treat Aggregate boundaries as business consistency boundaries.

An Aggregate must:

- protect its own invariants,
- own its state transitions,
- expose behavior-oriented methods where business behavior exists.

Do not move domain behavior into Application Services merely to simplify the Aggregate.

Example:

```java
subscription.pause();
subscription.resume();
```

## Project Documentation

Before making architectural or domain changes, consult the relevant documentation:

- `docs/architecture.md` — architecture, dependency direction, ports, CQS
- `docs/domain-model.md` — Aggregate boundaries and domain ownership
- `docs/security.md` — authentication and authorization boundaries
- `docs/testing.md` — testing strategy
- `docs/coding-guidelines.md` — code-quality conventions
- `docs/adr/` — historical architectural decisions

Treat these documents as the project's source of truth.