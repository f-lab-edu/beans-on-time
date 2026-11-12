alter table payments add column approved_at timestamp;
alter table payments add constraint successful_approval_time check (approved_at is null or status = 'SUCCESS');
-- 승인 증거가 존재하는 성공 결제만 이관한다. 시도 시각으로 승인 시각을 추정하지 않는다.
update payments p set approved_at = a.approved_at from payment_approvals a
where p.id = a.payment_id and p.status = 'SUCCESS';
alter table subscriptions add column withdrawn_at timestamp;
alter table subscriptions add constraint withdrawn_subscription check (withdrawn_at is null or lifecycle_status = 'CANCELLED');
alter table billings drop constraint billings_status_check;
alter table billings add constraint billings_status_check check (status in ('PENDING','PAID','EXPIRED','CANCELLED','REFUNDED'));
create table refunds (
 id uuid primary key,
 payment_id bigint not null unique references payments(id),
 amount integer not null check (amount >= 0),
 approval_transaction_id text not null check (length(trim(approval_transaction_id)) > 0),
 approved_at timestamp not null,
 requested_at timestamp not null,
 transaction_id text unique,
 completed_at timestamp,
 constraint refund_receipt check (
   (transaction_id is null and completed_at is null) or
   (transaction_id is not null and length(trim(transaction_id)) > 0 and completed_at is not null)
 )
);
create index pending_refunds on refunds(requested_at,id) where completed_at is null;

create table subscription_withdrawals (
 subscription_id uuid primary key references subscriptions(id),
 requested_at timestamp not null,
 payment_id bigint references payments(id),
 decision varchar(30) not null check (decision in ('NO_PAYMENT','OUTSIDE_WINDOW','REFUND_REQUESTED','ALREADY_REFUNDED','REVIEW_REQUIRED','PREVIOUSLY_CANCELLED')),
 constraint withdrawal_payment check (decision not in ('OUTSIDE_WINDOW','REFUND_REQUESTED','ALREADY_REFUNDED') or payment_id is not null)
);
create index latest_successful_payment on payments(approved_at desc nulls first,id desc) where status='SUCCESS';
