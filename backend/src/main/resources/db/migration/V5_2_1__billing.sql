-- Part 5.2: plans, subscriptions and payments (Stripe/fake provider).

CREATE TABLE plans (
    id                 char(36)     NOT NULL DEFAULT (UUID()),
    name               varchar(100) NOT NULL,
    monthly_screenings int          NOT NULL,
    price_bdt          decimal(10,2) NOT NULL,
    duration_days      int          NULL,
    active             boolean      NOT NULL DEFAULT true,
    created_at         datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at         datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_plans_name (name),
    CONSTRAINT chk_plans_monthly CHECK (monthly_screenings >= 0),
    CONSTRAINT chk_plans_price CHECK (price_bdt >= 0),
    CONSTRAINT chk_plans_duration CHECK (duration_days IS NULL OR duration_days BETWEEN 1 AND 365)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE company_subscriptions (
    id         char(36)    NOT NULL DEFAULT (UUID()),
    company_id char(36)    NOT NULL,
    plan_id    char(36)    NOT NULL,
    starts_at  datetime(3) NOT NULL,
    ends_at    datetime(3) NOT NULL,
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_subscriptions_company_ends (company_id, ends_at DESC),
    CONSTRAINT fk_subscriptions_company FOREIGN KEY (company_id)
        REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT chk_subscriptions_window CHECK (ends_at > starts_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE payments (
    id                  char(36)      NOT NULL DEFAULT (UUID()),
    company_id          char(36)      NOT NULL,
    plan_id             char(36)      NOT NULL,
    amount_bdt          decimal(10,2) NOT NULL,
    provider            varchar(20)   NOT NULL,
    provider_payment_id varchar(255)  NULL,
    merchant_invoice    varchar(255)  NOT NULL,
    status              varchar(20)   NOT NULL DEFAULT 'initiated',
    completed_at        datetime(3)   NULL,
    created_at          datetime(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_payments_provider_payment (provider_payment_id),
    UNIQUE KEY uq_payments_merchant_invoice (merchant_invoice),
    KEY idx_payments_company_created (company_id, created_at DESC),
    CONSTRAINT fk_payments_company FOREIGN KEY (company_id)
        REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT chk_payments_provider CHECK (provider IN ('fake', 'stripe')),
    CONSTRAINT chk_payments_status CHECK (status IN ('initiated', 'completed', 'failed', 'cancelled')),
    CONSTRAINT chk_payments_amount CHECK (amount_bdt >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

INSERT INTO plans (name, monthly_screenings, price_bdt, duration_days) VALUES
    ('Free', 20, 0.00, NULL),
    ('Standard', 200, 1500.00, 30),
    ('Pro', 1000, 5000.00, 30);
