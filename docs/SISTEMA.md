# TopDeck API — Cómo funciona el sistema

Este documento explica **cómo está armado y cómo funciona hoy** el backend de TopDeck: qué hace cada parte, qué reglas
de negocio aplica y por qué. Para la referencia rápida (lista de endpoints, variables de entorno, cómo ejecutar) está el
[README](../README.md). Lo pendiente está en [TODO.md](../TODO.md). El front tiene su propio documento en
`Front-Top-Deck/docs/SISTEMA.md`.

---

## 1. Qué es TopDeck

Tienda online de cartas coleccionables (TCG) importadas. Vende dos tipos de productos:

- **Sellados** (`tipoProducto = PRODUCTO`): sobres, cajas, barajas, accesorios, figuras. Tienen categoría y franquicia.
- **Singles** (`tipoProducto = SINGLE`): cartas sueltas, con atributos libres (set, rareza, color, tipo...) que se
  importan desde una API externa de TCG y se filtran por facetas.

Además publica **eventos** (torneos, juntadas) con fecha, lugar y precio de entrada.

Reglas de negocio que atraviesan todo el sistema:

- **No hay pago online.** El pedido se registra en el sistema y el pago y el costo de envío se coordinan por
  **WhatsApp** con la tienda.
- **Entrega**: `ENVIO` (a domicilio, requiere dirección) o `RETIRO` (en el local). El **teléfono de contacto es obligatorio**
  en todo pedido.
- **Se puede comprar sin cuenta** (como invitado). El sistema crea un usuario `GUEST` interno para colgarle los pedidos.
- **El stock se descuenta al confirmar** el pedido (no al crearlo). La tienda muestra el stock exacto solo cuando quedan
  5 unidades o menos ("¡Últimas N unidades!").

---

## 2. Piezas del sistema

```
                 ┌──────────────────────── Navegador ────────────────────────┐
                 │  Front React (Vite)  ── imágenes ──►  Firebase Storage      │
                 │        │  (los admins suben con un custom token de Firebase) │
                 └────────┼───────────────────────────────────────────────────┘
                          │ HTTPS / JSON (Authorization: Bearer <JWT>)
                          ▼
     ┌──────────────── API Spring Boot (este repo) ────────────────┐
     │  filtros de seguridad → controllers → services → repos      │──► PostgreSQL 16
     │                                                              │──► Gmail SMTP (mails)
     │                                                              │──► api.apitcg.com (cartas, solo admin)
     │                                                              │──► Firebase Admin SDK (custom tokens)
     └──────────────────────────────────────────────────────────────┘
```

| Pieza | Para qué |
|---|---|
| **PostgreSQL 16** | Todos los datos del negocio. El esquema lo maneja **Flyway** (sección 13). |
| **Firebase Storage** | Imágenes de productos, tags, carrusel y cartas. El front sube directo; la API solo emite el token que habilita a los admins (sección 10). |
| **Gmail SMTP** | Mails de cuenta (confirmación, reset, activación) y de pedidos (sección 11). **Sin SMTP nadie puede registrarse.** |
| **api.apitcg.com** | Búsqueda de cartas para importar singles. La API hace de proxy (la key no sale al front). |
| **WhatsApp (wa.me)** | Lo arma el front al terminar un pedido; la API no interviene. |

**En desarrollo** (`docker-compose.yml` en la raíz `TopDeck/`): `db` (puerto 5433), `backend` (8080), `frontend` (Vite, 5173).
**En producción** (`docker-compose.prod.yml`): nginx es la única entrada pública; sirve la SPA y hace de proxy de `/api/`
hacia el backend, que no publica puertos. Por eso front y API comparten dominio y no hace falta CORS.

---

## 3. Organización del código

