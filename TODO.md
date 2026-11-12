# TODO — API TopDeck

Lista viva de lo que falta en el backend. Cada ítem tiene un ID para poder pedirlo directo
("hacé API-042", "hacé API-036 y FRONT-037"). Cómo funciona hoy el sistema: [docs/SISTEMA.md](docs/SISTEMA.md).

_Revisado el 11/11/2026: todos los ítems abiertos siguen vigentes y reflejan el código actual._

**Formato de cada ítem**
- **Prioridad**: 🔴 bloquea producción · 🟠 alta · 🟡 media · 🟢 baja / idea
- **Contexto**: por qué hace falta y qué pasa hoy
- **Qué hacer**: pasos concretos
- **Archivos**: dónde se toca
- **Listo cuando**: criterio de aceptación (cómo se prueba)
- **Depende de / relacionado**: otros ítems (incluye los `FRONT-xxx` del repo del front)

Al terminar uno: marcarlo `[x]`, dejar el hash del commit y moverlo a "Hecho" al final.

---

## 1. Antes de producción (pasos manuales, no son código)

### [ ] API-001 · Rotar `JWT_SECRET` y la contraseña de Postgres 🔴
- **Contexto**: los dos quedaron en el historial de git del repo (commit `429ace3`, `application.properties`). Si el repo es o fue público, hay que darlos por filtrados. El rol ya no sale del token (se lee de la base), pero con el secret alguien puede firmar tokens válidos para cualquier email.
- **Qué hacer**:
  1. Generar un secret nuevo: `openssl rand -base64 48` (mínimo 32 bytes; con menos, `Keys.hmacShaKeyFor` falla al arrancar).
  2. Cambiar la contraseña del usuario de Postgres.
  3. Cargar ambos solo en el `.env` del servidor (`JWT_SECRET`, `DB_PASSWORD`), nunca en un `.properties` commiteado.
  4. Reiniciar: todos los tokens existentes dejan de valer (es lo deseado).
- **Listo cuando**: un token emitido antes de rotar responde 401; el login nuevo funciona.
- **Relacionado**: API-018 (limpiar historial, opcional).

### [ ] API-002 · Configurar el SMTP 🔴
- **Contexto**: desde el registro con confirmación por mail, **sin SMTP nadie puede registrarse** (el registro se cancela si el mail falla). También dependen del mail: restablecer contraseña, reclamo de cuenta GUEST, aviso de "ya tenés cuenta", mail de pedido nuevo a los admins y pedido confirmado al cliente.
- **Qué hacer**:
  1. Crear una cuenta de envío (Gmail con contraseña de aplicación, o un proveedor transaccional: Resend, Brevo, SES).
  2. Cargar `SPRING_EMAIL` y `SPRING_EMAIL_PASSWORD` (y `spring.mail.host/port` si no es Gmail).
  3. Confirmar que `app.email.log-links` **no** está definido en producción (solo es para desarrollo: escribe en el log links que permiten tomar cuentas).
  4. Probar los 6 mails con una casilla real y revisar que no caigan en spam (configurar SPF/DKIM del dominio si se usa uno propio).
- **Archivos**: `.env` del servidor, `application-prod.properties`.
- **Listo cuando**: registrarse, confirmar, olvidar contraseña y hacer un pedido envían sus mails y los links llevan a `APP_FRONT_URL`.

### [ ] API-003 · Service account de Firebase 🔴
- **Contexto**: el panel sube imágenes con un custom token de Firebase que emite `/firebase/admin/token`. Sin la service account, ese endpoint responde 503 y los admins no pueden subir imágenes una vez publicadas las reglas.
- **Qué hacer**: crear la service account en la consola de Firebase, guardar el JSON en `secrets/firebase-service-account.json` (montado como secret en `docker-compose.prod.yml`) o apuntar `FIREBASE_CREDENTIALS_PATH`. Recién después publicar `storage.rules` (FRONT-001).
- **Listo cuando**: un admin logueado obtiene token (200) y puede subir una imagen; un usuario común recibe 403.
- **Relacionado**: FRONT-001 (FRONT-011 ya restringió los tipos de imagen).

### [ ] API-004 · Primer ADMIN de producción y revisión de admins 🔴
- **Contexto**: el seed con el ADMIN de hash público ya no corre en `prod` (solo en desarrollo, `db/seed`), así que la base de producción arranca sin usuarios. Además en la base local hay más de un ADMIN (por ejemplo `odnr2005@gmail.com`).
- **Qué hacer**: registrarse desde la tienda con el mail del dueño, confirmar el mail y después `UPDATE usuario SET rol = 'ADMIN' WHERE email = '...';` (cierra su sesión: volver a entrar). Más adelante, revisar la lista de ADMIN desde el panel.
- **Listo cuando**: en producción solo son ADMIN las cuentas que corresponden y ninguna usa el hash del repo.

