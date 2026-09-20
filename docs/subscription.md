# 구독 도메인 규칙

## 문서 목적

이 문서는 구독과 이에 연결되는 상품, 청구, 배송 작업에서 따라야 할 현재 도메인 규칙의
기준 문서다. 다음 내용을 명확히 구분한다.

- **확정된 규칙**: 현재 코드와 테스트가 따라야 하는 구현 제약
- **결정 이유**: 현재 구조를 선택한 핵심 이유
- **후속 논의 대상**: 필요성은 예상되지만 정책이나 구현이 확정되지 않은 항목

공통 설계·비즈니스 용어는 `docs/glossary.md`를 따른다. 새 요구사항이 확정된 규칙과
충돌하면 기존 규칙을 임의로 변경하지 않고 충돌 내용과 선택지를 먼저 확인한다.

---

## 확정된 규칙

### 구독 애그리거트의 책임

`Subscription`은 고객의 반복 커피 구독을 표현하는 애그리거트 루트다. 다른
애그리거트를 객체 그래프로 포함하지 않고 `CustomerId`, `ProductId`로 참조한다.

현재 주요 상태는 다음과 같다.

- `SubscriptionId id`
- `CustomerId customerId`
- `ProductId productId`
- `DeliveryCycle deliveryCycle`
- `LocalDate startedDate`
- `SubscriptionPeriod currentPeriod`
- `Integer remainingPaidDays`
- `BillingAnchorDay billingAnchorDay`
- `LocalDate nextBillingDate`
- `LocalDateTime pausedAt`
- `LocalDate scheduledResumeDate`
- `SubscriptionStatus lifecycleStatus`
- `Set<SubscriptionSuspensionReason> suspensionReasons`

상태 변경은 `pause`, `resume`, `cancel`, 실행 차단 사유 추가·제거와 같은 의미 있는
도메인 행위를 통해서만 수행한다. 일반 setter를 추가하거나 변경 가능한 컬렉션을 외부에
직접 노출하지 않는다.

### 생명주기 상태와 실행 차단 사유

생명주기 상태는 `ACTIVE`, `PAUSED`, `CANCELLED`만 사용한다. `CANCELLED`는 최종
생명주기 상태이며 `cancel()`은 멱등이다.

시스템 또는 외부 조건이 구독 실행을 막는 원인은 생명주기와 별도 상태 축인
`SubscriptionSuspensionReason` 집합으로 관리한다.

- `PRODUCT_UNAVAILABLE`
- `PAYMENT_FAILED`

여러 사유는 동시에 존재할 수 있고 독립적으로 추가·제거한다. 생명주기 전이는 실행 차단
사유를 임의로 제거하지 않는다.

```text
executionBlocked = lifecycleStatus != ACTIVE || !suspensionReasons.isEmpty()
```

이는 구독 공통 상태에 의한 실행 차단만 의미한다. 청구나 배송의 최종 실행 조건을 모두
판단하는 API로 확대하지 않는다.

### Product 공급 상태와 실행 차단 연동

Product 공급 상태는 Subscription의 `lifecycleStatus`와 독립적으로 관리한다.
Product가 `TEMPORARILY_UNAVAILABLE` 또는 `DISCONTINUED`가 되어도 관련
Subscription을 `PAUSED` 또는 `CANCELLED`로 전이하지 않는다.

공급할 수 없는 Product는 기존 `PRODUCT_UNAVAILABLE` 실행 차단 사유로 표현한다.
상태 변경 시 해당 `ProductId`를 참조하는 종료되지 않은 Subscription에 다음을
반영한다.

- Product 공급 일시 중지 또는 영구 종료: `PRODUCT_UNAVAILABLE` 추가 또는 유지
- Product 공급 재개: `PRODUCT_UNAVAILABLE`만 제거
- Subscription `ACTIVE`, `PAUSED`: 연동 대상
- Subscription `CANCELLED`: 연동 제외

