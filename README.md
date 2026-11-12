# Beans on Time

> **신선한 원두를, 정확한 일정에.**

Beans on Time은 고객이 선택한 커피 원두를 원하는 주기에 맞춰 배송하는 정기구독
백엔드 서비스다.

## 개요

고객은 원두 상품과 납품 주기를 선택해 구독을 시작하고, 구독을 일시정지하거나 수동으로
재개할 수 있다.

구독 회차는 한 번의 결제로 확보한 선결제 이용 기간이고, 청구 일정은 결제가 예정된
업무 날짜다. 납품 주기는 상품을 배송하는 간격으로, 구독 회차나 청구 일정과 구분한다.
배송할 때마다 청구하는 흐름으로 이 개념들을 합치지 않는다.

현재 청구·결제 구현은 남은 이용권이 없는 일시정지 구독의 수동 재활성화를 지원한다.
최초 구독은 결제 연동 없이 이용 구간을 초기화하며, 최초 결제는 후속 범위다.
정기 갱신은 Fake Gateway로 구현했고 실제 PG 자동결제 수단 등록은 후속 범위다.
배송 일정 계산, 정기배송 주문 생성, 출고와 배송 건너뛰기도 아직 구현하지 않았다.

## 프로젝트 초점

이 프로젝트는 단순한 구독 CRUD 구현보다 다음 문제를 다루는 데 중점을 둔다.

- 구독 회차, 청구 일정과 납품 주기의 분리
- 선결제 이용 기간과 일시정지·재개의 일관성
- 청구 가격 확정과 실제 결제 시도 결과의 분리
- 중복 청구·결제·주문 방지
- 결제 실패에 대한 재시도
- 처리 도중 발생한 장애의 복구
- 구독 변경사항의 적용 시점 관리

이 목록은 장기적인 학습·설계 과제를 포함하며 현재 구현 완료 목록은 아니다.

## 아키텍처

Beans on Time은 도메인 주도 설계와 헥사고날 아키텍처를 기반으로 설계한다.

도메인 규칙을 애플리케이션 프레임워크, 데이터베이스 및 외부 시스템으로부터 분리하고,
구독·결제·주문 처리 과정의 책임과 경계를 명확하게 정의하는 것을 목표로 한다.

## 기술 스택

- Java 25
- Spring Boot 4.1
- Spring MVC, Spring Security
- Gradle 9.x

상품·구독·청구·결제는 PostgreSQL 17과 Spring JDBC `JdbcClient`로 영속화하고, Flyway로 스키마를
관리한다. 도메인은 영속성 어노테이션을 사용하지 않는다.
Virtual Thread 사용은 현재 프로젝트 설정에 명시되어 있지 않다.

## 현재 상태

현재는 도메인 설계를 발전시키면서 다음 수직 기능을 구현한 단계다.

- 상품 등록과 공개 단건 조회
- 구독 생성·상세 조회·일시정지·수동 재개
- 수동 재활성화 청구 준비·Checkout 조회·결제 결과에 따른 구독 상태 변경
- 고객·판매자 인증과 역할 및 리소스 소유권 인가

결제는 `FakePaymentGatewayAdapter`로 승인·거절·응답 미확정을 다룬다. 거절 후 새
Payment로 재시도하고, 미확정 시에는 새 시도를 막고 결과를 재확인한다. 공급 불가
상품의 청구 준비·결제 시작을 차단하며 Billing은 생성 후 10분 동안 결제를 시작할 수 있다.
진행 중 Payment가 있으면 기한이 지나도 결과를 기다린다.

상품·구독·청구·결제 변경은 상품 행 잠금과 DB 트랜잭션으로 조율한다. 외부 호출 전에
PROCESSING 시도를 커밋하고 Payment 성공·Billing 완료·Subscription 활성화를 함께 반영한다.
미완료 결제 이력은 DB에서 다시 읽어 결과를 확인한다. Fake Gateway의 외부 결과 자체는
메모리이므로 프로세스 재시작 후 PG 결과 복구를 보장하지 않는다.
토스 테스트 프로필에서는 승인 증거를 저장하고 DB 미반영 확인 후 전액 보상 취소를 복구한다.
보상 완료 시 기존 청구를 종료하고 새 청구에서 현재 가격으로 재결제한다.
PG는 테스트 환경만 사용하며 라이브 환경·실제 금액 거래는 제외한다. 웹훅과 실제 PG 자동결제는 후속 범위다. 결과 조회는 기본 30초
간격이며 `payment.reconciliation.delay-ms`와 `payment.reconciliation.enabled`로 설정한다.

