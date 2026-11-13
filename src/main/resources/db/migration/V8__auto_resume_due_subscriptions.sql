create index auto_resume_due_subscriptions on subscriptions(id)
    where lifecycle_status = 'PAUSED' and remaining_paid_days > 0;
