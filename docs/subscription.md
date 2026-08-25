# 구독 도메인 규칙

## 문서 목적

이 문서는 구독과 이에 연결되는 상품, 청구, 배송 작업에서 따라야 할
현재 도메인 규칙의 기준 문서다.

문서는 다음을 명확히 구분한다.

- **확정된 규칙**: 현재 코드와 테스트가 따라야 하는 규칙
- **결정 이유**: 현재 구조를 선택한 핵심 이유
- **후속 논의 대상**: 필요성은 예상되지만 세부 정책이나 구현이 확정되지 않은 항목

관련 코드를 변경하기 전에 이 문서를 읽는다. 확정된 규칙과 충돌하는 요구사항이 있으면
임의로 우회하거나 기존 의미를 바꾸지 말고 충돌 내용을 작업 결과에 보고한다.

---

## 공통 도메인 용어

설계 문서와 작업 보고에서는 다음 한국어 용어를 기본으로 사용한다. Java 코드 식별자는
기존 이름을 유지한다.

- 애그리거트
- 애그리거트 루트
- 불변식
- 생명주기 상태
- 실행 차단 사유
- 도메인 행위
- 애플리케이션 서비스
- 조회 모델
- 소유권 인가

같은 개념을 설명할 때 영어와 한국어 표현을 불필요하게 혼용하지 않는다.

---

# 확정된 규칙

## 1. 구독 애그리거트의 책임과 상태

`Subscription`은 고객의 반복 커피 구독을 표현하는 애그리거트 루트다. 다른 애그리거트를
객체 그래프로 포함하지 않고 `CustomerId`, `ProductId`로 참조한다.

현재 주요 상태는 다음과 같다.

- `SubscriptionId id`
- `CustomerId customerId`
- `ProductId productId`
- `Cycle cycle`
- `LocalDate startedDate`
- `SubscriptionPeriod currentPeriod`
- `BillingAnchorDay billingAnchorDay`
- `LocalDate nextBillingDate`
- `LocalDateTime pausedAt`
- `LocalDate resumeDate`
- `SubscriptionStatus lifecycleStatus`
- `Set<SubscriptionSuspensionReason> suspensionReasons`

상태 변경은 `pause`, `resume`, `cancel`, 실행 차단 사유 추가·제거와 같은 의미 있는
도메인 행위를 통해서만 수행한다. 일반 setter를 추가하거나 변경 가능한 컬렉션을 외부에
직접 노출하지 않는다.

### 1.1 생명주기 상태

생명주기 상태는 다음 세 값만 사용한다.

- `ACTIVE`
- `PAUSED`
- `CANCELLED`

`SUSPENDED`를 생명주기 상태로 저장하지 않는다. `CANCELLED`는 최종 생명주기
상태이며, `pause`와 `resume`은 허용하지 않는다.

### 1.2 실행 차단 사유

시스템 또는 외부 조건이 구독 실행을 막는 원인은 생명주기 상태와 별도로
관리한다.

현재 `SubscriptionSuspensionReason`은 다음 두 값만 가진다.

- `PRODUCT_UNAVAILABLE`
- `PAYMENT_FAILED`

타입은 `Set<SubscriptionSuspensionReason>`이며 여러 사유가 동시에 존재할 수 있다.
현재 애그리거트 내부 구현은 `EnumSet`을 사용하고 외부에는 수정할 수 없는 복사본을
반환한다. 추가와 제거는 멱등 도메인 행위다.

생명주기 상태와 실행 차단 사유는 독립적인 상태 축이다. 다음은 모두 유효하다.

```text
ACTIVE + [PRODUCT_UNAVAILABLE, PAYMENT_FAILED]
PAUSED + [PRODUCT_UNAVAILABLE]
```

일시정지와 재개는 실행 차단 사유를 제거하지 않는다. 취소 후 실행 차단 사유를
보존할지 정리할지는 아직 별도 정책으로 확정하지 않았다.

## 2. 구독 공통 실행 차단 판단

`isExecutionBlocked()`의 책임은 다음 하나뿐이다.

> 구독 자체의 공통 상태 때문에 후속 실행이 차단되어 있는가?