확정된 규칙과 후속 범위는 [프로젝트 문서](docs/README.md),
[구독 도메인 규칙](docs/domain/subscription.md), [청구·결제 도메인 규칙](docs/domain/billing-payment.md)을
기준으로 한다.

## 로컬 실행과 검증

Java 25와 Docker가 필요하다. 기본 구성은 PostgreSQL을 사용한다.

```sh
./gradlew bootRun
```

Spring Boot Docker Compose가 `compose.yaml`의 PostgreSQL을 시작한다. 데이터는
`postgres-data` 볼륨에 남고 앱 시작 시 Flyway가 `db/migration`을 적용한다.
기존 MySQL 볼륨의 삭제나 데이터 이관은 수행하지 않는다. 도메인 데이터는 기존에
InMemory였으므로 처음에는 빈 DB에서 시작한다.

별도 PostgreSQL을 사용할 때는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 설정하고
`SPRING_DOCKER_COMPOSE_ENABLED=false`로 Compose 자동 시작을 끈다.

```sh
./gradlew spotlessCheck test
```

통합 테스트는 Testcontainers PostgreSQL을 사용하며 로컬 개발 DB를 변경하지 않는다.
DB가 필요 없는 기존 API 테스트는 `in-memory` 프로필을 명시적으로 사용한다.
자세한 저장 범위와 실행 경계는 [영속화 결정](docs/adr/persistence.md)을 따른다.

## 토스 테스트 결제

`toss-test` 프로필에서만 토스 테스트 승인·조회·전액 보상 취소를 사용한다.
설정과 결제창 실행은 [토스 테스트 실행 안내](docs/toss-test.md)를 따른다.
기본 프로필은 Fake Gateway를 유지하며 라이브 키를 사용하는 구성은 제공하지 않는다.

## Fake Gateway 정기결제

기본적으로 비활성인 스케줄러를 아래와 같이 켤 수 있다. 토스 키나 카드 인증이 필요하지 않다.

```sh
./gradlew bootRun --args='--billing.recurring.enabled=true'
```

KST 청구 예정일이 도래한 ACTIVE·실행 차단 없는 구독을 100건씩 조회하고, 구독별 청구와
결제 시도를 커밋한 뒤 Fake Gateway를 호출한다. 실행 종료 후 기본 60초 뒤 재실행하며
`billing.recurring.delay-ms`로 간격을 설정한다. 정기 청구는 구독 ID·청구 예정일로 중복을
방지하고 10분 만료를 적용하지 않는다. 지연 결제도 예정일 기준 회차를 유지한다.
미확정은 기존 결과 조회로 복구하며 명확한 거절 후 자동 재시도·고객 알림은 제외한다.
`toss-test`에서는 이 스케줄러와 정기결제 유즈케이스가 등록되지 않는다.


## 구독 철회와 전액 환불

`POST /subscriptions/{id}/withdrawal`로 즉시 영구 철회하고, 최근 승인된 성공 결제 한 건이
최초 접수 시각 기준 24시간 이내면 자동 전액 환불한다. 환불 대상·판단은 최초 요청에서
고정하며, 철회 후 지연 승인된 결제는 별도로 반환한다.
`GET /subscriptions/{id}/withdrawal`로 최초 결정과 환불 결과를 조회한다.
환불은 Fake Gateway 또는 토스 테스트 환경만 사용하며 부분 환불은 지원하지 않는다.
미확정 환불은 기본 30초마다 복구하고 `refund.reconciliation.enabled`와
`refund.reconciliation.delay-ms`로 설정한다. 승인 시각을 알 수 없는 과거 결제는 자동 환불하지 않는다.
