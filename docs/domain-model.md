# Domain Model

## Purpose

This document records the current Domain model and Aggregate boundaries of Beans on Time.

The model should evolve with actual business requirements.

Do not treat this document as justification for speculative modeling.

---

# Subscription

Subscription is an Aggregate Root representing a Customer's recurring coffee delivery
subscription.

Current concepts include:

- SubscriptionId
- CustomerId
- ProductId
- Cycle
- SubscriptionStatus

Subscription owns its lifecycle and state transitions.

Current state behavior includes:

ACTIVE → PAUSED
PAUSED → ACTIVE

Business operations should be expressed through behavior-oriented methods.

Examples:

subscription.pause();
subscription.resume();

Invalid transitions should be rejected by the Domain.

---

## Subscription References

Customer and Product have independent lifecycles.

Subscription therefore keeps references using identifiers.

Subscription must not contain Customer or Product Aggregate objects.

Example:

Subscription
- CustomerId
- ProductId

---

# Product

Product is an Aggregate Root representing a coffee product offered by a Seller.

Product is owned and managed by a Seller while being publicly visible to Customers.

Current minimum use cases:

- Product registration
- Public single Product query

Initial Product modeling should remain minimal and expand only when real requirements
appear.

Potential concepts include:

- ProductId
- SellerId
- Name
- Description
- Price
- ProductStatus
- ProductImage
- supported grind types
- size options

Do not introduce all potential concepts before they are required.

---

## Product Ownership

A Product belongs to a Seller.

The Product Aggregate references the Seller using SellerId.

SellerId must represent a Seller domain identity rather than a Security concept.

Product registration must derive SellerId from the authenticated Seller rather than trust
a SellerId submitted by a client.

---

## Product Query

Product information may be publicly queried.

A public Product query does not require Product ownership authorization.

Seller ownership authorization should be introduced when a use case such as Product
modification requires it.

---

# Customer

Customer represents the customer business concept.

CustomerId belongs to the Customer domain even when Subscription or Security uses it.

A complete Customer Aggregate does not need to exist merely because CustomerId exists.

---

# Seller

Seller represents the seller business concept.

SellerId belongs to the Seller domain.

A complete Seller Aggregate should be introduced only when Seller-specific business
behavior requires one.

---

# Value Objects

Use Value Objects when a value has domain meaning, validation, or type-safety value.

Current examples include:

- SubscriptionId
- ProductId
- CustomerId
- SellerId
- Cycle

Possible future example:

- Money

Value Objects should normally be immutable.

Java records are appropriate when they clearly express the domain concept.

Do not create a Value Object solely to wrap every primitive.

Introduce one when it provides meaningful domain semantics or protects an invariant.

---

# Identity Ownership

Place identifiers in the domain that owns their meaning.

CustomerId
→ customer.domain

SellerId
→ seller.domain

ProductId
→ product.domain

SubscriptionId
→ subscription.domain

The package of the current consumer does not determine ownership.

Ask:

"What concept does this type represent?"

rather than:

"Which class currently uses this type?"

---

# Domain Exceptions

Exceptions representing a Domain invariant or invalid state transition belong close to
the Domain.

Example:

InvalidSubscriptionStateTransitionException

Application-level failures such as resource absence or use-case conflicts may belong to
the Application layer.

Examples:

SubscriptionNotFoundException
DuplicateSubscriptionException
ProductNotFoundException

Do not create generic business exception hierarchies until an actual need appears.

---

# Modeling Principles

Prefer behavior-rich models where meaningful Domain behavior exists.

Avoid anemic modeling caused by moving every business rule into Application Services.

At the same time, do not force simple data concepts to contain artificial behavior merely
to appear object-oriented.

Use Aggregate boundaries to define consistency boundaries.

Use identities across Aggregate boundaries.

Allow Query models to differ from Domain models.

Keep the Domain model independent from persistence representation.