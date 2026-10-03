# 토스 테스트 결제 실행

## 범위와 준비

이 연결은 기존의 **잔여 이용권이 없는 PAUSED 구독 재활성화**를 위한 카드/간편결제
테스트다. 최초 구독 결제, 정기 자동결제와 고객 요청 환불은 포함하지 않는다.
라이브 키는 시작 시 거절한다. 기본 프로필은 계속 Fake Gateway이며, `toss-test`에서만
토스 외부 API를 호출한다. `in-memory`와 `toss-test`를 함께 사용하지 않는다.

토스 개발자센터에서 **동일 테스트 상점의 API 개별 연동** 클라이언트 키(`test_ck_…`)와
시크릿 키(`test_sk_…`)를 준비한다. 시크릿 키는 채팅·브라우저 코드·Git에 넣지 않고
로컬 실행 환경의 `TOSS_TEST_SECRET_KEY`에만 설정한다. 클라이언트 키 환경 변수는
`TOSS_TEST_CLIENT_KEY`다. `.env` 파일은 자동으로 읽지 않는다.

Java 25와 Docker를 준비하고 두 환경 변수가 설정된 터미널에서 실행한다.

```sh
./gradlew bootRun --args='--spring.profiles.active=toss-test'
```

Compose가 PostgreSQL 17을 시작하고 Flyway V1~V3를 적용한다. DB를 직접 관리한다면
기존 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 설정을 사용한다.

## 결제창부터 결과 확인까지

1. 재활성화 대상 구독의 고객으로 `POST /subscriptions/{id}/reactivation-billing`을
   호출해 Billing ID를 얻는다. 구독 상태·잔여 일수·상품 공급 상태를 기존 규칙대로 검사한다.
2. 브라우저에서 `http://localhost:8080/billings/toss-test`를 연다. 개발 인증 계정은
   `customer1` / `password1` 또는 해당 구독 소유 고객이다.
3. Billing ID를 입력하고 **테스트 결제창 열기**를 누른다. 서버가 생성한 PG `orderId`와
   Billing 가격으로 토스 카드 인증을 진행한다. 이 `orderId`는 도메인 Order가 아니다.
4. 인증 후 같은 페이지로 돌아오면 서버가 저장한 청구 금액으로 승인한다. 브라우저의
   인증 결과 금액도 준비한 금액과 비교한다. `paymentKey`와 `orderId`는 Payment 시도와
   함께 커밋한 뒤 외부 승인을 호출한다.
5. `PROCESSING` 또는 `CANCEL_PENDING`이면 **결제 결과 다시 확인**을 사용한다.
   새 승인을 요청하지 않고 기존 시도를 조회한다. 기본 백그라운드 확인 주기는 30초다.

직접 API를 연결할 때의 계약은 다음과 같다. 두 POST 모두 고객 인증·청구 소유권이 필요하다.

```text
POST /billings/{billingId}/payment-checkout
-> { "orderId": "bot_…", "amount": 10000, "clientKey": "test_ck_…" }

POST /billings/{billingId}/payments
{ "orderId": "bot_…", "paymentKey": "인증 결과의 키" }

POST /billings/{billingId}/payments/{paymentId}/reconcile
```

토스 프로필에서 인증 정보 없는 기존 Fake 결제 요청은 허용하지 않는다. 인증창을 닫은
것만으로 Payment를 생성하지 않는다. 청구 만료 뒤 인증이 끝나면 새 승인을 시작하지 않는다.
인증 실패나 명확한 결제 거절 뒤 재시도할 때는 결제창 준비 API로 새 `orderId`를 받는다.
다른 청구의 주문번호·이미 사용한 주문번호로 시도를 바인딩할 수 없다.

## 보상과 운영 확인

승인·결제 조회 응답의 결제 키, 주문번호, 금액, 통화와 일반결제 타입을 검증한다.
HTTP 오류·404·Timeout·알 수 없는 상태만으로 FAILED를 만들지 않는다. PG에서 조회한
미승인 ABORTED/EXPIRED만 명확한 실패로 반영한다. 승인 요청을 백그라운드에서 재전송하지 않는다.

승인 증거는 `payment_approvals`에 보존한다. 내부 반영 완료가 확인되면 취소하지 않는다.
DB 반영 시도가 중단된 APPLYING은 2분 동안 진행 중 요청과의 경쟁을 피한 뒤, 동일 상품
잠금 아래 최신 결과를 확인한다. 이 2분은 결제 실패 기한이 아니다. SUCCESS 여부와
구독 재활성화 조건을 확인한 뒤 미반영 건에만 취소 결정을 저장한다.
취소 결정과 멱등키가 커밋되기 전에는 PG 취소를 호출하지 않는다.

취소 응답 유실이나 취소 후 DB 실패는 CANCEL_PENDING으로 남는다. PG의 전액 취소 이력과
잔액 0을 확인하고 Payment·Billing·취소 증거를 함께 저장하면 CANCELLED가 된다.
기존 Billing은 종료하며 새 Billing에서 현재 가격과 구독·공급 조건을 다시 확인한다.
승인 거래 ID는 취소 거래 ID로 덮어쓰지 않는다.

도메인 충돌은 REVIEW로 보존하고 자동 취소하지 않는다. 취소 요청이 15일 이상 지난 경우에도
새 멱등키를 만들거나 취소를 재전송하지 않고 조회만 한다. REVIEW, 장기간 PROCESSING,
15일 이후 CANCEL_PENDING은 저장된 승인·취소 기록과 토스 개발자센터를 통한 운영 확인이
필요하다. 운영 처리 API와 고객 환불 기능은 이번 범위가 아니다.

## 검증 범위

자동 테스트는 실제 PostgreSQL 컨테이너와 모의 토스 HTTP/클라이언트 응답을 사용한다.
승인·취소 응답 유실, 내부 결과 롤백, 커밋 응답 유실, 취소 결정·취소 완료 저장 실패,
동시 처리, 소유권, 금액·식별자 검증과 멱등키 만료를 검증한다.
외부 토스 테스트 상점에서의 카드 인증·승인·취소는 로컬 테스트 키로 별도 확인해야 한다.
실제 금액 거래를 실행하는 라이브 구성은 제공하지 않는다.

### 2026-10-03 검증 결과

- `./gradlew spotlessCheck test`를 포함한 검증에서 244개 테스트가 통과했다
  (실패·오류·건너뜀 0개). PostgreSQL 영속화와 결제 복구·보상 취소는 모의 토스 응답으로 검증했다.
- 외부 테스트 상점의 결제창 표시까지 확인했다. 카드 인증을 진행하지 않기로 하여
  외부 승인·조회·보상 취소의 전체 흐름은 미검증으로 남긴다. 격리된 검증 DB의 Payment는 0건이다.
- 테스트 화면에서 큰 정수 ID가 JavaScript 숫자로 반올림되는 문제를 수정했다.
  실제 화면 스크립트를 모의 HTTP 응답으로 실행해 64비트 ID 보존과 재조회 URL을 확인했다.
- 카드 인증 없는 자동 검증을 현재 완료 범위로 삼는다. 외부 상점 검증 결과로 대체해 해석하지 않는다.

공식 계약: [결제창 연동](https://docs.tosspayments.com/guides/v2/payment-window/integration),
[결제·조회·취소 API](https://docs.tosspayments.com/reference),
[멱등키](https://docs.tosspayments.com/reference/using-api/authorization).
