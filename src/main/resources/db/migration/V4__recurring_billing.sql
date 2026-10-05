alter table billings add column purpose varchar(20) not null default 'REACTIVATION'
    check (purpose in ('REACTIVATION', 'RECURRING'));
alter table billings alter column purpose drop default;
alter table billings alter column expires_at drop not null;
alter table billings drop constraint billing_window;
alter table billings add constraint billing_window check (
    (purpose = 'REACTIVATION' and expires_at is not null and expires_at = created_at + interval '10 minutes')
    or (purpose = 'RECURRING' and expires_at is null and status <> 'EXPIRED')
);
create unique index one_recurring_billing_per_due_date
    on billings(subscription_id, billing_date) where purpose = 'RECURRING';
create index recurring_due_subscriptions on subscriptions(id)
    where lifecycle_status = 'ACTIVE' and cardinality(suspension_reasons) = 0;
