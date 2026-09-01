# 상품 도메인 규칙

## 문서 목적

이 문서는 `Product` 애그리거트의 공급 상태와 상태 전이, 구독과의 연동 규칙을
기록한다. 공통 용어는 `docs/glossary.md`, 애그리거트 경계는
`docs/domain-model.md`, 구독의 구체적인 불변식은 `docs/subscription.md`를 따른다.

---

## 확정된 규칙

### 상품 상태

`ProductStatus`는 현재 판매 가능 여부와 공급 가능 여부를 하나의 상태 축으로
표현한다.

- `AVAILABLE`: 정상 공급할 수 있고 신규 및 기존 구독을 모두 수행할 수 있다.
- `TEMPORARILY_UNAVAILABLE`: 향후 재개할 수 있지만 현재 공급할 수 없어
  신규 구독과 기존 구독 실행을 모두 차단한다.
- `DISCONTINUED`: 상품 공급이 영구 종료되어 신규 구독과 기존 구독 실행을
  모두 차단한다. 복구할 수 없는 종료 상태다.

`AVAILABLE`인 상품만 신규 구독할 수 있다. 이 질의는
`Product.isSubscribable()`로 표현한다.

### 상품 상태 전이

| 현재 상태 | `stopSupply()` | `resumeSupply()` | `discontinue()` |
|---|---|---|---|
| `AVAILABLE` | `TEMPORARILY_UNAVAILABLE` | `AVAILABLE` 유지 | `DISCONTINUED` |
| `TEMPORARILY_UNAVAILABLE` | 상태 유지 | `AVAILABLE` | `DISCONTINUED` |
| `DISCONTINUED` | 상태 전이 예외 | 상태 전이 예외 | 상태 유지 |

동일한 최종 상태를 만드는 반복 요청은 멱등으로 처리한다. 단,
`DISCONTINUED`에서 일시 중지나 공급 재개를 요청하면 종료 상태의 불변식과
충돌하므로 도메인 예외를 발생시킨다.

### 구독과의 연동

Product는 Subscription 애그리거트를 직접 참조하거나 변경하지 않는다. 동기
애플리케이션 서비스가 두 애그리거트의 변경을 조율한다.

- `AVAILABLE -> TEMPORARILY_UNAVAILABLE`: 해당 상품을 참조하는 종료되지 않은
  구독에 `PRODUCT_UNAVAILABLE`을 추가한다.
- `TEMPORARILY_UNAVAILABLE -> AVAILABLE`: 동일한 구독에서
  `PRODUCT_UNAVAILABLE`만 제거한다.
- `AVAILABLE` 또는 `TEMPORARILY_UNAVAILABLE -> DISCONTINUED`: 동일한
  구독에 `PRODUCT_UNAVAILABLE`을 추가하거나 유지한다.

연동 대상은 `ACTIVE`, `PAUSED` 구독이다. `CANCELLED` 구독은 제외한다.
Product 상태 변경은 Subscription의 `lifecycleStatus`를 변경하지 않는다.
공급 재개는 `PRODUCT_UNAVAILABLE`만 제거하며 `PAYMENT_FAILED` 등 다른 실행 차단
사유를 변경하지 않는다.

### HTTP API와 소유권 인가

```text
PATCH /products/{id}/supply/stop
PATCH /products/{id}/supply/resume
PATCH /products/{id}/discontinue
```

상태값을 임의로 받는 범용 수정 API를 두지 않고 도메인 행위를 노출한다. 상품
상태 변경은 인증된 판매자 중 해당 Product의 `sellerId`와 일치하는 소유자만
수행할 수 있다. `@RequireOwnership`이 붙은 로더가 반환한 Product를
`ProductOwnershipResolver`가 판매자 주체와 비교한다.

경로의 Product ID가 0 이하면 Web Adapter가 외부 입력 오류로 검증해
`400 BAD_REQUEST`로 응답한다. 양수 ID이지만 Product가 없으면 `404 NOT_FOUND`를
유지한다. 영구 종료된 Product에 공급 중지나 재개를 요청하는 경우는 현재 리소스
상태와의 충돌이므로 `409 CONFLICT`로 응답한다. 다른 판매자의 Product이면
기존 소유권 인가 정책을 따른다. 동일 상태를 만드는 멱등 요청은 정상 처리한다.

---

## 결정 이유

- Subscription의 `PAUSED`는 고객이 선택한 구독 생명주기 상태로 유지한다.
- Product 공급 문제는 구독 생명주기가 아니라 실행 가능 여부의 문제다.
- 기존 `SubscriptionSuspensionReason`을 활용해 Subscription 모델 변경을 최소화한다.
- 실행 차단 사유를 집합으로 유지해 여러 원인이 독립적으로 공존하고 해제될 수
  있게 한다.
- 현재 InMemory 범위에서는 동기 애플리케이션 조율이 가장 작고 명시적인
  구현이다.

---

## 후속 논의 대상

- `DISCONTINUED` Product를 참조하는 기존 Subscription의 최종 처리
- 환불, 잔여 선결제 이용 기간 소멸, 대체 상품 전환
- `PRODUCT_UNAVAILABLE` 기간의 선결제 이용 기간 보상
- 대량 Subscription 갱신 방식과 Event, Async, Batch 도입 여부
- 동시성과 최종적 일관성 정책