현재 규칙은 다음과 같다.

```text
lifecycleStatus != ACTIVE
또는
suspensionReasons가 비어 있지 않음
```

다음 경우에만 `false`다.

```text
lifecycleStatus == ACTIVE
그리고
suspensionReasons.isEmpty()
```

| 생명주기 상태 | 실행 차단 사유 | isExecutionBlocked() |
| --- | --- | --- |
| ACTIVE | 없음 | false |
| ACTIVE | PRODUCT_UNAVAILABLE | true |
| ACTIVE | PAYMENT_FAILED | true |
| PAUSED | 없음 | true |
| PAUSED | PRODUCT_UNAVAILABLE | true |
| CANCELLED | 무관 | true |

`isExecutionBlocked() == false`는 결제 가능, 배송 가능, 상품 공급 가능, 또는 오늘이
실행 예정일이라는 뜻이 아니다. 향후 청구와 배송은 구독 공통 차단이
없는지 확인한 뒤 각 기능의 일정과 정책을 추가로 판단해야 한다.

## 3. PAUSED 문맥 불변식

`pausedAt`과 `resumeDate`는 과거 일시정지 이력이 아니라 현재 `PAUSED` 상태의 문맥을
표현한다.

구독 애그리거트는 생성 경로와 관계없이 다음 불변식을 만족해야 한다.

```text
PAUSED    -> pausedAt != null && resumeDate != null
ACTIVE    -> pausedAt == null && resumeDate == null
CANCELLED -> pausedAt == null && resumeDate == null
```

현재 InMemory 구조에서는 신규 생성과 `pause`, `resume`, `cancel` 도메인 행위가 이
불변식을 보장한다. JDBC 영속성과 복원 전용 생성 경로는 아직 존재하지 않는다.

향후 `Subscription.restore(...)` 같은 복원 경로가 추가되면 저장된 모든 값으로
애그리거트를 만들 때도 같은 불변식을 검증해야 한다. 영속성 어댑터가 불완전한
PAUSED 문맥을 그대로 도메인에 복원해서는 안 된다. 이 규칙을 이유로 현재 사용되지 않는
복원 전용 생성 경로를 미리 만들지는 않는다.

과거 일시정지 이력이 필요해지면 별도 이력, 이벤트, 감사 모델을 검토한다. 현재
애그리거트의 `pausedAt`과 `resumeDate`에 과거 값을 남기지 않는다.

## 4. 시간 타입과 KST 계약

날짜와 시각 이름은 다음 규칙을 따른다.

- `...Date`: `LocalDate`, 업무상 날짜가 중요함
- `...At`: `LocalDateTime`, 실제 발생 시각이 중요함

모든 구독 업무 날짜와 시각은 KST(`Asia/Seoul`) 기준으로 취급한다.

```text
인프라스트럭처 / 설정
-> Asia/Seoul 기준 Clock 제공

애플리케이션 서비스
-> LocalDate.now(clock)
-> LocalDateTime.now(clock)

도메인
-> 전달받은 값을 KST 업무 시간이라는 계약 아래 사용
```

`LocalDateTime`이 시간대 정보를 보유하지 않는다는 점은 현재 한국 단일 시간대 범위에서
의도적으로 수용한다. 도메인에 `Clock`을 주입하거나 도메인에서 `LocalDate.now()`,
`LocalDateTime.now()`를 직접 호출하지 않는다. `DateTimeUtils.now()` 같은 전역 정적
현재 시각 유틸도 만들지 않는다.

다중 시간대 지원이나 DB 저장 정책을 설계하게 되면 `Instant`, `OffsetDateTime`,
`ZonedDateTime` 사용 여부를 별도로 검토한다. 현재 모델 전체를 이 타입으로 변경하지
않는다.

## 5. SubscriptionPeriod와 Cycle

세 개념은 서로 다른 시간 의미를 가진다.

### 5.1 SubscriptionPeriod

- `SubscriptionPeriod`: 현재 결제를 통해 이미 확정된 구독 회차의 유효 기간.
  `startDate`와 `endDate` 양 끝 날짜를 모두 포함한다.
