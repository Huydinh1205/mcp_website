/* ====================================================================
   DROP EVERYTHING in the current database.
   Run in: Azure portal -> MCP_Ecommerce -> Query editor (preview).
   WARNING: irreversible. Removes ALL tables/views/schemas in this DB,
   including any tables owned by other teammates.
   ==================================================================== */

DECLARE @sql NVARCHAR(MAX);

-- 1) drop every foreign key (so table drop order does not matter)
SET @sql = N'';
SELECT @sql += N'ALTER TABLE ' + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name)
             + N' DROP CONSTRAINT ' + QUOTENAME(f.name) + N';' + CHAR(10)
FROM sys.foreign_keys f
JOIN sys.tables  t ON f.parent_object_id = t.object_id
JOIN sys.schemas s ON t.schema_id = s.schema_id;
EXEC sp_executesql @sql;

-- 2) drop every view
SET @sql = N'';
SELECT @sql += N'DROP VIEW ' + QUOTENAME(s.name) + N'.' + QUOTENAME(v.name) + N';' + CHAR(10)
FROM sys.views v
JOIN sys.schemas s ON v.schema_id = s.schema_id;
EXEC sp_executesql @sql;

-- 3) drop every table
SET @sql = N'';
SELECT @sql += N'DROP TABLE ' + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name) + N';' + CHAR(10)
FROM sys.tables t
JOIN sys.schemas s ON t.schema_id = s.schema_id;
EXEC sp_executesql @sql;

-- 4) drop every user-created schema (keeps dbo / sys / guest / INFORMATION_SCHEMA)
SET @sql = N'';
SELECT @sql += N'DROP SCHEMA ' + QUOTENAME(name) + N';' + CHAR(10)
FROM sys.schemas
WHERE name NOT IN ('dbo','guest','sys','INFORMATION_SCHEMA')
  AND name NOT LIKE 'db[_]%';
EXEC sp_executesql @sql;

-- 5) verify empty (should return no rows)
SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES;