```
src/main/java/org/example/topdeckapi/src/
├── controller/      REST controllers. Agrupan rutas por visibilidad: /public/**, /user/**, /admin/**
├── service/IMPL/    Lógica de negocio (transacciones, validaciones, auditoría, mails)
├── Repository/      Spring Data JPA (queries derivadas, JPQL, SQL nativo para facetas jsonb)
├── model/           Entidades JPA
├── DTOs/            request/ (entrada validada), response/ (salida), auth/, mappers/ (MapStruct)
├── Security/        SecurityConfig, JwtAuthFilter, RateLimitFilter, ClientIpResolver, handlers 401/403
├── Exception/       Excepciones de negocio + GlobalExceptionHandler
├── configuracion/   AsyncConfig (pool de mails), ContactConfig (datos de contacto de los mails)
├── Enumerados/      ROL, ESTADO_PEDIDO, ESTADO_EVENTO, TIPO_ENTREGA
└── util/            Montos (cálculo de importes), AuditMensaje (textos de auditoría), FechaUtils
src/main/resources/
├── application-prod.properties   perfil de producción (versionado; todo por variables de entorno)
├── application.properties        config de desarrollo de cada uno (en .gitignore)
├── db/migration/                 migraciones Flyway V1..Vn (todas las bases)
├── db/seed/                      datos de ejemplo (solo desarrollo)
└── templates/                    plantillas Thymeleaf de los mails
```

Convenciones:

- **Identificadores públicos**: toda entidad tiene un `id` Long interno y un `uuid`. La API **solo expone el uuid**
  (en los DTOs se llama `idProducto`, `idPedido`, etc.). El Long nunca sale.
- **Montos**: `BigDecimal` en Java y `NUMERIC(12,2)` en la base. El único cálculo de subtotal es
  `Montos.subtotal(precio, descuento%, cantidad)` con redondeo `HALF_UP` a 2 decimales. **Los precios siempre salen
  de la base**, nunca del cliente.
- **Errores**: las excepciones de negocio se traducen en `GlobalExceptionHandler` a un JSON uniforme
  (`status`, `error`, `message`, `path`, `timeStamp`, y `fieldErrors` en los 400 de validación). `BussinesException` → 409,
  `ResourceNotFoundException` → 404, validación → 400, `AccessDeniedException` → 403, ruta inexistente → 404.
- **Auditoría**: cada alta, cambio o baja llama a `AuditService.registrar(...)` desde el service (sección 9).

---

## 4. Modelo de datos

```
categoria ◄── producto ──► tag              usuario ◄── direccion
               │  1:1                          │  ▲
               ▼                               │  └── carrito ──► detallecarrito ──► producto
        producto_singles (atributos jsonb)     │
                                               └── pedido ──► direccion (opcional, la del envío)
                                                     └──► detallepedido ──► producto
evento          auditoria          flyway_schema_history
```

| Tabla | Qué guarda | Notas |
|---|---|---|
| `usuario` | Cuentas `ADMIN`/`USER` e invitados `GUEST` | `email` único; `telefono` único (ver API-019); `password` bcrypt (null en GUEST); `email_verificado`; `sesion_version`; hashes de tokens de verificación y reset; `fecha_baja` (cuenta anonimizada); datos de aceptación de TyC (versión, fecha, IP). |
| `producto` | Sellados y singles | `tipo_producto` `PRODUCTO`/`SINGLE`; `precio`, `stock`, `descuento` (%); `activo` (false = oculto en la tienda); categoría opcional en singles; `tag` = franquicia. |
| `producto_singles` | Atributos de un single | 1:1 con producto; `atributos` jsonb con índices GIN y por rareza/set. |
| `categoria`, `tag` | Tipo de producto y franquicia | `tag.nombre_normalizado` (minúsculas, sin acentos) para búsquedas; `tag.img_url`. |
| `carrito`, `detallecarrito` | Carrito de usuarios registrados | Uno por usuario. `carrito.session_id` es legado (el carrito de invitados vive en el navegador). |
| `pedido`, `detallepedido` | Pedidos y sus líneas | `estado` `PENDIENTE`/`CONFIRMADO`/`RECHAZADO`; `tipo_entrega`; `telefono_contacto`; `total`; `ip_usuario`; cada línea guarda `precio_unitario` y `subtotal` del momento. |
| `direccion` | Direcciones de un usuario | `principal` (una por usuario); `activa` (false = oculta porque la usa un pedido). |
| `evento` | Torneos y juntadas | `estado` `PROXIMAMENTE`/`EN_CURSO`/`FINALIZADO` (calculado por fecha). |
| `auditoria` | Registro de cambios | usuario, fecha, acción, tabla, `id_registro` (uuid afectado) y `detalle` (mensaje legible). |

---

