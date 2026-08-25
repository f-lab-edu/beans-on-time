# 도메인 모델

## 목적

이 문서는 Beans on Time의 현재 도메인 모델과 애그리거트 경계를 기록한다.

도메인 모델은 실제 비즈니스 요구사항에 따라 발전해야 한다.

추측에 기반한 모델링을 정당화하는 문서로 사용하지 않는다.

상세 도메인 문서는 `docs/README.md`에 정의한 “확정된 규칙”, “결정 이유”, “후속 논의
대상”을 구분한다.

---

# 구독

`Subscription`은 고객의 반복적인 커피 배송 구독을 표현하는 애그리거트 루트다.

현재 주요 개념은 다음과 같다.

- SubscriptionId
- CustomerId
- ProductId
- Cycle
- SubscriptionPeriod
- BillingAnchorDay
- SubscriptionStatus
- SubscriptionSuspensionReason

구독은 자신의 생명주기와 상태 전이를 소유한다.

생명주기 상태는 다음 세 값이다.

- ACTIVE
- PAUSED
- CANCELLED

CANCELLED는 최종 생명주기 상태다. 생명주기 상태와 실행 차단 사유 집합은 서로 독립적인
상태 축이다. 구독 회차, 청구 일정, 납품 주기도 서로 다른 시간 개념이다.

구독의 상세한 확정 규칙은 `docs/subscription.md`에 기록한다. 구독 또는 관련 상품,
청구, 배송 동작을 변경하기 전에 해당 문서를 읽는다. 문서는 확정된 규칙, 결정 이유,
아직 결정되지 않은 후속 논의 대상을 구분한다.

비즈니스 동작은 행위 중심 메서드로 표현한다.

예:

~~~java
subscription.pause();
subscription.resume();
~~~

잘못된 상태 전이는 도메인에서 거부한다.

---

## 구독의 참조

고객과 상품은 서로 독립적인 생명주기를 가진다.

따라서 구독은 식별자를 사용해 이들을 참조한다.

`Subscription` 안에 `Customer` 또는 `Product` 애그리거트 객체를 포함하지 않는다.

예:

~~~text
Subscription
- CustomerId
- ProductId
~~~

---

# 상품

`Product`는 판매자가 제공하는 커피 상품을 표현하는 애그리거트 루트다.

상품은 판매자가 소유하고 관리하며 고객에게 공개된다.

현재 최소 유즈케이스는 다음과 같다.

- 상품 등록
- 공개 단건 상품 조회

현재 주요 개념은 다음과 같다.

- ProductId
- SellerId
- Name
- Description
- Price
- ProductStatus
- ProductImage
- 지원하는 분쇄 방식
- 크기 선택지

현재 `ProductStatus`는 판매 생명주기인 `ACTIVE`와 `INACTIVE`를 가진다.

`ProductStatus`는 상품 납품 가능 상태가 아니다. 납품 가능 상태는
`docs/subscription.md`에 후속 논의 대상으로 기록한다. 상품 모델은 최소한으로
유지하고 실제 요구사항이 생길 때 확장한다.

---

## 상품 소유권

상품은 판매자에게 속한다.

상품 애그리거트는 `SellerId`를 사용해 판매자를 참조한다.

`SellerId`는 보안 개념이 아니라 판매자 도메인 식별자를 표현해야 한다.

상품 등록 시 클라이언트가 제출한 `SellerId`를 신뢰하지 않고 인증된 판매자로부터
`SellerId`를 가져온다.

---

## 상품 조회

상품 정보는 공개적으로 조회할 수 있다.

공개 상품 조회에는 상품 소유권 인가가 필요하지 않다.

상품 수정처럼 판매자가 소유한 변경 유즈케이스가 생길 때 판매자 소유권 인가를
도입한다.

---

# 고객

`Customer`는 고객이라는 비즈니스 개념을 표현한다.

구독이나 보안이 사용하더라도 `CustomerId`는 고객 도메인에 속한다.

`CustomerId`가 존재한다는 이유만으로 완전한 고객 애그리거트를 만들 필요는 없다.

---

# 판매자

`Seller`는 판매자라는 비즈니스 개념을 표현한다.

`SellerId`는 판매자 도메인에 속한다.

판매자 고유 비즈니스 행위가 필요할 때만 완전한 판매자 애그리거트를 도입한다.

---

# 값 객체

값에 도메인 의미, 검증 또는 타입 안전성의 이점이 있으면 값 객체를 사용한다.

현재 예:

- SubscriptionId
- ProductId
- CustomerId
- SellerId
- Cycle
- Money

값 객체는 일반적으로 불변이어야 한다.

Java record가 도메인 개념을 명확하게 표현한다면 사용할 수 있다.

모든 기본 타입을 감싸기 위해 값 객체를 만들지 않는다.

의미 있는 도메인 의미를 제공하거나 불변식을 보호할 때 도입한다.

---

# 식별자 소유권

식별자는 해당 의미를 소유하는 도메인에 둔다.

~~~text
CustomerId
→ customer.domain

SellerId
→ seller.domain

ProductId
→ product.domain

SubscriptionId
→ subscription.domain
~~~

현재 소비하는 코드의 패키지가 식별자의 소유권을 결정하지 않는다.

다음 질문을 기준으로 판단한다.

> 이 타입은 어떤 개념을 표현하는가?

다음 질문을 기준으로 판단하지 않는다.

> 현재 어떤 클래스가 이 타입을 사용하는가?

---

# 도메인 예외

도메인 불변식이나 잘못된 상태 전이를 나타내는 예외는 도메인 가까이에 둔다.

예:

~~~text
InvalidSubscriptionStateChangeException
InvalidSubscriptionPausePeriodException
InvalidSubscriptionResumeDateException
~~~

리소스 부재나 유즈케이스 충돌 같은 애플리케이션 수준 실패는 애플리케이션 계층에 둘 수
있다.

예:

~~~text
SubscriptionNotFoundException
DuplicateSubscriptionException
ProductNotFoundException
~~~

실제 필요가 생기기 전에는 범용 비즈니스 예외 계층을 만들지 않는다.

---

# 모델링 원칙

의미 있는 도메인 행위가 있다면 행위가 풍부한 모델을 선호한다.

모든 비즈니스 규칙을 애플리케이션 서비스로 옮겨 빈약한 도메인 모델을 만들지 않는다.

반대로 단순 데이터 개념을 객체 지향적으로 보이게 만들기 위해 인위적인 행위를
강제하지 않는다.

애그리거트 경계를 일관성 경계로 사용한다.

애그리거트 사이에서는 식별자를 사용한다.

조회 모델은 도메인 모델과 다른 구조를 가질 수 있다.

도메인 모델을 영속성 표현으로부터 독립적으로 유지한다.