다른 실행 차단 사유는 추가·제거하지 않는다. 공급 상태의 세부 의미와 전이는
`docs/product.md`를 따른다.

### 상태별 필드 불변식

`Subscription`은 생성과 모든 상태 전이가 끝난 뒤 다음 조합을 만족해야 한다.

#### ACTIVE

```text
lifecycleStatus == ACTIVE
currentPeriod != null
remainingPaidDays == null
pausedAt == null
scheduledResumeDate == null
nextBillingDate != null
```

#### PAUSED

```text
lifecycleStatus == PAUSED
currentPeriod == null
remainingPaidDays != null
remainingPaidDays >= 0
pausedAt != null
scheduledResumeDate != null
nextBillingDate != null
```

#### CANCELLED

```text
lifecycleStatus == CANCELLED
currentPeriod == null
remainingPaidDays == null
pausedAt == null
scheduledResumeDate == null
nextBillingDate == null
```

현재 InMemory 구조에는 복원 전용 생성 경로가 없다. 향후 JDBC와
`Subscription.restore(...)` 같은 복원 경로가 생기면 저장된 값으로 애그리거트를 만들
때도 위 불변식을 검증해야 한다. 이 요구를 이유로 사용되지 않는 복원 API를 미리 만들지
않는다.

### 날짜와 KST 계약

구독의 선결제 이용 기간은 `LocalDate` 단위로 계산한다. 부분 일자에 대한 시간 단위
보상이나 일할 계산은 하지 않는다.

- Pause 요청 당일은 사용한 유료 일자다.
- Resume 당일은 사용 가능한 유료 일자다.
- Pause 요청일과 실제 Resume 날짜 사이의 완전한 날짜만 동결한다.

```text
04/10 Pause, 04/10 Resume -> 동결 0일
04/10 Pause, 04/11 Resume -> 동결 0일
04/10 Pause, 04/12 Resume -> 04/11, 동결 1일
04/10 Pause, 04/20 Resume -> 04/11 ~ 04/19, 동결 9일
```

날짜와 시각 이름은 다음 규칙을 따른다.

- `...Date`: `LocalDate`, 업무상 날짜가 중요함
- `...At`: `LocalDateTime`, 실제 발생 시각이 중요함

모든 구독 업무 날짜와 시각은 KST(`Asia/Seoul`) 기준이다. 애플리케이션 서비스가
주입된 `Clock`으로 `LocalDate.now(clock)`, `LocalDateTime.now(clock)`을 구해 도메인에
전달한다. 도메인은 현재 시간을 직접 조회하지 않는다.

### currentPeriod

`currentPeriod`는 ACTIVE 상태에서 현재 실제로 사용할 수 있는 선결제 이용 구간이다.
`startDate`와 `endDate` 양 끝 날짜를 포함한다.

PAUSED에서는 현재 이용 가능한 구간이 없으므로 `currentPeriod`가 없다. 과거 이용
이력이나 남은 일수 계산을 위해 PAUSED 상태에 기존 값을 보존하지 않는다. CANCELLED에서도
존재하지 않는다.

Resume 뒤에는 과거 사용 구간을 포함하지 않고 실제 Resume 날짜부터 현재 이용 가능한
구간만 새로 구성한다. 따라서 같은 날 Resume이면 `04/10 ~ 04/30`, 다음 날 Resume이면
`04/11 ~ 04/30`처럼 표현할 수 있다.

### remainingPaidDays

`remainingPaidDays`는 PAUSED 상태에서 아직 사용하지 않은 선결제 이용 일수다. Pause
요청 당일은 사용한 날이므로 제외한다.

```text
remainingPaidDays = DAYS.between(pauseDate, currentPeriod.endDate)
```

예를 들어 `currentPeriod = 04/01 ~ 04/30`, `pauseDate = 04/10`이면
`04/11 ~ 04/30`의 20일이 남는다. 마지막 유료일에 Pause하면 0이며, 0도 유효한
PAUSED 상태다.

