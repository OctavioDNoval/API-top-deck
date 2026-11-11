# TODO — API TopDeck

Lista viva de lo que falta en el backend. Cada ítem tiene un ID para poder pedirlo directo
("hacé API-010", "hacé API-030 y FRONT-030").

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
- **Relacionado**: FRONT-001, FRONT-011.

### [ ] API-004 · Contraseña del admin del seed y revisión de admins 🔴
- **Contexto**: `TopDeck-DB-Tables.sql` crea un ADMIN con el hash de su contraseña público en el repo. Además hoy hay más de un ADMIN en la base local (por ejemplo `odnr2005@gmail.com`); conviene revisar quién es admin en la base de producción.
- **Qué hacer**: cambiar esa contraseña apenas se cree la base de producción (o crear el admin a mano y sacar el insert del seed). Listar los ADMIN y dejar solo los que correspondan.
- **Listo cuando**: el hash del repo no coincide con ninguna cuenta real.

### [ ] API-005 · Variables de entorno de producción 🔴
- **Qué revisar**: `SPRING_PROFILES_ACTIVE=prod`, `DB_*`, `JWT_SECRET`, `TCG_API_KEY`, `SPRING_EMAIL*`, `APP_FRONT_URL` (links de los mails), `APP_BASE_URL`, `APP_CONTACTO_TELEFONO/EMAIL/DIRECCION/HORARIO` (aparecen en los mails; hoy salen vacíos), `APP_CORS_ALLOWED_ORIGINS` (solo si front y API están en dominios distintos), `APP_TRUST_PROXY=true` solo si la API está detrás de nginx y no expone puertos.
- **Archivos**: `.env` del servidor, `docker-compose.prod.yml`, `.env.example` (mantenerlo al día).
- **Listo cuando**: el backend arranca con el perfil `prod` y los mails muestran los datos de contacto reales.

### [ ] API-006 · Base de datos de producción al día 🔴
- **Contexto**: `ddl-auto=update` crea columnas nuevas pero **no cambia tipos**.
- **Qué hacer**:
  1. Aplicar `db/migrations/2026-11-10_money_numeric.sql` (montos `Double` → `NUMERIC(12,2)`) **antes** de levantar la versión nueva.
  2. Verificar que se crearon las columnas nuevas: `usuario.email_verificado`, `usuario.sesion_version`, `usuario.token_reset_hash`, `usuario.token_reset_expira`, `pedido.tipo_entrega`, `pedido.telefono_contacto`.
  3. Backup antes de todo.
- **Relacionado**: API-041 (pasar a Flyway).

### [ ] API-007 · HTTPS 🔴
- **Contexto**: sin HTTPS el token viaja en claro. Se resuelve en el proxy (nginx del front o un balanceador).
- **Relacionado**: FRONT-003 (certificado y HSTS en nginx).

---

## 2. Seguridad (pendiente de la auditoría)

### [ ] API-010 · Que un admin no pueda dejar el sistema sin admins 🟠
- **Contexto**: hoy un admin puede borrarse a sí mismo, sacarse el rol, o sacárselo al último admin, y el panel queda sin dueño (solo se arregla tocando la base).
- **Qué hacer**: en `UsuarioService.actualizarUsuario` y `deleteUsuario`:
  - rechazar (409) borrar al usuario autenticado;
  - rechazar bajar de ADMIN a otro rol si es el único ADMIN (`usuarioRepo.countByRol(ROL.ADMIN) == 1`);
  - opcional: no permitir que un admin se cambie su propio rol.
- **Archivos**: `service/IMPL/UsuarioService.java`, `Repository/IUsuarioRepo.java`, tests en `UsuarioServiceTest` (nuevo).
- **Listo cuando**: los 3 casos devuelven 409 con un mensaje claro y hay tests.
- **Relacionado**: FRONT-020 (la pantalla de editar usuarios tiene que mostrar ese error).