### [ ] API-005 · Variables de entorno de producción 🔴
- **Qué revisar**: `SPRING_PROFILES_ACTIVE=prod`, `DB_*`, `JWT_SECRET`, `TCG_API_KEY`, `SPRING_EMAIL*`, `APP_FRONT_URL` (links de los mails), `APP_BASE_URL`, `APP_CONTACTO_TELEFONO/EMAIL/DIRECCION/HORARIO` (aparecen en los mails; hoy salen vacíos), `APP_CORS_ALLOWED_ORIGINS` (solo si front y API están en dominios distintos), `APP_TRUST_PROXY=true` solo si la API está detrás de nginx y no expone puertos.
- **Archivos**: `.env` del servidor, `docker-compose.prod.yml`, `.env.example` (mantenerlo al día).
- **Listo cuando**: el backend arranca con el perfil `prod` y los mails muestran los datos de contacto reales.

### [ ] API-006 · Base de datos de producción al día 🔴
- **Contexto**: el esquema lo crea Flyway al arrancar (API-041). Con una base nueva no hay que hacer nada; con una base que ya existía, Flyway la toma como versión 1 y le aplica de la V2 en adelante (hoy hasta la V5).
- **Qué hacer**:
  1. Backup antes de todo (si la base ya tenía datos).
  2. Levantar la API con el perfil `prod` y revisar en el log que Flyway aplicó las migraciones y que la API arrancó (`ddl-auto=validate` no arranca si falta algo).
  3. Crear el ADMIN a mano (el perfil `prod` no carga el seed).
- **Relacionado**: API-004.

### [ ] API-007 · HTTPS 🔴
- **Contexto**: sin HTTPS el token viaja en claro. Se resuelve en el proxy (nginx del front o un balanceador).
- **Relacionado**: FRONT-003 (certificado y HSTS en nginx).

---

## 2. Seguridad (pendiente de la auditoría)

### [ ] API-014 · Pedidos de invitado con mail o teléfono ajeno 🟡 (decisión de negocio)
- **Contexto**: cualquiera puede hacer un pedido como invitado con cualquier mail. Límite actual: 5 pedidos cada 10 minutos por IP. Riesgo: pedidos falsos y, si se confirma uno, le llega un mail a un tercero. Lo mitiga que el pago y la entrega se coordinan por WhatsApp.
- **Opciones**:
  - **A.** Sumar un límite por email (ej. 3 pedidos pendientes por mail) — sin fricción para el cliente.
  - **B.** Pedir que el invitado confirme su mail antes de registrar el pedido (link o código) — más seguro, agrega un paso.
  - **C.** No mandar mails al cliente hasta que el admin confirme por WhatsApp (ya es así: el mail de "pedido confirmado" sale al confirmar).
- **Qué hacer**: elegir opción y avisar. Con A: contador en `PedidoService.guardarPedidoEfimero` por `usuario.email` con estado PENDIENTE.
- **Relacionado**: FRONT-031.

### [ ] API-015 · Bloqueo de login y rate limit en memoria 🟢
- **Contexto**: `LoginAttemptService` y `RateLimitFilter` guardan su estado en memoria. Con una sola instancia está bien; un reinicio los limpia. Si se escala a varias instancias, cada una cuenta por separado.
- **Qué hacer (solo si se escala)**: mover el estado a Redis (o a la base con expiración).

### [ ] API-016 · Sesiones por dispositivo 🟢 (idea)
- **Contexto**: hoy cerrar sesión sube `sesion_version` y cierra **todos** los dispositivos del usuario. Es lo más seguro, pero puede molestar.
- **Qué hacer (si se quiere)**: tabla `sesion` (id, usuario, dispositivo, creada, revocada) con un `sid` en el JWT; logout revoca solo ese `sid`; agregar "cerrar todas las sesiones" en el perfil.

### [ ] API-017 · Token fuera de `localStorage` 🟢 (evaluar)
- **Contexto**: el JWT vive en `localStorage`, así que un XSS podría robarlo. Hoy no hay sinks de XSS conocidos y la defensa principal es la CSP del nginx (FRONT-010, ya hecha).
- **Qué hacer (si se decide)**: cookie `HttpOnly; Secure; SameSite=Strict` emitida por la API, protección CSRF y cambios en todos los `fetch` del front (`credentials: "include"`). Es un cambio grande en los dos repos.
- **Relacionado**: FRONT-013 (API-013 ya sacó `allowCredentials` de CORS: con cookies habría que volver a habilitarlo).