ACTIVE와 CANCELLED에서는 `remainingPaidDays`가 없다. 현재는 별도 값 객체가 필요한
추가 규칙이 없으므로 nullable `Integer`로 상태별 존재 여부를 표현한다.

### DeliveryCycle

`DeliveryCycle`은 상품 납품 반복 주기다. 결제 회차나 청구 일정이 아니다. 이전의 모호한
명칭은 사용하지 않는다.

현재 실제 배송 일정 계산은 구현하지 않는다. 구독 회차, 청구 일정, 납품 주기를 하나의
시간축이나 동일한 정책으로 처리하지 않는다.

### BillingAnchorDay와 최초 구독

신규 구독의 시작일을 `D`라고 하면 다음처럼 초기화한다.

```text
startedDate = D
billingAnchorDay = D.dayOfMonth
nextBillingDate = D 이후 처음 도래하는 billing anchor date
currentPeriod = D ~ nextBillingDate.minusDays(1)
lifecycleStatus = ACTIVE
remainingPaidDays = null
pausedAt = null
scheduledResumeDate = null
```

대상 월에 `billingAnchorDay`가 없으면 그 달의 마지막 날을 사용한다. 월말 보정된 실제
날짜를 다음 달 anchor로 바꾸지 않는다.

```text
billingAnchorDay = 31
2026-01-31 -> 2026-02-28 -> 2026-03-31

billingAnchorDay = 31
2028-01-31 -> 2028-02-29 -> 2028-03-31
```

`BillingAnchorDay.nextBillingDateAfter(date)`는 이름 그대로 `date`보다 뒤에 있는 첫
billing anchor date를 반환한다. 현재 월의 후보가 `date`보다 뒤면 현재 월을 반환하고,
그렇지 않으면 다음 달 후보를 반환한다.

```text
anchor 31, 2026-10-10 -> 2026-10-31
anchor 1,  2026-10-31 -> 2026-11-01
anchor 31, 2026-10-31 -> 2026-11-30
anchor 31, 2026-02-10 -> 2026-02-28
```

Resume의 새 `currentPeriod`와 `nextBillingDate`는 이 helper로 계산하지 않는다. Pause
당시 남은 일수와 실제 동결 일수를 사용한다.

### 일시정지

일시정지는 이미 결제한 선결제 이용 기간의 남은 부분을 동결하고 실제 재개 시 이어서
사용하는 행위다. 허용되는 생명주기 전이는 `ACTIVE -> PAUSED`다. 실행 차단 사유가 있는
ACTIVE 구독도 고객 의사로 일시정지할 수 있다.

도메인 입력은 `pauseUntilDate`, `pausedAt`이다. `pauseDate`는
`pausedAt.toLocalDate()`에서 도출한다.

먼저 다음 날짜 불변식을 검증한다.

```text
pauseUntilDate >= pauseDate
currentPeriod.startDate <= pauseDate <= currentPeriod.endDate
```

첫 조건 위반은 클라이언트가 종료일을 수정할 수 있는
`InvalidSubscriptionPausePeriodException`이다. 두 번째 조건 위반은 정상 ACTIVE
구독과 애플리케이션의 현재 업무 날짜가 불일치한 내부 기간 상태 문제이므로
`InvalidSubscriptionPeriodStateException`으로 표현한다.

검증 후 다음 값을 계산하고 상태를 전이한다.

```text
remainingPaidDays = DAYS.between(pauseDate, currentPeriod.endDate)
scheduledResumeDate = pauseUntilDate.plusDays(1)
nextBillingDate = scheduledResumeDate.plusDays(remainingPaidDays)
currentPeriod = null
pausedAt = 요청 발생 시각
lifecycleStatus = PAUSED
```

Pause 시 `billingAnchorDay`는 변경하지 않는다. `scheduledResumeDate`는 미래 계획이며
그 전에 Manual Resume이 발생하면 실제 청구 일정이 달라질 수 있기 때문이다.

#### PAUSED의 nextBillingDate

