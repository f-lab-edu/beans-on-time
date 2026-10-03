# PostgreSQL 영속화와 실행 경계

## 결정과 범위

2026-10-03 PostgreSQL을 선택했다. feature/11은 상품·구독·청구·결제 영속화를 대상으로 하며
기존 MySQL 드라이버, Flyway 모듈, Compose와 Testcontainers를 PostgreSQL로 전환한다.
JPA·Hibernate를 도입하지 않고 기존 JDBC 의존성의 `JdbcClient`를 사용한다.
PG 라이브 환경과 실제 금액 거래는 구현 범위에서 제외한다.

## 저장과 복원

- 기본 구성은 JDBC이며 `in-memory` 프로필은 기존 메모리 테스트용이다.
- Flyway V1은 상품·구독, V2는 청구·결제, V3는 승인·취소와 결제창 인증 기록을 소유한다.
- 네 애그리거트는 순수 Java 도메인 모델을 유지하고 복원 메서드로 불변식을 검사한다.
- 저장 포트의 `saveNew`는 신규 INSERT, `save`는 기존 상태 갱신이다. 신규 식별자 충돌을
  UPSERT로 덮어쓰지 않는다. 현재 식별자 타입과 생성 방식은 유지한다.
- 고객·판매자 애그리거트나 테이블은 이번 요구에 필요하지 않아 추가하지 않는다.
- 상품·구독 상세 조회는 별도 조회 어댑터가 SQL로 조회 모델을 반환한다.
- 구독 차단 사유는 PostgreSQL `text[]`에 저장하고 도메인에서는 계속 EnumSet으로 다룬다.
- 업무 날짜와 시각은 기존 KST 계약을 따른다. JDBC 왕복에서 시간대를 다시 변환하지 않는다.
  시각 저장은 마이크로초 미만을 버려 DB 반올림에 의한 업무 날짜 변경을 막는다.

## 트랜잭션과 잠금

`ProductExecutionPort`는 상품 공급 변경과 신규 구독의 로드·검증·저장 경계를 제공한다.
JDBC 어댑터는 `TransactionTemplate`으로 트랜잭션을 열고 상품 행을 `FOR UPDATE`로 잠근다.
잠금 획득 후 최신 상태와 소유권을 확인하며, 공급 상태와 연관 구독 변경을 함께 커밋한다.

`SubscriptionExecutionPort`는 구독의 불변 ProductId로 동일한 상품 실행 경계에 참여한다.
일시정지·재개는 경계 안에서 구독을 다시 로드한다. 동일 상품의 변경을 직렬화하는 보수적인
방식으로 현재 동기 공급 연동의 누락과 갱신 유실을 막는다. 대량 갱신이나 처리량 요구 없이
이벤트·Outbox·범용 트랜잭션 프레임워크를 선제 도입하지 않는다.

애플리케이션 서비스는 순수 Java 포트로 실행을 요청하며 트랜잭션 API는 어댑터 안에 둔다.
저장 포트는 실행 경계를 대신하지 않으므로 기존 상태의 변경은 로드 전부터 경계에 참여해야 한다.
DB의 부분 UNIQUE 인덱스도 종료되지 않은 고객·상품 구독 중복을 방지한다.

## 청구·결제 실행과 복구

`BillingExecutionPort`는 구독 실행 경계로 위임한다. 청구·결제도 JDBC 저장소를 사용하여
Payment 성공·Billing 완료·Subscription 활성화를 한 트랜잭션으로 반영한다.
외부 호출 전 시도 생성과 호출 후 결과 반영은 별도의 짧은 트랜잭션이며 PG 호출 중에는
DB 잠금을 유지하지 않는다. 잠금 획득 후 최신 Billing·Payment를 다시 읽고, 결과 응답도
저장된 상태를 다시 읽어 반환한다. 같은 객체 인스턴스를 공유하는 InMemory 동작에 기대지 않는다.

부분 UNIQUE 인덱스로 구독별 PENDING 청구, 청구별 PROCESSING 시도와 SUCCESS 결과의
중복을 막는다. 만료 충돌은 EXPIRED 저장 트랜잭션을 커밋한 뒤 API에 반환한다.
백그라운드와 명시적인 결과 재확인은 DB의 PROCESSING 목록에서 시작한다.
Fake Gateway 결과는 메모리에 있으므로 DB만으로 외부 승인 결과를 복구할 수는 없다.

토스 테스트 프로필에서는 PaymentApproval에 승인 증거, 내부 반영 시작과 취소 결정을
별도 커밋한다. 정상 내부 반영의 트랜잭션 예외는 어댑터가 `PaymentCommitUncertainException`으로
변환한다. 애플리케이션은 DB 예외 타입에 의존하지 않고, 새 실행 경계에서 최신 성공 여부를
확인한다. 취소 결정은 정상 완료와 같은 상품 잠금으로 조율하고 외부 취소는 경계 밖에서 호출한다.

승인 증거의 READY/APPLYING/REVIEW/CANCEL_PENDING/CANCELLED는 복구 단계다.
Payment SUCCESS는 Billing PAID·Subscription ACTIVE와 원자적으로 확정되므로,
외부 승인 사실을 먼저 저장했다는 이유만으로 SUCCESS를 사용하지 않는다.
보상 완료는 Payment·Billing CANCELLED와 취소 결과를 함께 저장한다.
구체적인 규칙과 실행 방법은 [청구·결제 문서](../domain/billing-payment.md),
[토스 테스트 실행](../toss-test.md)을 따른다.

## 검증

실제 PostgreSQL 컨테이너로 마이그레이션, 상태별 복원과 조회, DB 제약, 공급 연동 롤백,
동시 구독·공급 변경·일시정지, 소유권 인가와 기존 Fake 결제 연동을 검증한다.
청구·결제에서는 새 저장소 객체를 통한 미완료 시도 복원, 승인일 기준 결과 반영, 동시 완료,
세 애그리거트 롤백, 중복 제약과 만료 충돌 후 상태 커밋도 검증한다.
단위 테스트에서는 기존 InMemory와 출력 포트 mock을 계속 사용한다.