## 5. Usuarios, roles y ciclo de vida de una cuenta

| Rol | Quién | Puede |
|---|---|---|
| `ADMIN` | Personal de la tienda | Todo el panel (rutas `/<recurso>/admin/**`) y lo de un usuario. |
| `USER` | Cliente registrado con el mail confirmado | Carrito persistente, pedidos, direcciones, perfil. |
| `GUEST` | Invitado: se crea solo al hacer un pedido sin cuenta | Nada: no tiene contraseña y **nunca puede iniciar sesión**. Solo agrupa sus pedidos. |

**Registro** (`POST /auth/register`) — responde siempre **202** con el mismo mensaje, para no revelar qué mails existen:

- Mail nuevo → crea un `USER` con `email_verificado = false` y le manda un link de confirmación (24 h).
- Mail con cuenta sin confirmar → reenvía el link **sin tocar sus datos** (nadie puede cambiar la contraseña pendiente de otro).
- Mail con cuenta confirmada → le avisa al dueño "ya tenés una cuenta" (como mucho uno cada 30 min).
- Mail de un `GUEST` → flujo de **reclamo**: se manda un link para que el dueño active la cuenta eligiendo la contraseña ahí.

**Confirmación** (`POST /auth/confirmar-email`) pide el token **y la contraseña elegida al registrarse**: si otra persona
registró ese mail primero, el dueño real no la conoce y debe usar "olvidé mi contraseña", que prueba que el buzón es suyo.

**Baja** (`DELETE /user/admin/delete/{id}`, solo admin):

- Sin pedidos → se borra el usuario con su carrito y direcciones.
- Con pedidos → se **anonimiza**: nombre "Usuario eliminado", mail `eliminado-<uuid>@topdeck.invalid`, sin teléfono ni
  contraseña, rol `GUEST`, `fecha_baja` cargada, sesiones cerradas. Desaparece de la lista de usuarios y de las
  estadísticas, pero sus pedidos se conservan.
- Protecciones: un admin no puede borrarse ni cambiarse el rol a sí mismo, y no se puede borrar ni bajar de rol al
  **único** ADMIN (409).

**Perfil propio**: `PATCH /user/me` (nombre y teléfono; un teléfono de otra cuenta se rechaza) y
`POST /user/me/password` (pide la actual). El mail no se puede cambiar todavía.

---

## 6. Autenticación y sesiones

- **JWT HS256** firmado con `JWT_SECRET`. Dura **1 hora**; el front lo renueva con `POST /auth/refresh` 5 minutos antes.
  Claims: `sub` (email), `role` (informativo), `sv` (versión de sesión) y `auth_time` (momento del login).
- **Tope de 7 días**: el refresh conserva `auth_time`, así que ninguna sesión dura más de 7 días desde el login.
- **El rol se lee de la base en cada request** (`JwtAuthFilter`), nunca del token: un token con un rol inventado no da
  permisos y un cambio de rol aplica en la siguiente request.
- **Revocación**: cada usuario tiene `sesion_version`; el token lleva su `sv` y se rechaza si no coincide. La versión sube
  con el logout (cierra **todas** las sesiones), al restablecer o cambiar la contraseña, y cuando un admin cambia la
  contraseña o el rol del usuario. Al cambiar la propia contraseña se devuelve un token nuevo para el dispositivo actual.
- **Sin sesión**: cuentas sin confirmar, invitados y cuentas anonimizadas.
- **Login uniforme**: mail inexistente y contraseña incorrecta responden igual (401) y tardan lo mismo (se compara contra
  un hash de relleno).
- **Fuerza bruta**: `LoginAttemptService` bloquea un mail 15 minutos tras 5 fallos en 15 minutos (429). Aplica al login,
  a la confirmación y al cambio de contraseña. Además `RateLimitFilter` limita por IP: `/auth/**` a 10 req/min y los
  pedidos de invitado a 5 cada 10 min. **Ambos viven en memoria** (alcanza con una sola instancia; ver API-015).
- **IP del cliente**: la decide `ClientIpResolver` en el servidor (el front no la manda). Con `app.trust-proxy=true` toma
  `X-Real-IP` del nginx propio; si no, la de la conexión.