PAUSED 상태의 `nextBillingDate`는 현재 `scheduledResumeDate`에 정상 재개한다는 계획을
기준으로 한 다음 결제 예정일이다.

```text
nextBillingDate = scheduledResumeDate + remainingPaidDays
```

예를 들어 `remainingPaidDays = 20`, `scheduledResumeDate = 05/01`이면
`nextBillingDate = 05/21`이다. Manual Resume이 일찍 또는 늦게 발생하면 실제 Resume
날짜로 다시 계산한다. Auto Resume 실패 뒤 예정 결제일을 재계산하는 정책은 아직
확정하지 않는다.

### 수동 재개

Manual Resume은 고객이 날짜를 선택하는 기능이 아니다. 수동 재개 요청이 발생한 현재
KST 업무 날짜에 즉시 재개한다.

```text
ResumeSubscriptionCommand(subscriptionId)
-> ResumeSubscriptionService
-> LocalDate resumedDate = LocalDate.now(clock)
-> subscription.resume(resumedDate)
```

`ResumeSubscriptionCommand`에는 `resumedDate`가 없다. 도메인은 방어적 시간 순서
불변식으로 다음을 검증한다.

```text
resumedDate >= pausedAt.toLocalDate()
```

이 조건은 클라이언트 입력 검증이 아니다. 정상 흐름에서 위반되면 애플리케이션 Clock이나
복원 상태의 문제이므로 `InvalidSubscriptionResumeDateException`은 내부 상태 예외로
취급하고 4xx 전용 Handler를 두지 않는다.

`scheduledResumeDate`는 Manual Resume 가능 범위를 제한하지 않는다. 해당 날짜 이전,
당일, 이후에도 아직 PAUSED라면 요청 시점에 재개할 수 있다. 문서의 날짜 예시는 고객이
날짜를 고르는 입력 예시가 아니라 각 날짜에 실제 요청이 발생한 시나리오다.

#### 재개 날짜 계산

Pause가 없었다면 원래 도래했을 결제일과 실제 동결 일수는 다음처럼 계산한다.

```text
pauseDate = pausedAt.toLocalDate()
baseNextBillingDate = pauseDate.plusDays(remainingPaidDays + 1)
frozenDays = max(0, DAYS.between(pauseDate.plusDays(1), resumedDate))
actualNextBillingDate = baseNextBillingDate.plusDays(frozenDays)
```

`remainingPaidDays > 0`이면 다음처럼 ACTIVE 이용 구간을 재구성한다.

```text
currentPeriod.startDate = resumedDate
currentPeriod.endDate = actualNextBillingDate.minusDays(1)
nextBillingDate = actualNextBillingDate
remainingPaidDays = null
pausedAt = null
scheduledResumeDate = null
lifecycleStatus = ACTIVE
```

Pause 당일과 Resume 당일은 모두 사용 일자이므로 같은 날과 다음 날 Resume은
`frozenDays = 0`이다. 이 경우 기존 결제 일정과 `billingAnchorDay`를 유지한다.

완전히 동결된 날짜가 하나 이상이면 실제 결제 일정이 이동했으므로 Resume 성공 시
다음처럼 정기 결제 기준을 재정렬한다.

```text
frozenDays > 0
-> billingAnchorDay = BillingAnchorDay.from(actualNextBillingDate)
```

월말 보정만으로 실제 결제일의 일자가 달라진 경우에는 anchor를 변경하지 않는다.

#### remainingPaidDays가 0인 재개

마지막 유료일에 Pause한 뒤 같은 날 Resume하면 마지막 날짜를 아직 사용할 수 있으므로
새 결제 없이 ACTIVE로 돌아간다.

```text
currentPeriod = pauseDate ~ pauseDate
nextBillingDate = pauseDate.plusDays(1)
billingAnchorDay = 기존 값 유지
```