- 청구 일정: `billingAnchorDay`는 최초 결제 기준 월의 날짜를 보존하고,
  `nextBillingDate`는 다음 결제 예정 업무 날짜를 표현한다.

일시정지가 발생해도 이미 확정된 `currentPeriod`를 변경하지 않는다.

### 5.2 Cycle의 현재 의미

- `Cycle`: 결제 회차가 아니라 상품 납품 반복 주기다.

예를 들어 `Cycle = 2주`이고 `currentPeriod = 09/01 ~ 09/30`이면 배송 후보가
`09/01`, `09/15`, `09/29`일 수 있다. 현재는 실제 배송 일정 계산을 구현하지 않는다.
구독 회차, 청구 일정, 납품 주기를 하나의 시간축으로 합치거나 동일한 정책으로 처리하지 않는다.

## 6. BillingAnchorDay와 회차 확정

### 6.1 최초 회차와 BillingAnchorDay

신규 구독의 시작일을 `D`라고 하면 다음처럼 초기화한다.

```text
startedDate = D
billingAnchorDay = D.dayOfMonth
nextBillingDate = 다음 대상 월에 billingAnchorDay를 적용한 날짜
currentPeriod = D ~ nextBillingDate.minusDays(1)
lifecycleStatus = ACTIVE
suspensionReasons = empty
pausedAt = null
resumeDate = null
```

대상 월에 `billingAnchorDay`가 없으면 그 달의 마지막 날을 사용한다. 직전 결제일에
`plusMonths(1)`을 반복해서 원래 기준일을 잃지 않는다.

```text
billingAnchorDay = 31
2026-01-31 -> 2026-02-28 -> 2026-03-31 -> 2026-04-30

billingAnchorDay = 31
2028-02 -> 2028-02-29
```

### 6.2 currentPeriod와 nextBillingDate

`currentPeriod.endDate = nextBillingDate.minusDays(1)`은 구독 전체 생명주기에서
항상 성립하는 불변식이 아니다. 최초 생성이나 기존 회차 종료 후 재개처럼 새로운
정상 회차를 확정할 때 적용하는 계산 규칙이다.

## 7. 일시정지 도메인 행위

허용되는 생명주기 전이는 `ACTIVE -> PAUSED`다. `PAUSED` 또는 `CANCELLED`에서 다시
일시정지하면 `InvalidSubscriptionStateChangeException`을 발생시킨다. 실행 차단 사유가
존재하는 ACTIVE 구독도 고객 의사로 일시정지할 수 있다.

일시정지 요청이 성공하면 즉시 다음 상태가 된다.

```text
lifecycleStatus = PAUSED
pausedAt = 요청 발생 시각
resumeDate = pauseUntilDate.plusDays(1)
currentPeriod = 변경 없음
nextBillingDate = 변경 없음
suspensionReasons = 변경 없음
```

즉 현재 회차 종료까지 기다렸다가 PAUSED가 되는 이전 정책은 폐기되었다. 이미 확정된
`currentPeriod`를 일시정지 종료 날짜에 맞춰 이동하거나 새로 만들지 않는다.

### 7.1 일시정지 날짜 불변식

도메인 행위의 입력은 `pauseUntilDate`와 `pausedAt`이다. 별도 `pausedDate`를 받지 않고
업무 날짜는 `pausedAt.toLocalDate()`에서 도출한다.

```text
pauseUntilDate >= pausedAt.toLocalDate()
```

동일 날짜는 허용한다. 과거 날짜이면 `InvalidSubscriptionPausePeriodException`을
발생시킨다. `pauseUntilDate >= nextBillingDate` 제한은 사용하지 않는다.

```text
pausedAt = 2026-09-10T14:30
pauseUntilDate = 2026-09-10
resumeDate = 2026-09-11
```

### 7.2 일시정지와 nextBillingDate

일시정지할 때 `nextBillingDate = resumeDate`로 강제 변경하지 않고 기존 값을 유지한다.

```text
currentPeriod = 09/01 ~ 09/30
nextBillingDate = 10/01
일시정지 = 09/10 ~ 09/20
resumeDate = 09/21

일시정지 이후에도 nextBillingDate = 10/01
```