- **Contraseñas**: 8 a 72 caracteres (72 es el límite de bcrypt). Tokens de confirmación, activación y reset: 32 bytes
  aleatorios; en la base solo se guarda su SHA-256, son de un solo uso y vencen (24 h los de cuenta, 1 h el de reset).
- **CORS**: solo para desarrollo (front en otro puerto); sin `allowCredentials`, porque la autenticación va por header.

Reglas de acceso por URL (`SecurityConfig`): `/auth/**` y las rutas `/<recurso>/public/**` abiertas; las `/<recurso>/admin/**` (incluidas `/stats/admin` y `/firebase/admin`) solo `ADMIN`;
`/user/**`, `/carrito/user/**`, `/direccion/user/**`, `/pedidos/user/**` para `USER` o `ADMIN`. Además, cada service
verifica que el recurso (carrito, dirección, pedido) **pertenezca** al usuario autenticado.

---

## 7. Catálogo

- **Listados públicos**: `/products/public/obtenerPaginados` (sellados) y `/products/public/singles` (singles), solo
  activos, con búsqueda, orden, filtro por categoría y franquicia. Los singles filtran además por atributos
  (`attr=clave:valor`) y exponen **facetas** (`/singles/facetas`): cada atributo con sus valores y cantidades, armadas con
  SQL nativo sobre el `jsonb` y ordenadas por prioridad (set, rareza, color, tipo...).
- **Ofertas**: los 8 productos activos con más descuento.
- **Ocultar vs borrar**: `deslistar` / `relistar` cambian `activo` (el producto deja de verse pero conserva su historial).
  Borrar elimina el producto.
- **Importar singles**: el admin busca cartas con `/products/admin/tcg/{franquicia}/{nombre}/...` (proxy a
  api.apitcg.com; la franquicia se valida como slug), el front descarga la imagen, la sube a Firebase y crea el producto
  `SINGLE` con sus atributos.

---

## 8. Carrito, pedidos, direcciones y eventos

**Carrito**
- Invitado: vive solo en el navegador (`localStorage`); la API no interviene hasta el pedido.
- Usuario: persiste en `carrito` / `detallecarrito`. Agregar valida que el producto esté activo y que la cantidad total
  no supere el stock. Al iniciar sesión, el front migra el carrito del navegador al del servidor.

**Pedido** (`POST /pedidos/user/newPedido` o `/pedidos/public/pedidoEfimero` para invitados)
1. Se unifican las líneas repetidas del mismo producto y se valida el stock contra la cantidad total.
2. Precios y total se calculan en el servidor (`Montos`).
3. `ENVIO` exige dirección (propia y activa en el de usuario; en el de invitado se crea o reutiliza una del GUEST).
4. El teléfono de contacto se guarda en el pedido y, si el cliente no tenía teléfono, también en su perfil (salvo que
   otra cuenta ya lo use).
5. Invitado: si el mail no existe se crea un `GUEST`; si es de un GUEST o de una cuenta sin confirmar, se reutiliza; si es
   de una cuenta confirmada, se rechaza ("ingresá a tu cuenta").
6. Se avisa por mail a todos los ADMIN.

**Estados** — `PENDIENTE → CONFIRMADO | RECHAZADO` y `CONFIRMADO → RECHAZADO`:
- Confirmar **descuenta el stock** con lock pesimista (falla si no alcanza) y le manda el mail de confirmación al cliente.
- Rechazar un pedido confirmado, o borrarlo, **repone el stock**.

**Direcciones**
- Una sola `principal` por usuario; la primera que se carga lo es. Si se borra la principal, pasa a serlo otra.
- Una dirección que usa algún pedido **nunca se modifica ni se borra de verdad**: al editarla se oculta (`activa = false`)
  y se guarda la versión nueva como otra dirección; al borrarla solo se oculta. Así cada pedido conserva la dirección a
  la que se envió. Volver a cargar una dirección oculta idéntica la reactiva.

**Eventos**
- El estado sale de la fecha: futura → `PROXIMAMENTE`, hoy → `EN_CURSO`, pasada → `FINALIZADO`. Se calcula al crear, al
  editar la fecha y todas las noches a las 00:00.
- La edición es parcial (`PATCH`): solo cambian los campos enviados.

---

## 9. Auditoría

Cada acción que cambia datos deja una fila en `auditoria`, escrita desde el service (no hay triggers):

