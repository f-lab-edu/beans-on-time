create table billings (
    id bigint primary key,
    customer_id bigint not null,
    subscription_id uuid not null references subscriptions(id),
    product_id bigint not null references products(id),
    amount integer not null check (amount >= 0),
    billing_date date not null,
    created_at timestamp not null,
    expires_at timestamp not null,
    status varchar(20) not null check (status in ('PENDING', 'PAID', 'EXPIRED')),
    constraint billing_window check (expires_at = created_at + interval '10 minutes')
);
create unique index one_pending_billing_per_subscription on billings(subscription_id)
    where status = 'PENDING';

create table payments (
    id bigint primary key,
    billing_id bigint not null references billings(id),
    amount integer not null check (amount >= 0),
    status varchar(20) not null check (status in ('PROCESSING', 'SUCCESS', 'FAILED')),
    transaction_id text,
    attempted_at timestamp not null,
    constraint payment_approval check (
      (status = 'SUCCESS' and transaction_id is not null and length(trim(transaction_id)) > 0)
      or (status <> 'SUCCESS' and transaction_id is null)
    )
);
create unique index one_processing_payment_per_billing on payments(billing_id)
    where status = 'PROCESSING';
create unique index one_successful_payment_per_billing on payments(billing_id)
    where status = 'SUCCESS';
create unique index unique_payment_transaction on payments(transaction_id)
    where transaction_id is not null;
create index payments_billing on payments(billing_id);