일시정지가 `nextBillingDate`를 넘어갈 때 결제 예정일을 어떻게 조정할지는 확정되지 않은
후속 청구 정책이다.

## 8. 수동 재개 도메인 행위

허용되는 생명주기 전이는 `PAUSED -> ACTIVE`다. `ACTIVE` 또는 `CANCELLED`에서
재개하면 `InvalidSubscriptionStateChangeException`을 발생시킨다.

### 8.1 resumeDate와 수동 재개의 관계

`resumeDate`는 자동 재개 예정일이다. 수동 재개의 최소 날짜나 최대 날짜가 아니다.
고객은 일시정지 요청 이후라면 예정일 전에도, 예정일 당일에도, 예정일이 지난 후에도 아직
PAUSED 상태인 구독을 수동 재개할 수 있다.

```text
pausedAt = 2026-09-10T14:30
resumeDate = 2026-09-30

허용: 2026-09-10, 2026-09-20, 2026-09-30, 2026-10-02
거부: 2026-09-09
```

수동 재개 날짜의 유일한 현재 하한 불변식은 다음과 같다.

```text
resumedDate >= pausedAt.toLocalDate()
```

이를 위반하면 `InvalidSubscriptionResumeDateException`을 발생시킨다. 다음 제한은
추가하지 않는다.

```text
resumedDate >= resumeDate
resumedDate <= resumeDate
```

### 8.2 기존 currentPeriod 안에서 재개

다음 조건이면 기존 회차와 청구 일정을 유지한다.

```text
pausedAt.toLocalDate() <= resumedDate <= currentPeriod.endDate
```

결과는 다음과 같다.

```text
lifecycleStatus = ACTIVE
currentPeriod = 변경 없음
nextBillingDate = 변경 없음
pausedAt = null
resumeDate = null
suspensionReasons = 변경 없음
```

기존 회차와 겹치는 새 회차를 만들지 않는다.

### 8.3 기존 currentPeriod 종료 후 재개

`resumedDate > currentPeriod.endDate`이면 `resumedDate`부터 새 정상 회차를 확정한다.

```text
currentPeriod.startDate = resumedDate
nextBillingDate = BillingAnchorDay로 다음 대상 월 날짜 계산
currentPeriod.endDate = nextBillingDate.minusDays(1)
pausedAt = null
resumeDate = null
suspensionReasons = 변경 없음
```

예시는 다음과 같다.

```text
기존 currentPeriod = 09/01 ~ 09/30
billingAnchorDay = 31
resumedDate = 10/10

새 nextBillingDate = 11/30
새 currentPeriod = 10/10 ~ 11/29
```

재개는 고객 일시정지 생명주기만 종료한다. `PRODUCT_UNAVAILABLE`, `PAYMENT_FAILED` 같은
실행 차단 사유를 제거하지 않는다.

## 9. 취소 도메인 행위

`cancel()`은 최종 상태를 `CANCELLED`로 만드는 멱등 도메인 행위다.

```text
ACTIVE -> CANCELLED
PAUSED -> CANCELLED
CANCELLED -> CANCELLED
```

이미 CANCELLED여도 예외를 발생시키지 않는다. 취소 시 현재 PAUSED 문맥을 끝내므로
`pausedAt`과 `resumeDate`를 null로 초기화한다.

## 10. 예외의 의미와 위치

구독 불변식 위반 예외는 도메인 패키지에 둔다.

- `InvalidSubscriptionStateChangeException`: 허용되지 않는 생명주기 전이
- `InvalidSubscriptionPausePeriodException`: 유효하지 않은 일시정지 종료일
- `InvalidSubscriptionResumeDateException`: 일시정지 요청일보다 이른 재개 날짜

리소스 부재나 유즈케이스 충돌 같은 애플리케이션 수준 실패와 구분한다. 현재 요구 없이
공통 비즈니스 예외 계층을 만들지 않는다.

## 11. 조회 모델 조합과 상품 조회 정보

구독 상세 조회는 다음 전용 조회 모델을 사용한다.

