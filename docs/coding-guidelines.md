# Coding Guidelines

## Purpose

Code should communicate business intent clearly and remain easy to change.

Prefer clarity over cleverness.

---

# Naming

Use names that express business meaning.

Prefer:

RegisterProductService
PauseSubscriptionUseCase
SaveProductPort
SellerId

Avoid vague names such as:

Manager
Processor
Helper
Util
CommonService

unless the name genuinely represents the responsibility.

---

# Business Vocabulary

Use business terminology consistently across:

- Domain classes,
- Use Cases,
- Ports,
- tests,
- documentation.

When the business says "subscribe", prefer subscribe over create.

When the business says "pause", prefer pause over updateStatus.

---

# Methods

Methods should have one understandable responsibility.

Prefer short orchestration methods whose steps communicate the use case.

Extract methods when doing so clarifies intent.

Do not extract every small expression merely to reduce line count.

---

# State

Prefer immutable state where practical.

Do not expose unrestricted setters on Domain objects.

State changes should preferably occur through meaningful Domain behavior.

---

# Dependencies

Use constructor injection.

Dependencies should be explicit.

Avoid static access to infrastructure from Application or Domain code.

Framework dependencies must remain in appropriate outer layers.

---

# Primitive Obsession

Use Domain-specific types when they improve semantics or safety.

Prefer:

SellerId
CustomerId
ProductId

over passing unrelated long values throughout business code.

Do not wrap primitives when the wrapper adds no useful domain meaning.

---

# Abstraction

Do not generalize based only on similar code.

Ask whether two concepts share the same semantics and change reasons.

Prefer temporary duplication over an incorrect abstraction.

Avoid generic frameworks created solely for possible future reuse.

---

# Comments

Code should primarily explain itself through naming and structure.

Use comments to explain:

- why a non-obvious decision exists,
- architectural constraints,
- important trade-offs,
- temporary limitations.

Do not use comments to restate obvious implementation details.

---

# Scope of Changes

Keep changes focused on the requested task.

Avoid unrelated cleanup while implementing a feature.

Large refactors should be intentional and separately reviewable when practical.

Do not silently replace an established project convention with a new one.

---

# Error Handling

Use exceptions that communicate business meaning.

Avoid broad catch blocks unless recovery or translation is intentional.

Do not swallow exceptions.

Do not introduce generic exception hierarchies without demonstrated value.

---

# Framework Usage

Framework convenience must not override architectural boundaries.

Do not expose Spring Security types to the Domain.

Do not add persistence annotations to Domain models merely for adapter convenience.

Do not introduce a framework dependency when plain Java can express the Domain concept.

---

# Simplicity

Implement the smallest coherent solution that satisfies the current requirement.

Do not design for imaginary future features.

Allow the architecture to evolve as real requirements reveal new boundaries.