alter table payments drop constraint payments_status_check;
alter table payments add constraint payments_status_check check (status in ('PROCESSING','SUCCESS','FAILED','CANCEL_PENDING','CANCELLED','REVIEW'));
alter table payments drop constraint payment_approval;
alter table payments add constraint payment_approval check (
  (status in ('SUCCESS','CANCEL_PENDING','CANCELLED','REVIEW') and transaction_id is not null and length(trim(transaction_id)) > 0)
  or (status in ('PROCESSING','FAILED') and transaction_id is null)
);
alter table payments drop constraint successful_approval_time;
alter table payments add constraint successful_approval_time check (
  (status = 'REVIEW' and approved_at is not null) or
  (status <> 'REVIEW' and (approved_at is null or status = 'SUCCESS'))
);
drop index one_processing_payment_per_billing;
create unique index one_processing_payment_per_billing on payments(billing_id)
  where status in ('PROCESSING','CANCEL_PENDING','REVIEW');
