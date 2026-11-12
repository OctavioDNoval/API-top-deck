-- Guest carts live in the browser (localStorage) since the ephemeral cart endpoints were removed:
-- carrito.session_id is never written or read any more.
ALTER TABLE carrito DROP COLUMN IF EXISTS session_id;
