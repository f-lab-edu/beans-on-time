# 아키텍처

## 목적

Beans on Time은 도메인 주도 설계와 헥사고날 아키텍처를 사용하여 비즈니스 규칙을
프레임워크, 영속성 기술, 전달 메커니즘으로부터 분리한다.

아키텍처는 다음 목표를 우선한다.

- 명확한 도메인 경계
- 명시적인 의존 방향
- 교체 가능한 인프라스트럭처
- 테스트 가능성
- 유지보수성
- 점진적인 발전

아키텍처 자체가 목적은 아니다.

경계를 보존하면서 비즈니스 요구사항을 올바르게 표현하는 가장 단순한 설계를 선택한다.

---

# 아키텍처 스타일

프로젝트는 헥사고날 아키텍처를 따른다.

개념적인 의존 흐름은 다음과 같다.

~~~text
인바운드 어댑터
↓
입력 포트
↓
애플리케이션 서비스
↓
도메인
↓
출력 포트
↑
아웃바운드 어댑터
~~~

의존성은 애플리케이션과 도메인 핵심을 향해야 한다.

외부 계층은 내부 계층에 의존할 수 있지만, 내부 계층은 외부 인프라스트럭처에
의존해서는 안 된다.

---

# 패키지 구조

프로젝트는 주로 비즈니스 기능을 기준으로 구성한다.

예:

~~~text
com.bluetoya.beansontime
├─ subscription
│  ├─ domain
│  ├─ application
│  │  ├─ port
│  │  │  ├─ in
│  │  │  └─ out
│  │  └─ service
│  └─ adapter
│     ├─ in
│     │  └─ web
│     └─ out
│        └─ persistence
│
├─ product
│  ├─ domain
│  ├─ application
│  └─ adapter
│
├─ customer
│  └─ domain
│
├─ seller
│  └─ domain
│
└─ security
   ├─ application
   ├─ adapter
   ├─ annotation
   ├─ aspect
   └─ config
~~~

Java 구현 타입만을 기준으로 패키지를 만들지 않는다.

다음과 같은 구조를 피한다.

~~~text
record/
enum/
interface/
impl/
~~~

비즈니스 소유권과 아키텍처 책임을 기준으로 구성한다.

---

# 도메인 계층

도메인 계층은 비즈니스 개념, 상태, 행위와 불변식을 포함한다.

도메인 계층은 다음에 의존하지 않아야 한다.

- Spring Framework
- Spring Security
- 영속성 프레임워크
- HTTP
- 데이터베이스 고유 개념

애그리거트와 값 객체는 순수한 Java 도메인 모델로 유지한다.

도메인 객체 자체로 표현할 수 있는 비즈니스 행위는 도메인에 둔다.

다음을 선호한다.

~~~java
subscription.pause();
~~~

다음과 같은 일반 상태 변경은 피한다.

~~~java
subscription.setStatus(PAUSED);
~~~

---

# 애플리케이션 계층

애플리케이션 계층은 유즈케이스를 조율한다.

애플리케이션 서비스는 다음을 수행할 수 있다.

- 애그리거트 로드
- 도메인 행위 호출
- 애그리거트 저장
- 여러 포트 조율
- 애플리케이션 대상 추상화를 통한 인증 주체 정보 획득
- 조회 모델 조합

도메인 객체가 자연스럽게 소유해야 하는 규칙을 애플리케이션 서비스에서 중복하지 않는다.

애플리케이션 계층은 다음을 정의할 수 있다.

- 입력 포트
- 출력 포트
- 명령
- 조회 결과
- 유즈케이스 서비스

---

# 어댑터 계층

어댑터는 외부 메커니즘을 애플리케이션 포트에 연결한다.

인바운드 어댑터 예:

- REST Controller

아웃바운드 어댑터 예:

- InMemory 영속성
- JDBC 영속성
- 외부 API 클라이언트

어댑터는 프레임워크 고유 API에 의존할 수 있다.

프레임워크 고유 타입을 도메인 계층에 노출하지 않는다.

---

# 포트

## 입력 포트

입력 포트는 애플리케이션 유즈케이스를 표현한다.

가능하면 비즈니스 중심 용어를 사용한다.

예:

~~~text
RegisterProductUseCase
PauseSubscriptionUseCase
ResumeSubscriptionUseCase
FindProductQuery
~~~

비즈니스 행위가 있다면 다음과 같은 용어를 선호한다.

~~~text
register()
subscribe()
pause()
resume()
~~~

일반 CRUD 용어는 피한다.

## 출력 포트

출력 포트는 애플리케이션 계층이 필요로 하는 기능을 표현한다.

예:

~~~text
SaveProductPort
LoadSubscriptionPort
FindProductQueryPort
ExistsSubscriptionPort
~~~

이름에 구현 기술을 노출하지 않는다.

다음과 같은 이름을 피한다.

~~~text
MysqlProductPort
RedisSubscriptionPort
~~~

---

# 포트 이름 규칙

Beans on Time은 현재 다음 규칙을 사용한다.

- save: 애그리거트를 저장한다.
- load: 명령 측 도메인 행위를 위해 애그리거트를 로드하거나 재구성한다.
- find: 조회 데이터를 가져온다.
- exists: 존재 여부를 확인한다.

이는 보편적인 업계 표준이 아니라 프로젝트 관례다.

유즈케이스가 더 명확한 이름을 요구하지 않는 한 일관성을 유지한다.

---

# 명령과 조회 분리

프로젝트는 CQS를 따른다.

## 명령 측

명령 측은 상태를 변경한다.

일반적인 흐름은 다음과 같다.

~~~text
입력
→ 애플리케이션 서비스
→ 애그리거트 로드
→ 도메인 행위 실행
→ 애그리거트 저장
~~~

도메인 행위가 필요하면 실제 애그리거트를 사용한다.

## 조회 측

조회 측은 데이터를 읽는다.

조회는 애그리거트 대신 전용 조회 모델을 반환할 수 있다.

예:

~~~text
ProductQueryResult
SubscriptionQueryResult
~~~

조회 모델은 애그리거트 구조를 그대로 복제할 필요가 없다.

서로 다른 도메인의 정보를 조회 측에서 조합할 수 있다.

예:

~~~text
구독 데이터
+
상품 데이터
→ SubscriptionQueryResult
~~~

조회에 상품 정보가 필요하다는 이유로 `Product` 객체를 `Subscription` 애그리거트 안에
추가하지 않는다.

---

# 애그리거트 간 참조

애그리거트는 다른 애그리거트를 식별자로 참조한다.

예:

~~~text
Subscription
- SubscriptionId
- CustomerId
- ProductId

Product
- ProductId
- SellerId
~~~

의도적인 애그리거트 경계 재설계가 없는 한 다음과 같은 객체 그래프를 피한다.

~~~text
Subscription
└─ Product
   └─ Seller
~~~

---

# 점진적인 아키텍처

가상의 미래 요구사항을 위한 추상화를 도입하지 않는다.

다음 순서를 선호한다.

~~~text
실제 요구사항
→ 구체적인 구현
→ 중복 또는 변경 압력 관찰
→ 추상화 도입
~~~

다음 순서는 피한다.

~~~text
가능성만 있는 미래 요구사항
→ 범용 추상화
→ 현재 기능을 추상화에 억지로 맞춤
~~~

올바른 추상화를 발견하는 데 도움이 된다면 일시적인 중복을 허용한다.

아키텍처 일관성은 동등한 문제에 같은 원칙을 적용한다는 뜻이지, 모든 유즈케이스의
구조를 똑같이 만든다는 뜻이 아니다.
