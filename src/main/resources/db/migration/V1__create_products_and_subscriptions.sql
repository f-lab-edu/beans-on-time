CREATE TABLE products (
    id bigint PRIMARY KEY CHECK (id > 0),
    seller_id bigint NOT NULL,
    name text NOT NULL,
    description text NOT NULL,
    base_price integer NOT NULL CHECK (base_price >= 0),
    supply_status varchar(32) NOT NULL
        CHECK (supply_status IN ('AVAILABLE', 'TEMPORARILY_UNAVAILABLE', 'DISCONTINUED'))
);

CREATE TABLE subscriptions (
    id uuid PRIMARY KEY,
    customer_id bigint NOT NULL,
    product_id bigint NOT NULL REFERENCES products(id),
    delivery_cycle_unit varchar(16) NOT NULL CHECK (delivery_cycle_unit IN ('ONE_WEEK', 'ONE_MONTH')),
    delivery_cycle_interval integer NOT NULL CHECK (delivery_cycle_interval > 0),
    started_date date NOT NULL,
    billing_anchor_day integer NOT NULL CHECK (billing_anchor_day BETWEEN 1 AND 31),
    current_period_start_date date,
    current_period_end_date date,
    remaining_paid_days integer,
    next_billing_date date,
    paused_at timestamp without time zone,
    scheduled_resume_date date,
    lifecycle_status varchar(16) NOT NULL CHECK (lifecycle_status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    suspension_reasons text[] NOT NULL DEFAULT '{}',
    CONSTRAINT valid_suspension_reasons CHECK (
        suspension_reasons <@ ARRAY['PRODUCT_UNAVAILABLE', 'PAYMENT_FAILED']::text[]
        AND array_position(suspension_reasons, NULL) IS NULL
    ),
    CONSTRAINT valid_subscription_period CHECK (
        (lifecycle_status = 'ACTIVE'
            AND current_period_start_date IS NOT NULL AND current_period_end_date IS NOT NULL
            AND current_period_start_date >= started_date
            AND current_period_end_date >= current_period_start_date
            AND remaining_paid_days IS NULL AND paused_at IS NULL AND scheduled_resume_date IS NULL
            AND next_billing_date IS NOT NULL AND current_period_end_date = next_billing_date - 1)
        OR (lifecycle_status = 'PAUSED'
            AND current_period_start_date IS NULL AND current_period_end_date IS NULL
            AND remaining_paid_days IS NOT NULL AND remaining_paid_days >= 0
            AND paused_at IS NOT NULL AND paused_at::date >= started_date
            AND scheduled_resume_date IS NOT NULL AND scheduled_resume_date > paused_at::date
            AND next_billing_date IS NOT NULL
            AND next_billing_date = scheduled_resume_date + remaining_paid_days)
        OR (lifecycle_status = 'CANCELLED'
            AND current_period_start_date IS NULL AND current_period_end_date IS NULL
            AND remaining_paid_days IS NULL AND paused_at IS NULL
            AND scheduled_resume_date IS NULL AND next_billing_date IS NULL)
    )
);

CREATE UNIQUE INDEX unique_open_subscription ON subscriptions (customer_id, product_id)
    WHERE lifecycle_status <> 'CANCELLED';
CREATE INDEX subscriptions_product_id ON subscriptions (product_id);
