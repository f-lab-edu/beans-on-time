# 청구·결제 도메인 규칙

## 문서 목적

이 문서는 수동 구독 재활성화를 위해 도입한 청구와 결제의 현재 확정 규칙, 결정 이유와
후속 논의 대상을 기록한다. 구독의 생명주기와 선결제 이용 기간 규칙은
`docs/subscription.md`를 함께 따른다.

---

## 확정된 규칙

### 청구와 결제의 책임

`Billing`은 특정 고객의 특정 구독에 대해 특정 시점에 금액을 확정한 청구 사실을
표현하는 애그리거트다. `Payment`는 특정 청구에 대해 실제 결제를 시도한 결과를 표현하는
별도 애그리거트다. 청구 안에 결제 컬렉션을 두지 않으며 두 애그리거트는 식별자로
연결한다.

`Billing`은 다음 정보를 가진다.

- `BillingId id`
- `CustomerId customerId`
- `SubscriptionId subscriptionId`
- `ProductId productId`
- `Money amount`
- `LocalDate billingDate`
- `BillingStatus status`
- `LocalDateTime createdAt`

`BillingStatus`는 `PENDING`, `PAID`만 사용한다. 정상 결제 거절은 청구 자체를 무효로
만들지 않으므로 `FAILED` 청구 상태를 두지 않는다. `markPaid()`가 `PENDING -> PAID`
전이를 소유하며, 결제 유즈케이스는 `PAID` 청구에 대한 재요청을 Gateway 호출 전에
거절한다.

`Payment`는 다음 정보를 가진다.

- `PaymentId id`
- `BillingId billingId`
- `Money amount`
- `PaymentStatus status`
- `String transactionId`
- `LocalDateTime attemptedAt`

`PaymentStatus`는 `SUCCESS`, `FAILED`만 사용한다. 성공한 결제에는 비어 있지 않은
`transactionId`가 있고 거절된 결제에는 없다. 생성된 Payment의 상태와 금액은 변경하지
않는다. 이번 범위에서는 Billing 하나에 Payment 시도를 한 번만 허용한다. FAILED Payment가
이미 존재하는 PENDING Billing의 재요청은 `409 CONFLICT`로 거절하며 재시도 정책은 후속
논의 대상으로 남긴다.

### 가격 확정

재활성화 Billing을 처음 준비할 때 Product의 현재 `basePrice`를 `Billing.amount`로
스냅샷한다. Billing 생성 뒤 Product 가격이 바뀌어도 Billing 금액은 바뀌지 않는다.

Checkout 표시 금액과 PaymentGateway 요청 금액은 항상 `Billing.amount`를 사용한다.
Payment 시점에 Product 가격을 다시 조회하지 않으며 클라이언트는 결제 금액을 전달하지
않는다.

### 재활성화 Billing 준비

Billing 준비 대상은 다음 조건을 모두 만족하는 Subscription이다.

```text
lifecycleStatus == PAUSED
remainingPaidDays == 0
```

Subscription은 존재해야 하고 인증된 고객의 소유여야 한다. 새 Billing을 만드는 경우
참조한 Product도 존재해야 한다. 현재 Product의 판매 상태나 공급 가능 여부는 검사하지
않는다.

한 Subscription에는 동시에 하나의 PENDING 재활성화 Billing만 둔다. 같은 Subscription에
준비 요청이 반복되면 기존 PENDING Billing을 반환하며 Product 가격을 다시 읽지 않는다.
따라서 최초 준비 때 확정한 가격을 Checkout까지 유지한다.

`billingDate`와 `createdAt`은 애플리케이션 서비스가 주입된 KST `Clock`으로 구한다.
도메인은 현재 시간을 직접 조회하지 않는다.

### Checkout 조회

Checkout은 기존 Subscription 상세 조회를 확장하지 않고 Billing 영역의 별도 조회
모델인 `BillingCheckoutDetail`로 제공한다. 조회 어댑터가 Billing, Subscription,
Product 정보를 조합하지만 애그리거트 경계는 바뀌지 않는다.

Checkout API는 Billing의 `customerId`를 기준으로 매 요청마다 소유권을 독립적으로
검사한다. 이전 Subscription API의 인가 결과를 신뢰하지 않는다.

### 결제 성공

PENDING Billing의 소유권과 재활성화 대상 Subscription 상태를 확인한 뒤
PaymentGateway에 Billing 식별자와 `Billing.amount`를 전달한다. 승인 결과를 받으면
SUCCESS Payment를 저장하고 다음 상태 변경을 수행한다.

```text
Billing.status = PAID
Subscription.lifecycleStatus = ACTIVE
Subscription.currentPeriod = 새 선결제 이용 구간
Subscription.remainingPaidDays = null
Subscription.pausedAt = null
Subscription.scheduledResumeDate = null
Subscription.suspensionReasons에서 PAYMENT_FAILED만 제거
```

`PRODUCT_UNAVAILABLE` 같은 다른 실행 차단 사유는 유지한다.

실제 Payment 성공 KST 업무 날짜를 `paymentDate`라고 하면 새 일정은 다음과 같다.