### [ ] API-011 · Simplificar `/auth/validate/start` 🟡
- **Contexto**: devuelve el mismo token que recibe. El front lo usa al cargar para "validar" y después llama a `/user/me`. Ya no filtra el texto del error, pero es redundante: `/user/me` ya valida todo (firma, versión de sesión, mail confirmado, tope de 7 días).
- **Qué hacer**: eliminar el endpoint y que el front valide directamente con `/user/me` (si da 401, logout). Hacerlo en conjunto con FRONT-012.
- **Listo cuando**: el arranque del front funciona igual sin ese endpoint.

### [ ] API-012 · Borrar `/products/test` 🟢
- **Contexto**: endpoint de prueba que quedó; responde a cualquier usuario autenticado.
- **Archivos**: `controller/ProductoController.java`, README (tabla de endpoints).

### [ ] API-013 · CORS sin `allowCredentials` 🟢
- **Contexto**: la autenticación va por header `Authorization`, no por cookies, así que no hace falta. Si algún día se pasa a cookies (API-017), revisarlo junto con CSRF.
- **Archivos**: `Security/SecurityConfig.java` (`corsConfigurationSource`).

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
- **Contexto**: el JWT vive en `localStorage`, así que un XSS podría robarlo. Hoy no hay sinks de XSS conocidos y la defensa principal es la CSP (FRONT-010).
- **Qué hacer (si se decide)**: cookie `HttpOnly; Secure; SameSite=Strict` emitida por la API, protección CSRF y cambios en todos los `fetch` del front (`credentials: "include"`). Es un cambio grande en los dos repos.
- **Relacionado**: FRONT-013, API-013.

### [ ] API-018 · Limpiar el historial de git 🟢 (opcional)
- **Contexto**: con `git filter-repo` se pueden sacar los secrets viejos del historial (requiere force-push y que todos re-clonen). No reemplaza rotarlos (API-001).

### [ ] API-019 · `telefono` UNIQUE en `usuario` 🟡
- **Contexto**: la columna es única. Dos clientes con el mismo teléfono (familia, un local) no pueden guardarlo en su perfil. El pedido igual guarda el teléfono en `pedido.telefono_contacto`, y el perfil solo se completa si nadie más lo tiene.
- **Qué hacer**: decidir si se saca el `UNIQUE` (migración `ALTER TABLE usuario DROP CONSTRAINT ...`) y simplificar `PedidoService.guardarTelefonoSiFalta`.

---

## 3. Funcionalidad nueva

### [ ] API-030 · Direcciones del usuario: editar y borrar 🟡
- **Contexto**: el usuario solo puede crear y listar direcciones. `DireccionService.update` y `delete` existen pero no tienen endpoint, y **`delete` no verifica que la dirección sea del usuario** (hay que agregarlo antes de exponerlo).
- **Qué hacer**:
  - `PATCH /direccion/user/{id}` → `update` (ya chequea dueño);
  - `DELETE /direccion/user/{id}` → agregar chequeo de dueño (403 si no es suya);
  - `PATCH /direccion/user/{id}/principal` para marcar la principal (desmarca las demás);
  - borrar una dirección usada en pedidos: los pedidos viejos guardan la referencia, así que hacer borrado lógico (`activa=false`) o desvincular.
- **Archivos**: `controller/DireccionController.java`, `service/IMPL/DireccionService.java`, `model/Direccion.java`.
- **Listo cuando**: un usuario edita/borra las suyas y recibe 403 con las de otro (test).
- **Relacionado**: FRONT-030.

### [ ] API-031 · Editar eventos 🟡
- **Contexto**: los eventos se crean y se borran, pero no se editan.
- **Qué hacer**: `PATCH /eventos/admin/edit/{id}` con DTO parcial (nombre, ubicación, fecha, hora, precio). El estado lo sigue calculando el job diario (`EventoService.actualizarEstadoEventoAutomaticamente`); al cambiar la fecha, recalcularlo en el momento.
- **Archivos**: `controller/EventosController.java`, `service/IMPL/EventoService.java`, DTO nuevo `EventoUpdateRequest`.
- **Relacionado**: FRONT-021.

