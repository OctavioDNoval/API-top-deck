-- Columns added after the initial schema (until now they were created by
-- ddl-auto=update), plus the money type fix from db/migrations/2026-11-10.
-- Idempotent: databases that already have them are left as they are.

-- Accounts and sessions: email confirmation, revocable sessions, password reset.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_verificado   BOOLEAN;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS sesion_version     INTEGER;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS token_reset_hash   VARCHAR(64);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS token_reset_expira TIMESTAMP;
-- Deleted users that had orders are anonymized and hidden instead of removed.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS fecha_baja         TIMESTAMP;

-- Orders: delivery type and contact phone.
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS tipo_entrega      VARCHAR(10);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS telefono_contacto VARCHAR(30);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pedido_tipo_entrega_check') THEN
        ALTER TABLE pedido ADD CONSTRAINT pedido_tipo_entrega_check
            CHECK (tipo_entrega IN ('ENVIO', 'RETIRO'));
    END IF;
END $$;

-- Money columns as NUMERIC(12,2) (entities use BigDecimal). No-op where already numeric.
ALTER TABLE producto      ALTER COLUMN precio          TYPE NUMERIC(12,2) USING ROUND(precio::numeric, 2);
ALTER TABLE pedido        ALTER COLUMN total           TYPE NUMERIC(12,2) USING ROUND(total::numeric, 2);
ALTER TABLE detallepedido ALTER COLUMN precio_unitario TYPE NUMERIC(12,2) USING ROUND(precio_unitario::numeric, 2);
ALTER TABLE detallepedido ALTER COLUMN subtotal        TYPE NUMERIC(12,2) USING ROUND(subtotal::numeric, 2);
ALTER TABLE evento        ALTER COLUMN precio_entrada  TYPE NUMERIC(12,2) USING ROUND(precio_entrada::numeric, 2);