다음 날 이후에는 남은 선결제 이용권이 없으므로 일반 Manual Resume만으로 ACTIVE가 될 수
없다. `SubscriptionResumeRequiresPaymentException`을 발생시키고 결제 성공 전까지 PAUSED
상태를 유지한다. 수동 재활성화 Billing과 Payment 흐름은 `docs/billing-payment.md`를
따른다.

### 결제를 통한 수동 재활성화

`PAUSED`이고 `remainingPaidDays == 0`인 Subscription만 수동 재활성화 Billing의 대상이다.
Payment 승인 전에는 새 `currentPeriod`를 만들거나 ACTIVE로 전이하지 않는다.

결제가 승인되면 Payment 성공 KST 업무 날짜를 시작으로 새 선결제 이용 구간을 열고,
그 날짜의 일자를 새 `billingAnchorDay`로 확정한다. 기존 월말 보정 규칙으로 다음
`nextBillingDate`를 구하고 `currentPeriod.endDate`를 그 전날로 정한다. PAUSED 전용 필드를
비우고 ACTIVE로 전이하며 `PAYMENT_FAILED`만 제거한다. 다른 실행 차단 사유는 유지한다.

결제가 거절되면 PAUSED 기간 문맥과 기존 청구 일정을 바꾸지 않고 `PAYMENT_FAILED`만
추가한다. 상세한 Billing·Payment 책임, 가격과 실패 규칙은
`docs/billing-payment.md`에서 관리한다.

### 취소

`cancel()`은 다음 전이를 허용하는 멱등 도메인 행위다.

```text
ACTIVE -> CANCELLED
PAUSED -> CANCELLED
CANCELLED -> CANCELLED
```

취소하면 `currentPeriod`, `remainingPaidDays`, `nextBillingDate`, `pausedAt`,
`scheduledResumeDate`를 모두 비운다. 남은 선결제 기간의 환불 또는 소멸 정책은 아직
확정하지 않는다.

### 예외와 HTTP 상태

예외를 HTTP 상태로 매핑할 때 클라이언트가 해결할 수 있는지, 현재 리소스와 충돌하는지,
정상 흐름에서 불가능한 내부 불변식 위반인지를 구분한다.

| 예외 | 의미 | HTTP |
|---|---|---|
| `InvalidSubscriptionPausePeriodException` | `pauseUntilDate` 입력 오류 | `400 BAD_REQUEST` |
| `InvalidSubscriptionStateChangeException` | 현재 생명주기 상태와 행위 충돌 | `409 CONFLICT` |
| `SubscriptionResumeRequiresPaymentException` | 현재 잔여 이용권만으로 재개 불가 | `409 CONFLICT` |
| `InvalidSubscriptionResumeDateException` | 내부 시간 순서 불변식 위반 | 전용 4xx Handler 없음 |
| `InvalidSubscriptionPeriodStateException` | ACTIVE/PAUSED 기간 문맥 불변식 위반 | 전용 4xx Handler 없음 |

도메인 불변식 예외는 구독 도메인 패키지에 둔다. 리소스 부재나 중복 구독 같은
애플리케이션 수준 실패와 구분하며, 현재 요구 없이 범용 비즈니스 예외 계층을 만들지
않는다.

### HTTP API와 소유권 인가

`POST /subscriptions`는 `201 CREATED`와 `{ "subscriptionId": ... }`를 반환한다.

구독 리소스 식별자는 조회와 상태 변경에서 모두 Path Variable을 사용한다.

```text
GET   /subscriptions/{id}
PATCH /subscriptions/{id}/pause?pauseUntilDate=YYYY-MM-DD
PATCH /subscriptions/{id}/resume
```

도메인과 유즈케이스의 용어에 맞춰 `hold`를 사용하지 않는다. `pauseUntilDate`는 리소스
식별자가 아니라 Pause 행위의 입력이므로 Request Parameter로 전달한다.

소유권 인가는 Controller 파라미터 이름이나 위치가 아니라
`@RequireOwnership`이 붙은 로더 또는 조회가 반환한 리소스와 등록된
`OwnershipResolver`를 기준으로 수행한다. Path Variable 전환이 이 인가를 우회하거나
제거하지 않는다.

