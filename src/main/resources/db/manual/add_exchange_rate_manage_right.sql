-- EXCHANGE_RATE_MANAGE right checked by ExchangeRateController (SELV3-845).
-- Rights belong to referencedata, so this is NOT a Flyway migration of this module.
-- Apply once per environment, by hand, after deploying a version that has the exchange rate
-- endpoints. Prefer the API variant from README.md; this SQL is the equivalent for psql.
-- Idempotent: inserts nothing when the right already exists.
INSERT INTO referencedata.rights (id, description, name, type)
SELECT CAST('fb86b91f-e003-41f0-9037-08ed1a9489a9' AS UUID), NULL, 'EXCHANGE_RATE_MANAGE', 'GENERAL_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM referencedata.rights WHERE name = 'EXCHANGE_RATE_MANAGE');
