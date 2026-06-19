-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- USD -> MZM exchange rate (SELV). Immutable rows; the row with the latest valid_from is the active rate.
CREATE TABLE IF NOT EXISTS fulfillment.exchange_rates (
    id          UUID PRIMARY KEY,
    rate        NUMERIC(12,6) NOT NULL CHECK (rate > 0),
    valid_from  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  UUID
);

CREATE INDEX IF NOT EXISTS ix_exchange_rates_valid_from
    ON fulfillment.exchange_rates (valid_from DESC);