### 조회 모델

구독 상세 조회는 `SubscriptionDetail`, `SubscriptionInfo`, `ProductInfo`를 조합한다.
조합은 애그리거트 경계를 바꾸지 않는다. `Subscription`은 계속 `ProductId`만 보유한다.

조회 모델과 HTTP Response도 도메인과 같은 용어를 사용한다.

- `deliveryCycleUnit`, `deliveryCycleInterval`
- `currentPeriodStartDate`, `currentPeriodEndDate`
- `remainingPaidDays`
- `scheduledResumeDate`
- `nextBillingDate`

PAUSED 조회에서는 `currentPeriod` 날짜가 null이고 `remainingPaidDays`가 존재한다.
ACTIVE 조회에서는 그 반대다. `SubscriptionInfo.customerId`를 사용하는 기존 소유권 인가
흐름을 유지한다.

---

## 결정 이유

### 생명주기와 실행 차단 사유를 분리한 이유

고객의 일시정지와 상품 공급 문제·결제 실패는 동시에 존재할 수 있다. 하나의 상태값으로
압축하면 한 원인을 해제할 때 다른 원인이나 고객 생명주기까지 잘못 변경할 수 있다.

Product 공급 문제는 고객이 선택한 `PAUSED`의 의미를 변경하지 않는다. 기존
`SubscriptionSuspensionReason`을 사용하면 Subscription 모델을 추가로 변경하지
않으면서 공급 문제와 결제 실패 등 여러 실행 차단 사유를 독립적으로 유지할 수
있다.

### 일시정지가 선결제 이용 기간을 동결하는 이유

일시정지는 실행만 막고 이미 결제한 기간을 계속 소진하는 기능이 아니다. 고객이 실제로
이용하지 못한 완전한 날짜만큼 이용권과 다음 결제 일정을 뒤로 이동시켜야 한다.

### currentPeriod를 ACTIVE 전용으로 둔 이유

PAUSED에서 과거 기간을 남은 일수 계산용으로 보존하면 같은 필드가 상태에 따라 “현재
이용 구간”과 “과거 결제 구간”이라는 서로 다른 의미를 갖는다. ACTIVE의 현재 이용 가능
구간은 `currentPeriod`, PAUSED의 남은 이용권은 `remainingPaidDays`로 분리한다.

### remainingPaidDays를 값 객체로 만들지 않은 이유

현재 규칙은 0 이상이라는 상태 불변식과 날짜 계산뿐이다. 별도 값 객체가 제공할 추가
행위나 타입 안전성 이점이 생기기 전까지 nullable `Integer`가 가장 작은 표현이다.

### scheduledResumeDate와 PAUSED nextBillingDate를 유지하는 이유

일시정지에는 필수 종료일이 있고 정상 운영에서는 다음 날 Auto Resume을 시도할 예정이다.
따라서 PAUSED 상태에서도 미래 재개 계획과 그 계획을 반영한 결제 예정일을 명시적으로
보여준다. 실제 Manual Resume이나 Auto Resume 실패가 발생하면 실제 일정으로 다시
계산해야 한다.

### 실제 동결 때만 billingAnchorDay를 재정렬하는 이유

월말 보정은 기존 기준일을 잃어야 할 이유가 아니다. 반면 Pause로 완전한 날짜가 동결되어
실제 다음 결제 일정이 이동하면 이후 정기 일정도 새 날짜에 맞춰야 한다. 따라서
`frozenDays > 0`인 Resume 성공에서만 anchor를 새 `nextBillingDate`로 재정렬한다.

### Clock을 애플리케이션에 두는 이유

현재 시간은 외부 환경 값이다. 애플리케이션 서비스가 KST Clock으로 현재 날짜와 시각을
만들어 도메인에 전달하면 도메인은 프레임워크와 시간 인프라스트럭처에 의존하지 않고
테스트에서 날짜를 명확히 제어할 수 있다.

