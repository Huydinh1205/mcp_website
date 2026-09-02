-- Run once in the Azure portal: MCP_Ecommerce -> Query editor (preview).
-- Undoes the first (mis-targeted) Flyway run so the app can re-migrate cleanly
-- into its own `mcp` schema. Your teammate's own tables in `dbo` are NOT touched
-- (feedback / discounts / applied_discounts / anything else stays).

-- 1) inspect current state (optional — just to see what's there)
SELECT TABLE_SCHEMA, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_SCHEMA, TABLE_NAME;

-- 2) drop the half-applied migration bookkeeping + empty mcp schema
DROP TABLE  IF EXISTS mcp.flyway_schema_history;
DROP SCHEMA IF EXISTS mcp;

-- 3) drop the 9 tables the first run created UNQUALIFIED in dbo.
--    Flyway made them empty seconds ago; nothing of yours has these names
--    (V1 succeeded, which is only possible if they were free).
DROP TABLE IF EXISTS dbo.negotiation_rounds;
DROP TABLE IF EXISTS dbo.negotiations;
DROP TABLE IF EXISTS dbo.seller_ai_configs;
DROP TABLE IF EXISTS dbo.buyer_ai_configs;
DROP TABLE IF EXISTS dbo.[orders];
DROP TABLE IF EXISTS dbo.products;
DROP TABLE IF EXISTS dbo.sellers;
DROP TABLE IF EXISTS dbo.buyers;
DROP TABLE IF EXISTS dbo.[users];
