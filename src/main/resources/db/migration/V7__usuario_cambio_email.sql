-- Email change from the profile: the new address waits here until its owner opens the
-- link mailed to it (token stored hashed, like the other account tokens).
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_pendiente VARCHAR(255);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS token_email_hash VARCHAR(64);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS token_email_expira TIMESTAMP;