```text
subscription.application.port.in.SubscriptionDetail
|- subscription.application.port.in.SubscriptionInfo
`- product.application.port.in.ProductInfo
```

`SubscriptionInfo`는 구독 상세에 필요한 구독 정보를 표현하고 구독 애플리케이션
계층이 소유한다. `ProductInfo`와 `ProductAvailability`는 상품 조회 정보와 조회 가능
여부를 표현하므로 상품 애플리케이션 계층이 소유한다. `SubscriptionDetail`은 구독 상세
유즈케이스를 위해 두 조회 정보를 조합한다.

이 조합은 애그리거트 경계를 변경하지 않는다. `Subscription` 애그리거트는 계속
`ProductId`로만 상품을 참조하며 `Product`, `ProductInfo`, `ProductAvailability`를 내부
상태로 포함하지 않는다. 조회 모델을 사용하는 위치가 아니라 각 타입이 표현하는 개념을
기준으로 소유 패키지를 정한다.

`SubscriptionInfo`와 `ProductInfo`는 각 애그리거트의 복제 모델이 아니며 구독 상세
조회에 필요한 데이터만 표현한다. `SubscriptionInfo.customerId`를 이용하는 기존 소유권
인가 흐름을 유지한다.

`ProductInfo`가 사용하는 `ProductAvailability` 값은 `AVAILABLE`, `UNAVAILABLE`이다.
이는 상품의 판매 상태나 납품 가능 상태가 아니라, 구독이 참조하는 상품 데이터를 상세 조회에서
정상적으로 확보했는지를 뜻한다. 조회할 수 없으면 전체 상세 조회를 실패시키지 않고
다음 대체 정보를 사용한다.

```text
availability = UNAVAILABLE
productId = 구독이 보유한 기존 productId
name = null
basePrice = null
```

명령 어댑터와 조회 어댑터의 책임을 합치지 않는다. InMemory 저장소는 물리 데이터
조회 의미의 `findById`를 제공하고, CQS 의미는 애플리케이션 포트와 어댑터에서
표현한다.

---

# 결정 이유

이 장은 현재 구조를 선택한 이유를 기록한다. 현재 코드가 따라야 하는 규칙 자체는
“확정된 규칙”에서 관리한다.

## 1. SUSPENDED를 생명주기 상태로 두지 않는 이유

고객의 일시정지와 `PRODUCT_UNAVAILABLE` 같은 시스템 실행 차단 조건은 동시에 존재할
수 있다. 이를 하나의 상태값으로 표현하면 `PAUSED -> SUSPENDED` 전환 과정에서 고객이
일시정지를 요청했다는 생명주기 문맥을 잃을 수 있다. 따라서 `SUSPENDED`를 저장하지
않고 생명주기 상태와 실행 차단 사유를 조합해 현재 실행 차단 여부를 판단한다.

## 2. 실행 차단 사유를 별도 상태 축으로 둔 이유

상품 공급 문제와 결제 실패는 동시에 발생할 수 있고 각각 독립적으로 추가·해제되어야
한다. 하나의 상태값으로 압축하면 원인 하나를 해제할 때 다른 원인이나 고객의 생명주기
상태까지 잘못 변경할 수 있다. 따라서 생명주기 상태와
`Set<SubscriptionSuspensionReason>`을 독립적으로 관리한다.

## 3. 일시정지 시 currentPeriod를 유지하는 이유

`currentPeriod`는 이미 결제를 통해 확정된 구독 회차다. 일시정지는 앞으로의 구독 실행을
즉시 차단하는 행위이지, 과거에 확정된 회차를 다시 작성하는 행위가 아니다. 따라서
일시정지는 `currentPeriod`를 이동하거나 미래 회차로 교체하지 않는다.

## 4. 재개 시 회차를 조건부로 다시 만드는 이유

현재 회차 안에서 재개할 때 새 회차를 만들면 기존 회차와 기간이 겹친다. 기존 회차가
아직 유효하면 그대로 사용하고, 종료된 후 실제 공백에서 재개할 때만 `resumedDate`부터
새 회차를 확정한다.

## 5. billingAnchorDay를 별도로 보존하는 이유

월말 보정된 날짜를 다음 계산의 기준으로 사용하면 최초 결제 기준일을 잃는다. 예를 들어
`01/31 -> 02/28` 이후 2월 28일에 단순히 한 달을 더하면 3월 28일이 된다. 최초 기준일
31을 보존하고 매번 대상 월에 다시 적용해야 `03/31`로 복원할 수 있다.

## 6. Clock을 애플리케이션에 두는 이유

현재 시간은 외부 환경에 의존하는 값이다. 도메인이 시스템 시간을 직접 조회하지 않고
애플리케이션 서비스가 KST `Clock`으로 날짜와 시각을 만든 뒤 도메인 행위에 전달한다.
이를 통해 도메인은 프레임워크와 시간 인프라스트럭처에 의존하지 않고, 테스트는 특정
시각을 명확하게 제어할 수 있다.

## 7. isExecutionBlocked의 책임을 좁게 둔 이유

청구와 배송은 각각 예정일, 결제 수단, 상품 공급 가능성 같은 추가 조건을 가진다.
구독 애그리거트가 모든 기능의 최종 실행 가능 여부까지 판단하면 아직 확정되지 않은
정책이 도메인에 섞인다. 따라서 이 메서드는 구독 공통 상태에 의한 차단 여부만 판단한다.

## 8. 조회 모델 조합과 타입 소유권을 애그리거트 경계와 분리한 이유

구독 상세는 구독과 상품 데이터를 조합하지만, 조회 편의를 위해 애그리거트 경계를
합치지 않는다. `SubscriptionInfo`와 `ProductInfo`는 상세 조회에 필요한 정보만 가진
조회 모델이며, 명령 측 애그리거트의 구조를 그대로 복제할 의무가 없다.

`ProductInfo`와 `ProductAvailability`는 구독 상세에서 소비되더라도 상품 조회 개념을
표현한다. 따라서 구독 패키지가 아니라 상품 애플리케이션 계층이 소유하고, 구독 상세
조회 모델은 이 타입을 조합해 사용한다. 이를 통해 타입의 소유권을 현재 소비하는
유즈케이스가 아니라 해당 타입이 표현하는 도메인 개념에 맞춘다.

---

# 후속 논의 대상

이 장의 항목은 필요성이나 문제가 발견되었지만 최종 정책 또는 구현 방식을 확정하지
않은 내용이다. 현재 구현 요구사항이 아니며, 명시적인 사용자 요청 없이 구현하거나
미래 확장용 타입·필드·추상화·이벤트·DB 스키마를 선제적으로 추가하지 않는다.

> 후속 논의 대상에 있다 ≠ 현재 구현해야 한다

관련 요구사항이 실제로 등장하면 현재 요청 범위에서 정책을 결정한다. 결정된 내용만
“확정된 규칙”으로 옮기고, 중요한 선택이면 “결정 이유”도 함께 기록한다.

## 1. 청구

- PAUSED 중 `nextBillingDate`가 도래했을 때의 처리
- 일시정지 중 지나간 `nextBillingDate` 재계산 방식
- 청구 최종 실행 조건과 결제 수단 유효성
- 결제 실패 후 재시도와 `PAYMENT_FAILED` 추가·제거 주체
- 결제 성공 후 다음 `SubscriptionPeriod` 확정 방식
- Payment 관련 상태 모델
- 청구 배치와 실제 정기 결제

현재 `isExecutionBlocked()`만으로 청구의 최종 실행 가능 여부를 판단하지 않는다.
사용되지 않는 청구 정책 추상화를 미리 만들지 않는다.

## 2. 배송과 납품 주기

- `Cycle` 기반 배송 일정 생성
- `nextDeliveryDate` 필요 여부
- 일시정지 중 배송 건너뛰기
- 일시정지 중 누락된 배송의 보상 여부
- 재개 후 배송 일정 재조정
- 기존 `Cycle` 기준일 유지 여부
- 재개 날짜를 새 배송 기준일로 사용할지 여부
- 상품 납품 가능성과 배송 전용 최종 실행 조건

현재 `Cycle`은 납품 반복 주기라는 의미만 확정되어 있다. 사용되지 않는
배송 정책이나 배송 애그리거트를 미리 만들지 않는다.

## 3. 상품과 구독 연결

- 기존 상품 판매 상태와 별개인 납품 가능 상태가 필요한지 여부
- 일시적·영구적 납품 불가의 구체적 상태 모델
- 상품 상태에 따른 구독 실행 차단 사유 추가·제거
- 상품 영구 중지 시 구독 취소 여부
- 판매자 또는 운영 권한을 가진 관리자의 상품 상태 변경 유즈케이스
- 상품 상태 변경 결과를 구독에 전파하는 방식

`ProductAvailability` 조회 값이나 `SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE`의
존재를 상품 납품 상태 구현이 완료되었다는 뜻으로 해석하지 않는다.

## 4. 자동화와 대량 처리

- 자동 재개 Scheduler
- 수동 재개와 자동 재개의 애플리케이션 유즈케이스 구성
- 상품 상태 변경 후 구독 대량 처리
- `ApplicationEventPublisher`
- `@Async`
- Virtual Thread
- Batch
- Transactional Outbox
- 메시징 시스템

`resumeDate`는 자동 재개 예정일이라는 의미가 확정되어 있지만, 자동 재개 실행 기능은
아직 없다.

## 5. 관리자와 시스템 실행 주체

현재 관리자는 별도 애그리거트가 아니다. 실제 요구가 생기면 우선 보안의 인증·인가
주체로 표현하는 방향을 검토한다.

시스템은 인증된 사용자 애그리거트나 `ROLE_SYSTEM`이 아니다. 스케줄러 또는 이벤트
리스너 같은 자동 처리 주체를 의미한다. 예를 들어 판매자나 관리자가 상품 상태 변경을
요청하고 시스템이 그 결과에 따른 구독 후속 처리를 수행할 수 있다. 이 흐름과
권한 정책은 아직 구현되지 않았다.

## 6. 영속화와 애그리거트 복원

- JDBC 도입과 DB 스키마
- `Subscription.restore(...)` 또는 재구성 방식
- 복원 과정에서 PAUSED 문맥 불변식을 검증하는 위치와 실패 처리
- DB column 시간대 정책

현재 InMemory 구조에는 복원 전용 생성 경로가 없다. 복원 요구사항이 생기기 전에
사용되지 않는 복원 생성 객체나 영속성 전용 도메인 API를 추가하지 않는다.

## 7. 시간대 전략

- 다중 시간대 지원
- `Instant`, `OffsetDateTime`, `ZonedDateTime` 전환 여부
- DB 및 외부 시스템과 시간을 교환하는 방식

현재 InMemory 구조와 KST `Clock` 계약을 유지한다.

## 8. 이력과 기타 미정 정책

- 일시정지 이력
- 구독 상태 변경 이력
- 감사 로그
- CANCELLED 상태에서 실행 차단 사유를 보존하거나 정리할지 여부

---

# 작업 체크리스트

구독 또는 관련 애그리거트를 수정할 때 다음을 확인한다.

1. 루트 `AGENTS.md`와 관련 아키텍처·도메인·보안 문서를 읽는다.
2. 이 문서의 확정된 규칙을 현재 구현 제약으로 확인한다.
3. 새 요구사항과 확정된 규칙의 충돌 여부를 확인한다.
4. 관련 후속 논의 대상은 참고하되 현재 요청 없이 구현하지 않는다.
5. 현재 요청 범위에 필요한 최소 설계를 수행한다.
6. 새 정책이 확정되면 후속 논의 대상에서 확정된 규칙으로 옮긴다.
7. 향후 잘못 되돌리기 쉬운 선택이면 결정 이유를 기록한다.
8. 상태 변경은 의미 있는 도메인 행위를 통해 수행한다.
9. 변경 가능한 컬렉션을 조회 메서드로 직접 노출하지 않는다.
10. 프로덕션 동작을 바꾸면 가장 작은 범위의 테스트로 의도를 증명하고 관련 전체
    테스트를 실행한다.