### [ ] API-018 · Limpiar el historial de git 🟢 (opcional)
- **Contexto**: con `git filter-repo` se pueden sacar los secrets viejos del historial (requiere force-push y que todos re-clonen). No reemplaza rotarlos (API-001).

### [ ] API-019 · `telefono` UNIQUE en `usuario` 🟡
- **Contexto**: la columna es única. Dos clientes con el mismo teléfono (familia, un local) no pueden guardarlo en su perfil. El pedido igual guarda el teléfono en `pedido.telefono_contacto`, y el perfil solo se completa si nadie más lo tiene.
- **Qué hacer**: decidir si se saca el `UNIQUE` (migración `ALTER TABLE usuario DROP CONSTRAINT ...`) y simplificar `PedidoService.guardarTelefonoSiFalta`.

---

## 3. Funcionalidad nueva

### [ ] API-034 · Pago online (MercadoPago) 🟢 (futuro)
- **Contexto**: hoy el medio de pago se coordina por WhatsApp. Integrar Checkout Pro de MercadoPago permitiría cobrar al confirmar.
- **Qué hacer (cuando se decida)**: preferencia de pago al crear el pedido, webhook de notificación que pase el pedido a pagado, estados nuevos en `ESTADO_PEDIDO`, credenciales como secret.

### [ ] API-036 · Cambio de email desde el perfil 🟡
- **Contexto**: el cliente puede editar nombre, teléfono y contraseña, pero no su email (API-032 lo dejó afuera a propósito): cambiarlo sin confirmar permitiría que alguien con la sesión abierta se quede con la cuenta.
- **Qué hacer**: `POST /user/me/email` `{ nuevoEmail, password }` → verifica la contraseña, guarda el pedido de cambio con un token (SHA-256, 24 h) y manda un link al **mail nuevo**; al abrirlo se aplica el cambio, se cierran las sesiones y se avisa al mail viejo. Rechazar un mail que ya tenga cuenta (sin revelar cuál). Columnas nuevas por migración (`email_pendiente`, `token_email_hash`, `token_email_expira`) y mensaje de auditoría sin los mails.
- **Relacionado**: FRONT-037.

### [ ] API-035 · Costo de envío 🟢 (futuro)
- **Contexto**: hoy el envío "se coordina". Si se quiere mostrar el costo antes de confirmar: tabla de tarifas por provincia/CP o integración con un correo (Andreani, Correo Argentino), campo `costo_envio` en `pedido` sumado al total.

---

## 4. Calidad y técnico

### [ ] API-040 · Wrapper de Maven 🟢
- **Contexto**: falta la carpeta `.mvn/wrapper/` en el repo, así que `./mvnw` no funciona; hoy se usa un Maven instalado a mano.
- **Qué hacer**: `mvn -N wrapper:wrapper` y commitear `.mvn/` + `mvnw` + `mvnw.cmd`. Revisar que el Dockerfile use el wrapper o la imagen de Maven.

### [ ] API-042 · Tests de integración y de seguridad 🟡
- **Contexto**: hay 79 tests unitarios (sin base, con Mockito). No hay tests de controllers, de las reglas de seguridad por URL ni de las migraciones contra un Postgres real.
- **Qué hacer**: Testcontainers con Postgres + `@SpringBootTest` + MockMvc. Casos mínimos:
  - usuario B no puede tocar carrito/direcciones/pedidos de A (403);
  - endpoints `/admin/**` → 403 para USER, 401 sin token;
  - flujo completo de registro → confirmar → login → logout → token revocado;
  - pedido de invitado con envío y con retiro.
- **Listo cuando**: corre en CI (ver API-044) en menos de 2 minutos.

### [ ] API-043 · Limpieza de código 🟢
- `CustomUserDetailsService`: ya no lo usa nadie (el filtro lee el usuario del repo). Borrarlo o dejarlo solo si se usa el `AuthenticationManager`.
- `CarritoController`: ~27 líneas comentadas de los endpoints del carrito efímero (ya vive en el front). Borrarlas junto con los métodos comentados de `CarritoService`.
- `HELP.md` es el archivo por defecto de Spring Initializr: borrarlo.
- `Enumerados/STATUS.java`: enum vacío, no lo usa nadie. Borrarlo.
- `Security/JpaConfig.java` + `Security/AuditorAwareIMPL.java`: habilitan la auditoría de JPA (`@CreatedBy`), pero ninguna entidad la usa (la auditoría real es `AuditService`). Borrarlos.
- `Carrito.sessionId` / `carrito.session_id`: legado del carrito de invitados en la API; hoy vive en el front. Se puede quitar con una migración.
- Comentarios en español/inglés mezclados: los nuevos van en inglés (regla del proyecto).

