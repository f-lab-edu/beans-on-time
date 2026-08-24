# Testing Strategy

## Purpose

Tests are part of the project's design feedback loop.

Tests should verify meaningful behavior while respecting architectural boundaries.

Do not pursue coverage numbers at the expense of useful tests.

---

# Domain Unit Tests

Domain tests should use real Domain objects.

Do not mock the Aggregate under test.

Test:

- invariants,
- state transitions,
- Value Object validation,
- Domain behavior.

Example:

Given an ACTIVE Subscription
When pause() is called
Then the Subscription becomes PAUSED

Also verify invalid transitions.

---

# Application Unit Tests

Application tests should verify use-case orchestration.

Prefer:

- real Domain objects,
- mocked Output Ports and external collaborators.

Typical checks include:

- correct Domain behavior is triggered,
- expected object is persisted,
- query result is returned,
- required collaborator is invoked.

Do not mock Domain behavior merely to make an Application Service test easier.

---

# Query Tests

Queries should primarily verify returned data.

Do not assert only that a mocked port was called if the Query's observable contract is its
return value.

If a query combines multiple data sources, verify the resulting projection.

---

# Adapter Tests

Adapters should be tested when they contain meaningful behavior such as:

- HTTP mapping,
- persistence mapping,
- SecurityContext translation,
- serialization,
- external API mapping.

Simple delegation does not automatically require extensive tests.

---

# Security Tests

SecurityCurrentCustomerProvider and SecurityCurrentSellerProvider may be tested without a
full Spring Context by setting SecurityContextHolder directly.

Always clear SecurityContextHolder after tests.

Verify at minimum:

- valid principal returns the expected Domain identifier,
- missing or incompatible authentication produces the expected authentication failure.

---

# AOP Authorization Tests

Separate authorization logic testing from Spring AOP wiring testing.

## Authorization Logic Test

The Aspect advice may be directly invoked to verify:

- owner succeeds,
- non-owner receives AccessDeniedException.

This validates authorization logic.

## Proxy Wiring Test

When necessary, add a small Spring test proving:

annotation
→ Spring proxy
→ Aspect advice
→ authorization result

Do not use a full application context if a smaller configuration can prove the wiring.

---

# Given / When / Then

Prefer tests structured conceptually as:

Given
When
Then

The structure should emphasize scenario behavior rather than framework implementation.

---

# Assertions

Prefer assertions on observable results and state.

Use interaction verification when the interaction itself is part of the contract.

Examples where verification may be useful:

- an Aggregate must be saved after a successful command,
- an external side effect must occur exactly once.

Avoid excessive verify() calls that couple tests to internal implementation structure.

---

# Fixtures

Shared fixtures may reduce repetitive object construction.

A fixture should return a new Aggregate instance for each test.

Avoid shared mutable static Domain objects.

Prefer simple fixture methods over complex test-builder frameworks unless test setup
actually becomes difficult to manage.

---

# Test Scope

Use the smallest test scope that proves the behavior.

Prefer:

Domain unit test
→ Application unit test
→ focused adapter/integration test

before defaulting to:

@SpringBootTest

Full-context tests are useful only when the full application wiring is what needs to be
verified.

---

# Completion Rule

When production behavior changes:

- add or update relevant tests,
- run targeted tests during implementation,
- run the broader relevant test suite before completion.

Do not claim tests pass unless they were actually executed successfully.