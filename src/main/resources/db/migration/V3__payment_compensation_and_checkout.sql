alter table billings drop constraint billings_status_check;
alter table billings add constraint billings_status_check check (status in ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED'));
alter table payments drop constraint payments_status_check;
alter table payments drop constraint payment_approval;
alter table payments add constraint payments_status_check check (status in ('PROCESSING','SUCCESS','FAILED','CANCEL_PENDING','CANCELLED'));
alter table payments add constraint payment_approval check (
  (status in ('SUCCESS','CANCEL_PENDING','CANCELLED') and transaction_id is not null and length(trim(transaction_id)) > 0)
  or (status in ('PROCESSING','FAILED') and transaction_id is null)
);
drop index one_processing_payment_per_billing;
create unique index one_processing_payment_per_billing on payments(billing_id) where status in ('PROCESSING','CANCEL_PENDING');

-- PG 주문번호는 결제창 인증의 상관관계이며 도메인 Order가 아니다.
create table payment_checkouts (
  order_id varchar(64) primary key,
  billing_id bigint not null references billings(id),
  payment_id bigint unique references payments(id),
  payment_key varchar(200) unique,
  constraint bound_checkout check ((payment_id is null and payment_key is null) or (payment_id is not null and payment_key is not null))
);
create table payment_approvals (
  payment_id bigint primary key references payments(id),
  transaction_id text not null unique,
  approved_at timestamp not null,
  phase varchar(20) not null check (phase in ('READY','APPLYING','REVIEW','CANCEL_PENDING','CANCELLED')),
  application_started_at timestamp,
  cancel_key varchar(100) unique,
  cancel_requested_at timestamp,
  cancel_transaction_id text unique,
  cancelled_at timestamp,
  constraint application_started check (phase <> 'APPLYING' or application_started_at is not null),
  constraint cancellation_decision check (
    (phase in ('CANCEL_PENDING','CANCELLED') and cancel_key is not null and length(trim(cancel_key)) > 0 and cancel_requested_at is not null)
    or (phase not in ('CANCEL_PENDING','CANCELLED') and cancel_key is null and cancel_requested_at is null)
  ),
  constraint cancellation_receipt check (
    (phase = 'CANCELLED' and cancel_transaction_id is not null and length(trim(cancel_transaction_id)) > 0 and cancelled_at is not null)
    or (phase <> 'CANCELLED' and cancel_transaction_id is null and cancelled_at is null)
  )
);