### [ ] API-032 · Perfil autogestionado: datos y cambio de contraseña 🟡
- **Contexto**: el usuario no puede editar su nombre o teléfono ni cambiar su contraseña desde la cuenta (solo con "olvidé mi contraseña").
- **Qué hacer**:
  - `PATCH /user/me` → nombre y teléfono (validar teléfono igual que en el checkout; respetar API-019);
  - `POST /user/me/password` `{ actual, nueva }` → verificar la actual, mínimo 8, `invalidarSesiones()` y devolver un token nuevo para el dispositivo actual;
  - cambio de email: solo con confirmación al mail nuevo (dejar para después).
- **Archivos**: `controller/UsuarioController.java`, `service/IMPL/UsuarioService.java`, DTOs nuevos.
- **Listo cuando**: cambiar la contraseña cierra las otras sesiones y deja la actual abierta (test).
- **Relacionado**: FRONT-030.

### [ ] API-033 · Auditoría útil: registro afectado y filtros 🟡
- **Contexto**: el log de auditoría guarda acción, tabla y usuario, pero no **qué registro** se tocó, y solo se filtra por acción. Las acciones de invitados y del sistema quedan como `SISTEMA`.
- **Qué hacer**:
  - agregar `id_registro` (uuid) y opcionalmente un resumen (`detalle`) a `Audit`; pasar el uuid en cada `auditService.registrar(...)`;
  - `GET /audit/admin/obtenerPaginados` con filtros `usuario`, `desde`, `hasta`, `tabla`;
  - recordar que `AuditService` purga los logs de más de 90 días (03:30) — dejarlo configurable.
- **Relacionado**: FRONT-022.

### [ ] API-034 · Pago online (MercadoPago) 🟢 (futuro)
- **Contexto**: hoy el medio de pago se coordina por WhatsApp. Integrar Checkout Pro de MercadoPago permitiría cobrar al confirmar.
- **Qué hacer (cuando se decida)**: preferencia de pago al crear el pedido, webhook de notificación que pase el pedido a pagado, estados nuevos en `ESTADO_PEDIDO`, credenciales como secret.

### [ ] API-035 · Costo de envío 🟢 (futuro)
- **Contexto**: hoy el envío "se coordina". Si se quiere mostrar el costo antes de confirmar: tabla de tarifas por provincia/CP o integración con un correo (Andreani, Correo Argentino), campo `costo_envio` en `pedido` sumado al total.

---

## 4. Calidad y técnico

### [ ] API-040 · Wrapper de Maven 🟢
- **Contexto**: falta la carpeta `.mvn/wrapper/` en el repo, así que `./mvnw` no funciona; hoy se usa un Maven instalado a mano.
- **Qué hacer**: `mvn -N wrapper:wrapper` y commitear `.mvn/` + `mvnw` + `mvnw.cmd`. Revisar que el Dockerfile use el wrapper o la imagen de Maven.

### [ ] API-041 · Migraciones versionadas (Flyway) 🟡
- **Contexto**: el esquema lo maneja `ddl-auto=update` (también en producción) más scripts sueltos en `db/migrations/`. `update` no cambia tipos, no borra columnas y no deja historial.
- **Qué hacer**: agregar Flyway, generar `V1__baseline.sql` desde el esquema actual (`baselineOnMigrate=true`), pasar los scripts de `db/migrations/` a `V2...`, y poner `ddl-auto=validate` en prod.
- **Relacionado**: API-006.

### [ ] API-042 · Tests de integración y de seguridad 🟡
- **Contexto**: hay 46 tests unitarios (sin base). No hay tests de controllers ni de las reglas de seguridad por URL.
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
- Comentarios en español/inglés mezclados: los nuevos van en inglés (regla del proyecto).

### [ ] API-044 · CI en GitHub Actions 🟢
- **Qué hacer**: workflow que en cada push/PR corra `mvn test` (con JDK 21 y `-Duser.timezone=America/Argentina/Buenos_Aires`) y, opcional, construya la imagen Docker.

### [ ] API-045 · README al día 🟢
- **Falta documentar**: forma de entrega (`tipoEntrega` ENVIO/RETIRO), `telefonoContacto` obligatorio en los pedidos, y que la dirección solo se exige con envío; las variables `app.email.log-links` y `APP_FRONT_URL` en la tabla de variables.
- **Archivos**: `README.md`.

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
