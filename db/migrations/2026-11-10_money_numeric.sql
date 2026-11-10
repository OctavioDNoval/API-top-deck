-- Money columns: DOUBLE PRECISION -> NUMERIC(12,2) (entities use BigDecimal).
-- Only needed for databases created before this change; new ones get it from
-- TopDeck-DB-Tables.sql. Safe to run more than once.
-- Local Docker:  docker exec -i topdeck-db psql -U postgres -d topdeck_database < db/migrations/2026-11-10_money_numeric.sql
BEGIN;
ALTER TABLE producto      ALTER COLUMN precio          TYPE NUMERIC(12,2) USING ROUND(precio::numeric, 2);
ALTER TABLE pedido        ALTER COLUMN total           TYPE NUMERIC(12,2) USING ROUND(total::numeric, 2);
ALTER TABLE detallepedido ALTER COLUMN precio_unitario TYPE NUMERIC(12,2) USING ROUND(precio_unitario::numeric, 2);
ALTER TABLE detallepedido ALTER COLUMN subtotal        TYPE NUMERIC(12,2) USING ROUND(subtotal::numeric, 2);
ALTER TABLE evento        ALTER COLUMN precio_entrada  TYPE NUMERIC(12,2) USING ROUND(precio_entrada::numeric, 2);
COMMIT;
