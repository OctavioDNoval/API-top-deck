-- Audit log: which record was touched (its uuid) and a short readable summary.
ALTER TABLE auditoria ADD COLUMN IF NOT EXISTS id_registro VARCHAR(36);
ALTER TABLE auditoria ADD COLUMN IF NOT EXISTS detalle     VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria(fecha_audit);
CREATE INDEX IF NOT EXISTS idx_auditoria_tabla ON auditoria(tabla);
