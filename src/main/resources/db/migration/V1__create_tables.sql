CREATE TABLE assets (
    id          UUID          NOT NULL PRIMARY KEY,
    code        VARCHAR(10)   NOT NULL UNIQUE,
    name        VARCHAR(100)  NOT NULL,
    sector      VARCHAR(100),
    created_at  TIMESTAMP     NOT NULL
);

CREATE TABLE financial_statements (
    id              UUID           NOT NULL PRIMARY KEY,
    asset_id        UUID           NOT NULL REFERENCES assets(id),
    year            INTEGER        NOT NULL,
    period          VARCHAR(10)    NOT NULL,
    net_revenue     NUMERIC(18,2),
    gross_profit    NUMERIC(18,2),
    ebitda          NUMERIC(18,2),
    ebit            NUMERIC(18,2),
    net_income      NUMERIC(18,2),
    op_cash_flow    NUMERIC(18,2),
    free_cash_flow  NUMERIC(18,2),
    total_debt      NUMERIC(18,2),
    net_debt        NUMERIC(18,2),
    equity          NUMERIC(18,2),
    total_assets    NUMERIC(18,2),
    created_at      TIMESTAMP      NOT NULL,
    CONSTRAINT uq_asset_period UNIQUE (asset_id, year, period)
);
