-- Addresses used by orders are hidden instead of deleted (or edited in place),
-- so past orders keep the address they were shipped to.
ALTER TABLE direccion ADD COLUMN IF NOT EXISTS activa BOOLEAN NOT NULL DEFAULT TRUE;