| Campo | Contenido |
|---|---|
| `nombre_usuario` | Mail del usuario autenticado, o `SISTEMA` (invitados, procesos automáticos). |
| `accion` / `tabla` | `INSERT`/`UPDATE`/`DELETE` y la entidad (`producto`, `pedido`, `usuario`, `direccion`, `evento`, `categoria`, `tag`). |
| `id_registro` | uuid del registro afectado (el panel lo usa para abrirlo desde el log). |
| `detalle` | Mensaje legible armado con `util/AuditMensaje`. |

Cómo se arman los mensajes (todo en `AuditMensaje`):
- **Altas y bajas** describen el registro: `Creación del producto "Sobre X" ($8.500,00) · stock 50 · 10% off · Sobres · Pokémon`.
- **Ediciones** listan solo lo que cambió, con antes → después: `Edición del producto "Sobre X": precio $8.500,00 → $9.000,00, stock 50 → 45`.
  Se usa `AuditMensaje.Cambios` tomando una foto de los valores antes de modificar.
- **Pedidos** incluyen código corto (`#A1B2C3D4`), ítems, total, entrega y el efecto en el stock.
- **Usuarios y direcciones nunca guardan datos personales**: solo dicen qué campos cambiaron ("Editó su perfil: teléfono").

Consulta: `GET /audit/admin/obtenerPaginados` con filtros `accion`, `tabla`, `usuario` (parcial), `desde`, `hasta`.
Los logs de más de `app.audit.dias-retencion` días (90 por defecto) se borran todas las noches a las 03:30.

---

## 10. Imágenes (Firebase Storage)

- El front sube y borra imágenes directo en Firebase Storage (carpetas `productos/`, `Tags/`, `PromosCarrusel/`, `cards/`).
- Para que **solo los admins** puedan escribir, la API emite con `GET /firebase/admin/token` un custom token con el claim
  `admin: true`; el front hace `signInWithCustomToken` y las reglas (`Front-Top-Deck/storage.rules`) exigen ese claim.
  Las reglas también limitan a PNG/JPEG/WebP/GIF de menos de 10 MB. Lectura pública.
- Requiere `FIREBASE_CREDENTIALS_PATH` (service account). Sin eso el endpoint responde 503 y no se pueden subir imágenes.

---

## 11. Mails

`EmailService` arma HTML con Thymeleaf (`resources/templates/`):

| Plantilla | Cuándo | A quién | Envío |
|---|---|---|---|
| `EmailAccionCuenta.html` | Confirmar registro, restablecer contraseña, aviso "ya tenés una cuenta" | El dueño del mail | Confirmación: sincrónico; resto: asincrónico |
| `EmailVerificarCuenta.html` | Activar una cuenta que tenía pedidos como invitado | El dueño del mail | Sincrónico |
| `EmailNuevoPedido.html` | Se crea un pedido | Todos los ADMIN | Asincrónico |
| `EmailPedidoConfirmado.html` | Un pedido pasa a CONFIRMADO | El cliente (no a cuentas anonimizadas) | Asincrónico |

- Los asincrónicos van a un pool acotado (`mailExecutor`: 2-4 hilos, cola de 200): si el SMTP falla, el pedido igual se registra.
- Los datos de contacto del pie salen de `app.contacto.*`; el logo, de `${APP_BASE_URL}/img/logo.webp`.
- **Desarrollo sin SMTP**: `app.email.log-links=true` (solo en el `application.properties` local) escribe los links en el
  log y no corta el registro si el mail falla. Nunca en producción.

---

## 12. Estadísticas

`GET /stats/admin` devuelve en una sola respuesta (`StatsResponse`) los datos del dashboard: resumen general (ingresos de
pedidos confirmados, ticket promedio, conteos), ventas por mes, pedidos y eventos por estado, productos por categoría y
franquicia, usuarios por rol (sin cuentas dadas de baja), top de productos vendidos, distribución de stock (sin stock /
bajo / ok, solo sellados), registros por mes, ingresos por franquicia y acciones de auditoría.

---

## 13. Base de datos y migraciones (Flyway)

- El esquema lo crean y actualizan las migraciones de `db/migration/` al arrancar la API. Hibernate está en
  `ddl-auto=validate`: **no crea ni cambia nada**, solo verifica que las entidades coincidan; si falta algo, la API no arranca.
