CREATE TABLE CustomerFinancials (
    customer_id NUMBER,
    sales_rep_id NUMBER,
    revenue_tier NUMBER,
    total_ltv NUMBER,
    monthly_subscription NUMBER,
    overdue_count NUMBER,
    avg_payment_delay_days NUMBER
);

CREATE TABLE sales_reps (
    rep_id NUMBER,
    rep_name VARCHAR2(100),
    job_title VARCHAR2(100),
    hire_date DATE,
    region_id NUMBER
);

CREATE TABLE regions (
    region_id NUMBER,
    region_name VARCHAR2(100),
    country_code VARCHAR2(10)
);

CREATE TABLE ChurnSignals (
    customer_id NUMBER,
    negative_trend_months NUMBER
);

DROP TABLE IF EXISTS ENTERPRISES;
DROP TABLE IF EXISTS SUBSCRIPTIONS;
DROP TABLE IF EXISTS INVOICES;
DROP TABLE IF EXISTS USAGE_LOGS;

CREATE TABLE ENTERPRISES (
    enterprise_id   BIGINT PRIMARY KEY,
    enterprise_name VARCHAR(100),
    industry_code   VARCHAR(50)
);

CREATE TABLE SUBSCRIPTIONS (
    subscription_id BIGINT PRIMARY KEY,
    enterprise_id   BIGINT NOT NULL
);

CREATE TABLE INVOICES (
    invoice_id      BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    amount          DECIMAL(15,2)
);

CREATE TABLE USAGE_LOGS (
    log_id          BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    active_users    INT,
    api_calls       BIGINT
);