### 일반 Resume이 결제가 필요한 재활성화를 완료하지 않는 이유

남은 이용권이 0이고 마지막 유료일이 지난 뒤에는 새 결제 결과 없이는 ACTIVE 이용
구간과 다음 일정을 확정할 수 없다. 일반 `resume()`은 성공한 것처럼 상태를 만들지 않고
명시적인 충돌로 남긴다. Payment 승인 뒤의 별도 도메인 행위만 새 이용 구간을 만들 수
있다.

---

## 후속 논의 대상

이 절은 현재 구현 요구사항이 아니다. 명시적인 요청 없이 열거형, 필드, 인터페이스,
Policy, Strategy, Event, Adapter, DB 스키마나 범용 추상화를 추가하지 않는다.

### Billing과 결제

- 최초 구독 결제와 자동 정기결제
- 결제 실패 재시도와 Billing 1 : N Payment
- PENDING Billing 만료·취소와 Payment idempotency
- 실제 PG와 Payment Method
- PG 성공 후 저장 실패 복구와 최종적 일관성
- Billing과 Pause가 같은 날 실행될 때의 순서와 동시성
- 청구 Batch와 중복 결제 방지

### Auto Resume

- `scheduledResumeDate` 기반 Auto Resume Scheduler
- `remainingPaidDays == 0`일 때 선결제 후 재개 조율
- Auto Resume 실패 후 재시도 날짜와 PAUSED `nextBillingDate` 재계산
- Manual Resume과 Auto Resume의 동시성
- 중복 상태 전이와 중복 결제 방지

향후 Auto Resume은 `scheduledResumeDate`를 실제 `resumedDate`로 전달해 동일한 도메인
규칙을 사용할 수 있어야 한다.

### 상품과 실행 차단

- Product 실행 차단 기간에도 유료 이용 기간을 동결할지 여부
- `DISCONTINUED` Product의 기존 Subscription 최종 처리
- 환불, 잔여 이용 기간 소멸, 대체 상품 전환
- 상품 공급 상태 변경 뒤 구독 대량 처리와 Event, Async, Batch 도입 여부
- Product와 Subscription 갱신의 동시성 및 최종적 일관성

### 배송

- `DeliveryCycle` 기반 배송 일정
- `nextDeliveryDate` 필요 여부
- Pause 중 배송 건너뛰기와 보상
- Resume 뒤 배송 일정 재조정

### 취소

- Cancel 시 남은 선결제 기간의 환불 또는 소멸 정책
- CANCELLED에서 실행 차단 사유를 보존할지 여부

### 영속화와 복원

- JDBC와 DB 스키마
- 애그리거트 복원 API
- 복원 시 상태별 필드 불변식 검증 위치와 실패 처리
- DB column 시간대 정책

### 시간과 이력

- 다중 시간대
- `Instant`, `OffsetDateTime`, `ZonedDateTime` 전환 여부
- Pause 이력, 상태 변경 이력, 감사 로그

---

## 작업 체크리스트

구독, 상품, 청구, 결제 또는 배송을 변경할 때 다음을 확인한다.

1. 루트 `AGENTS.md`, `docs/glossary.md`, 관련 아키텍처·도메인·보안 문서를 읽는다.
2. 이 문서의 확정된 규칙과 새 요구사항의 충돌 여부를 확인한다.
3. 후속 논의 대상은 참고하되 현재 요청 없이 구현하지 않는다.
4. 현재 요청 범위에 필요한 최소 설계를 수행한다.
5. 새 정책이 확정되면 확정된 규칙으로 옮기고 중요한 결정 이유를 기록한다.
6. 상태 변경은 의미 있는 도메인 행위로 수행한다.
7. 코드, 테스트, 조회 모델, HTTP API와 문서에서 같은 비즈니스 용어를 사용한다.
8. 날짜 경계와 상태별 불변식을 테스트한다.
9. 구현 후 `docs/review-guidelines.md`를 기준으로 자체 리뷰한다.