- Migraciones actuales: `V1` esquema inicial · `V2` columnas de cuentas y pedidos + montos `NUMERIC` · `V3`
  `direccion.activa` · `V4` `auditoria.id_registro` y `detalle` · `V5` `detalle` a 500 caracteres.
- `db/seed/V1_1__dev_seed.sql` carga datos de ejemplo **solo en desarrollo** y solo si no hay categorías.
- Bases creadas antes de Flyway se toman como versión 1 (`baseline-on-migrate`).
- **Para cambiar el esquema**: nuevo archivo `V<n+1>__descripcion.sql`. Nunca editar uno aplicado (Flyway compara
  checksums y no arranca).

---

## 14. Configuración y entornos

| Archivo / perfil | Uso |
|---|---|
| `application.properties` | Desarrollo de cada uno. **En .gitignore.** Flyway con `db/migration` + `db/seed`. |
| `application-local.properties` | Overrides de desarrollo (ignorado por git). Nunca en producción. |
| `application-prod.properties` | Producción (`SPRING_PROFILES_ACTIVE=prod`). Versionado y autosuficiente: todo por variables de entorno, logs en INFO/WARN, sin seed. |

La lista completa de variables está en el README. Las imprescindibles: `DB_*`, `JWT_SECRET`, `SPRING_EMAIL` /
`SPRING_EMAIL_PASSWORD`, `TCG_API_KEY`, `FIREBASE_CREDENTIALS_PATH`, `APP_FRONT_URL`, `APP_BASE_URL`, `APP_TRUST_PROXY`.

---

## 15. Tests

JUnit 5 + Mockito, sin base de datos (`./mvnw test`, o el Maven local con `-Duser.timezone=America/Argentina/Buenos_Aires`
en Windows):

| Clase | Qué cubre |
|---|---|
| `JwtAuthFilterTest` | Rol desde la base, sesión revocada, tope de 7 días |
| `RateLimitFilterTest`, `ClientIpResolverTest` | Límites por IP y resolución de IP |
| `AuthServiceTest` | Login uniforme, registro y confirmación, reclamo de GUEST, reset y cambio de contraseña |
| `UsuarioServiceTest` | Protección del último admin, baja con anonimización, perfil propio |
| `DireccionServiceTest` | Dueño de la dirección, principal, ocultar/editar las usadas en pedidos |
| `PedidoServiceTest` | Unificación de líneas, stock, entrega y teléfono |
| `EventoServiceTest` | Edición parcial y estado por fecha |
| `MontosTest`, `AuditMensajeTest` | Cálculo de importes y formato de los mensajes de auditoría |

No hay tests de integración contra Postgres ni de reglas por URL todavía (API-042).

---

## 16. Cómo extender sin romper nada

- **Columna o tabla nueva**: migración `V<n>` + campo en la entidad + DTO/mapper. Probar que la API arranque (`validate`).
- **Endpoint nuevo**: ubicarlo bajo `/public`, `/user` o `/admin` según quién lo use (eso define la seguridad); si toca un
  recurso de un usuario, verificar el dueño en el service.
- **Acción que modifica datos**: llamar a `auditService.registrar(accion, tabla, uuid, mensaje)` con un mensaje de
  `AuditMensaje` (en ediciones, `Cambios` con los valores de antes). Sin datos personales en usuarios ni direcciones.
- **Importes**: siempre `BigDecimal` y `Montos`; nunca confiar en precios del cliente.
- **Mail nuevo**: plantilla en `templates/` y método en `EmailService` (asincrónico salvo que el flujo dependa de que salga).

---

## 17. Limitaciones conocidas

- Bloqueo de login y rate limit en memoria: con más de una instancia de la API habría que compartirlos (API-015).
- El token vive en `localStorage` del navegador; la defensa principal ante XSS es la CSP del nginx (API-017 / FRONT-013).
- `telefono` es único en `usuario`, así que dos clientes no pueden tener el mismo en su perfil (API-019).
- Pedidos de invitado con mails ajenos: solo limitados por IP (API-014, decisión de negocio pendiente).
- Sin pago online ni cálculo de envío (API-034, API-035).
