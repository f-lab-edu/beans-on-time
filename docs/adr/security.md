# 보안

## 목적

Beans on Time의 보안은 다음 관심사를 분리한다.

1. 인증
2. 요청 수준 인가
3. 리소스 수준 인가
4. 비즈니스 규칙

이 관심사들을 불필요하게 혼합하지 않는다.

---

# 인증

Spring Security가 인증을 담당한다.

현재 개발 환경은 HTTP Basic 인증을 사용한다.

인증된 주체는 현재 다음 비즈니스 역할을 표현한다.

- 고객
- 판매자

프레임워크 고유 인증 모델은 보안 어댑터 계층에 둔다.

예:

~~~text
AuthenticatedCustomer
AuthenticatedSeller
~~~

`CustomerId`와 `SellerId` 같은 비즈니스 식별자는 도메인 타입으로 유지한다.

---

# 현재 주체 제공자

인증된 식별자가 애플리케이션 입력으로 필요하면 애플리케이션 계층이
SecurityContextHolder에 직접 접근하지 않고 추상화에 의존한다.

예:

~~~text
CurrentCustomerProvider
CurrentSellerProvider
~~~

Spring Security 고유 구현은 SecurityContextHolder에서 인증 정보를 읽을 수 있다.

이 구조를 통해 애플리케이션 서비스가 Spring Security API에 의존하지 않게 한다.

---

# 요청 수준 인가

SecurityConfig는 HTTP 요청에 대한 큰 범위의 인가를 제공한다.

현재 의도한 규칙은 다음과 같다.

~~~text
GET /products/**
→ 공개

POST /products
→ SELLER

/subscriptions/**
→ CUSTOMER

/billings/**
→ CUSTOMER
~~~

요청 수준 인가는 다음 질문에 답한다.

> 이 종류의 주체가 이 종류의 엔드포인트에 접근할 수 있는가?

다음 질문에는 답하지 않는다.

> 이 주체가 특정 리소스의 소유자인가?

---

# 리소스 수준 인가

리소스 소유권은 요청 수준 역할 인가와 별도로 평가한다.

예:

~~~text
ROLE_SELLER
→ 판매자가 상품 관리 엔드포인트에 진입할 수 있음

Product.sellerId == 인증된 SellerId
→ 판매자가 해당 상품을 수정할 수 있음
~~~

현재 구독 소유권 인가는 어노테이션/AOP 기반 구조를 사용한다.

재활성화 Billing 준비는 Subscription, Checkout 조회는 전용 조회 모델, Payment 명령은
Billing 반환값으로 각각 소유권을 독립적으로 확인한다. 앞선 API의 인가 결과를 다음
API의 보안 근거로 사용하지 않는다.

동등한 상품 소유권 인가는 실제 판매자 소유 변경 유즈케이스가 생길 때 도입한다.

구독과 구조를 맞추기 위한 목적으로 상품 소유권 AOP를 미리 추가하지 않는다.

---

# 소유권 데이터

인증에서 신뢰할 수 있는 식별자를 얻을 수 있다면 클라이언트가 전달한 소유권 식별자를
신뢰하지 않는다.

나쁜 예:

~~~text
POST /products

{
  "sellerId": 123,
  ...
}
~~~

판매자 식별자는 인증 정보에서 가져와야 한다.

권장 흐름:

~~~text
인증
→ CurrentSellerProvider
→ SellerId
→ RegisterProductService
→ Product
~~~

같은 원칙을 고객 소유 작업에도 필요에 따라 적용한다.

---

# 인가와 비즈니스 규칙

인가와 비즈니스 검증은 서로 다른 관심사다.

예:

~~~text
이 고객이 구독 소유자인가?
→ 인가

이 고객이 이미 이 상품을 구독 중인가?
→ 비즈니스 규칙

PAUSED 구독을 재개할 수 있는가?
→ 도메인 규칙
~~~

인가에 AOP를 사용한다는 이유로 비즈니스 검증을 AOP로 옮기지 않는다.

---

# 보안 예외

인증과 인가 실패에는 보안 계층의 의미를 사용한다.

예:

~~~text
AuthenticationCredentialsNotFoundException
AccessDeniedException
~~~

명시적인 HTTP 오류 응답 요구사항이 없다면 도메인 예외 처리기가 보안 실패까지
처리하지 않는다.

애플리케이션 또는 도메인 예외를 인증·인가 실패의 대체 수단으로 사용하지 않는다.

---

# 보안의 발전

고객과 판매자 구현의 구조가 비슷하다는 이유만으로 범용 행위 주체 모델을 미리
도입하지 않는다.

실제 공통 의미를 먼저 관찰한다.

고객과 판매자 인증 모델에 공통 요구사항이 확인된 뒤에만 일반화한다.

구조적 중복만으로 공통 추상화를 만들지 않는다.

관리자와 시스템 실행 주체의 인증·인가 정책은 현재 미정이다. 관련 유즈케이스가 실제로
필요해질 때 논의하며, 역할이나 범용 실행 주체 모델을 선제적으로 추가하지 않는다.