```text
billingAnchorDay = paymentDate.dayOfMonth
nextBillingDate = 새 billingAnchorDay 기준으로 paymentDate 뒤에 처음 도래하는 결제일
currentPeriod = paymentDate ~ nextBillingDate.minusDays(1)
```

기존 `BillingAnchorDay`의 월말 보정 규칙을 재사용한다. 예를 들어 2026-01-31 성공이면
anchor는 31, 다음 결제일은 2026-02-28, 새 이용 구간은 2026-01-31부터
2026-02-27까지다. 2028년에는 다음 결제일이 2028-02-29다. 월말 보정된 실제 날짜로
anchor를 바꾸지 않는다.

### 결제 거절과 Gateway 장애

카드 승인 거절이나 한도 부족처럼 정상 응답으로 확인한 결제 거절은 Payment의
`FAILED` 결과다.

```text
Payment.status = FAILED
Billing.status = PENDING 유지
Subscription.lifecycleStatus = PAUSED 유지
Subscription.currentPeriod = null 유지
Subscription.remainingPaidDays = 0 유지
Subscription.suspensionReasons에 PAYMENT_FAILED 추가
기존 nextBillingDate, billingAnchorDay, pausedAt, scheduledResumeDate 유지
```

연결 실패나 Timeout처럼 정상 응답을 받지 못한 PaymentGateway 장애는 결제 거절과
구분한다. 이 경우 FAILED Payment를 만들거나 `PAYMENT_FAILED`를 추가하지 않고 외부 시스템
장애로 전파해 HTTP `503 SERVICE_UNAVAILABLE`로 응답한다.

### 포트와 InMemory 어댑터

Billing 명령에는 저장, ID 조회, Subscription ID 기준 PENDING 조회 포트를 사용한다.
Payment는 결과 저장과 Billing ID 기준 시도 존재 확인 포트를 둔다. Payment 직접 조회
API나 이력 조회 포트는 만들지 않는다. Checkout은 CQS에 따라 전용 조회 포트를 사용한다.

`PaymentGateway`는 Billing ID와 금액을 받아 승인 또는 거절 결과를 반환하는 아웃바운드
포트다. 현재 `FakePaymentGatewayAdapter`가 승인, 거절, 장애 결과를 제어할 수 있게
구현한다. 실제 PG SDK 구조나 Payment Method를 흉내 내지 않는다.

### 인증·인가와 HTTP API

Billing과 Payment HTTP API는 CUSTOMER 역할을 요구한다. 소유권은 다음 경계에서 각각
독립적으로 검사한다.

- Billing 준비: 반환된 Subscription 기준
- Checkout: 반환된 `BillingCheckoutDetail`의 Billing 고객 기준
- Payment: 반환된 Billing 기준

현재 API는 다음과 같다.

```text
POST /subscriptions/{subscriptionId}/reactivation-billing
GET  /billings/{billingId}/checkout
POST /billings/{billingId}/payments
```

Payment API는 Request Body로 금액을 받지 않는다. 0 이하 Billing ID는 `400 BAD_REQUEST`,
형식이 유효하지만 존재하지 않는 Billing이나 Subscription은 `404 NOT_FOUND`, 재활성화
대상이 아닌 Subscription, 이미 PAID인 Billing과 이미 Payment를 시도한 PENDING Billing은
`409 CONFLICT`다. 타인 소유 리소스는 기존 보안 정책에 따라 거부한다. 모든
`IllegalArgumentException`을 전역 `400`으로 변환하지 않는다.

---

## 결정 이유

### Billing과 Payment를 분리한 이유

청구할 거래 가격이 확정됐다는 사실과 실제 돈을 받으려 시도한 결과는 생명주기와 변경
이유가 다르다. 결제 거절 뒤에도 청구는 유효하므로 하나의 상태 모델로 합치지 않는다.

### 기존 PENDING Billing을 반환하는 이유

Checkout 진입 뒤 준비 요청이 반복되더라도 최초 확정 가격을 보존해야 한다. 별도 만료
정책이 없는 현재 범위에서는 PENDING Billing을 재사용하는 것이 가장 작은 멱등 동작이다.

### 결제 성공일에 새 구독 회차를 여는 이유

새 이용권은 실제 결제가 승인된 뒤에만 제공한다. 요청일이나 Billing 생성일이 아니라
성공 업무 날짜를 시작일로 사용해야 결제 전 기간을 유료 이용 기간으로 잘못 제공하지
않는다.

---

## 후속 논의 대상

다음 항목은 현재 구현 요구사항이 아니며 선제 구현하지 않는다.

- 최초 구독 결제와 결제 전 구매 의도 모델
- Order, Cart와 다상품 일괄 결제
- 자동 정기결제, Scheduler와 Batch
- Payment 재시도와 Billing 1 : N Payment 이력 조회
- PENDING Billing 만료·취소
- Payment idempotency와 중복 결제 방지
- PENDING Billing 준비 및 Payment 동시 요청 경쟁 조건
- 실제 PG와 Payment Method, 카드 저장
- PG 거래 성공 후 저장 실패 복구
- JDBC, Transaction, Outbox, Event와 최종적 일관성
- 환불과 결제 취소
- Product 공급 가능 여부와 재활성화 Billing 연동
- Product 공급 불가 상태에서의 결제 정책
