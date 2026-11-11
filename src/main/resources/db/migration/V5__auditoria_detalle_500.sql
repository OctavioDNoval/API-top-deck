-- Edit messages list every changed field ("precio $1 → $2, stock 5 → 3"): room for 500 characters.
ALTER TABLE auditoria ALTER COLUMN detalle TYPE VARCHAR(500);