### [ ] API-044 · CI en GitHub Actions 🟢
- **Qué hacer**: workflow que en cada push/PR corra `mvn test` (con JDK 21 y `-Duser.timezone=America/Argentina/Buenos_Aires`) y, opcional, construya la imagen Docker.

---

## 5. Ideas / por definir

> Agregar acá lo que vaya surgiendo, aunque sea una línea. Después se le da formato de ítem.

-

---

## Hecho (referencia)

- Auditoría inicial (nov-2026): rol leído de la base, reclamo de GUEST por mail, Firebase con custom tokens, rate limit con IP confiable, perfil `prod`, IP de TyC en el servidor, login sin enumeración, PATCH parcial de usuarios, stock repuesto al borrar pedidos confirmados, montos en `BigDecimal`, proxy TCG validado, 30 tests.
- Estadísticas: stock bajo solo para sellados (`f2553cc`).
- Carrito: stock en el detalle (`63fd890`).
- Pedidos: forma de entrega y teléfono de contacto (`a630d66`).
- Cuentas y sesiones (`e5ca4df`): registro con confirmación por mail, sesiones revocables con tope de 7 días, logout real, olvidé mi contraseña, bloqueo por intentos, contraseñas de 8 a 72.
- Tanda 1 de seguridad (`58119aa`, `b5836ad`): API-010 un admin no puede borrarse ni cambiarse el rol, y no se puede borrar ni bajar al único ADMIN (409, `UsuarioServiceTest`); API-011 se eliminó `/auth/validate/start`; API-012 se eliminó `/products/test`; API-013 CORS sin `allowCredentials`. Además: rutas inexistentes devuelven 404 en vez de 500.
- Baja de usuarios (`72b1e6f`): API-020 borrar un usuario elimina su carrito y direcciones; si tiene pedidos, la cuenta se anonimiza (nombre, mail `@topdeck.invalid`, sin teléfono ni contraseña, GUEST, `fecha_baja`) y se oculta de la lista y las estadísticas, conservando los pedidos y las direcciones que usan. `DELETE /user/admin/delete/{id}` responde `{resultado, message}`; `UsuarioResponse` expone `emailConfirmado`.
- Migraciones con Flyway (`ca703ba`): API-041 `V1__initial_schema`, `V2__accounts_and_orders_columns` (idempotente, reemplaza `db/migrations/`), seed solo de desarrollo en `db/seed`, `ddl-auto=validate` y `baseline-on-migrate`. Probado con la base existente y con bases vacías en dev y prod. Se eliminó `TopDeck-DB-Tables.sql`.
- Perfil del cliente (`c2c8749`): API-030 editar, borrar y marcar principal las direcciones propias (403 con las de otro; si un pedido la usa, se oculta o se guarda como nueva, `V3__direccion_activa`); la primera dirección queda como principal. API-032 `PATCH /user/me` (nombre y teléfono) y `POST /user/me/password` (verifica la actual con el mismo bloqueo del login, cierra las otras sesiones y devuelve un token nuevo). El cambio de email queda pendiente.
- Eventos y auditoría (`6893169`): API-031 `PATCH /eventos/admin/edit/{id}` parcial; el estado se recalcula al cambiar la fecha y al crear (antes un evento de hoy quedaba "próximo"). API-033 cada log guarda el uuid del registro y un resumen legible (`V4__auditoria_registro`; en usuarios y direcciones describe la acción, sin datos personales); filtros `accion`, `tabla`, `usuario`, `desde`, `hasta`; retención configurable con `app.audit.dias-retencion` (90 por defecto). Mensajes armados en `util/AuditMensaje` para productos, pedidos, eventos, categorías, tags, usuarios y direcciones: altas con los datos clave ("Creación del producto \"X\" ($8.500,00) · stock 50 · ..."), ediciones con antes → después de cada campo cambiado, cambios de estado de pedidos con el efecto en el stock; en usuarios y direcciones solo se nombran los campos, sin sus valores (`V5` amplía `detalle` a 500). Para abrir el registro desde los logs: `GET /pedidos/admin/{id}` y `GET /products/admin/{id}` (incluye productos ocultos).
- Documentación del sistema (11/11/2026): API-045 `docs/SISTEMA.md` explica cómo funciona hoy todo el backend (negocio, cuentas, sesiones, catálogo, pedidos, direcciones, eventos, auditoría, imágenes, mails, estadísticas, migraciones, configuración, tests y cómo extenderlo); el README quedó como referencia (endpoints, variables, ejecución) y se corrigieron sus partes desactualizadas (modelo de datos, CORS, mails, tests).
