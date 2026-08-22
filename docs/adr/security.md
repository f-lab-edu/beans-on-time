# Security

## Purpose

Security in Beans on Time separates:

1. Authentication
2. Request-level authorization
3. Resource-level authorization
4. Business rules

These concerns must not be mixed unnecessarily.

---

# Authentication

Spring Security is responsible for authentication.

The current development environment uses HTTP Basic authentication.

Authenticated principals currently represent business actors such as:

- Customer
- Seller

Framework-specific authentication models belong to the Security adapter layer.

Examples:

AuthenticatedCustomer
AuthenticatedSeller

Business identifiers such as CustomerId and SellerId remain Domain types.

---

# Current Actor Providers

When authenticated identity is required as application input, the Application layer should
depend on an abstraction rather than directly access SecurityContextHolder.

Examples:

CurrentCustomerProvider
CurrentSellerProvider

Spring Security-specific implementations may read from SecurityContextHolder.

This keeps Application Services independent from Spring Security APIs.

---

# Request-Level Authorization

SecurityConfig provides coarse-grained HTTP request authorization.

Current intended rules include:

GET /products/**
→ public

POST /products
→ SELLER

/subscriptions/**
→ CUSTOMER

Request authorization answers:

"Can this kind of actor access this type of endpoint?"

It does not answer:

"Does this actor own this specific resource?"

---

# Resource-Level Authorization

Resource ownership is evaluated separately from request-level role authorization.

Example:

ROLE_SELLER
→ Seller may enter Product management endpoints

Product.sellerId == authenticated SellerId
→ Seller may modify this Product

Current Subscription ownership authorization uses annotation/AOP-based authorization.

Equivalent Product ownership authorization should be introduced only when an actual
Seller-owned mutation use case requires it.

Do not add Product ownership AOP merely for structural symmetry with Subscription.

---

# Ownership Data

Never trust ownership identity supplied by the client when the authoritative identity is
already available from authentication.

Bad:

POST /products

{
"sellerId": 123,
...
}

when Seller identity should come from authentication.

Preferred flow:

Authentication
→ CurrentSellerProvider
→ SellerId
→ RegisterProductService
→ Product

The same principle applies to Customer-owned operations when appropriate.

---

# Authorization vs Business Rules

Authorization and business validation are different concerns.

Examples:

"Is this Customer the owner of the Subscription?"
→ authorization

"Does this Customer already subscribe to this Product?"
→ business rule

"Can a PAUSED Subscription be resumed?"
→ Domain rule

Do not move business validation into AOP simply because authorization uses AOP.

---

# Security Exceptions

Authentication and authorization failures should use Security-layer semantics.

Examples:

AuthenticationCredentialsNotFoundException
AccessDeniedException

Domain-specific exception handlers should not take ownership of Security failures unless
there is an explicit HTTP error-response requirement.

Application and Domain exceptions must not be used as substitutes for authentication or
authorization failures.

---

# Security Evolution

Do not prematurely introduce a generic Actor model solely because Customer and Seller
implementations appear structurally similar.

First observe actual common semantics.

Generalize only when Customer and Seller authentication models have proven shared
requirements.

Structural duplication alone is not sufficient justification for a shared abstraction.