# TopDeck API

API RESTful construida con Spring Boot 3.5.4 y Java 21 para el e-commerce de cartas TCG (Trading Card Games) importados.

## Stack Tecnologico

- **Framework**: Spring Boot 3.5.4
- **Java**: 21
- **Base de datos**: PostgreSQL 16
- **ORM**: Spring Data JPA / Hibernate (`ddl-auto=validate`)
- **Migraciones**: Flyway (`src/main/resources/db/migration`)
- **Seguridad**: Spring Security + JWT (jjwt 0.11.5)
- **Mapeo**: MapStruct 1.6.3
- **Validacion**: Jakarta Bean Validation
- **Email**: Spring Mail (SMTP Gmail) + plantillas Thymeleaf
- **Storage**: Firebase Admin SDK 9.4.3 (solo para emitir tokens de admin)
- **Utilidades**: Lombok
- **Build**: Maven

---

## Arquitectura

```
                    ┌─────────────────────────────────────────────┐
                    │              Cliente (Frontend)              │
                    │         React + Vite (localhost:5173)        │
                    └─────────────────┬───────────────────────────┘
                                      │ HTTP (JSON)
                                      ▼
                    ┌─────────────────────────────────────────────┐
                    │            Security Filters                 │
                    │  ┌─────────────┐  ┌──────────────────────┐  │
                    │  │ Rate Limit  │→ │   JWT Auth Filter    │  │
                    │  │ (10 req/min)│  │ (Bearer token)       │  │
                    │  └─────────────┘  └──────────────────────┘  │
                    └─────────────────┬───────────────────────────┘
                                      ▼
                    ┌─────────────────────────────────────────────┐
                    │           Controller Layer                  │
                    │  AuthController      (public)               │
                    │  ProductoController  (public/admin)         │
                    │  PedidoController    (public/admin/user)    │
                    │  UsuarioController   (admin/user)           │
                    │  CarritoController   (user)                 │
                    │  CategoriaController (public/admin)         │
                    │  TagController       (public/admin)         │
                    │  EventosController   (public/admin)         │
                    │  DireccionController (user)                 │
                    │  AuditController     (admin)                │
                    │  StatsController     (admin)                │
                    │  FirebaseController  (admin)                │
                    └─────────────────┬───────────────────────────┘
                                      ▼
                    ┌─────────────────────────────────────────────┐
                    │            Service Layer                   │
                    │  AuthService, UsuarioService,               │
                    │  ProductoService, PedidoService,            │
                    │  CarritoService, CategoriaService,          │
                    │  TagService, EventoService,                 │
                    │  DireccionService, DetallePedidoService,    │
                    │  StatsService, JwtService,                  │
                    │  PaginacionService, AuditService,           │
                    │  EmailService, FirebaseTokenService         │
                    └─────────────────┬───────────────────────────┘
                                      ▼
                    ┌─────────────────────────────────────────────┐
                    │          Repository Layer (JPA)             │
                    │  IUsuarioRepo, IProductoRepo,               │
                    │  IProductoSinglesRepository,                │
                    │  ProductoSinglesQueryRepository (facetas),  │
                    │  IPedidoRepo, IDetallePedidoRepo,           │
                    │  ICategoriasRepo, ITagRepository,           │
                    │  ICarritoRepository, IDetalleCarritoRepo,   │
                    │  IDireccionRepo, IEventoRepository,         │
                    │  IAuditRepository                           │
                    └─────────────────┬───────────────────────────┘
                                      ▼
                    ┌─────────────────────────────────────────────┐
                    │              PostgreSQL 16                  │
                    │         topdeck_database                    │
                    └─────────────────────────────────────────────┘
```

---

## Modelo de Datos (ER)

```
┌──────────────┐       ┌──────────────┐       ┌──────────────────┐
│  categoria   │       │     tag      │       │     usuario      │
│──────────────│       │──────────────│       │──────────────────│
│ id_categoria │PK     │ id_tag       │PK     │ id_usuario       │PK
│ uuid         │       │ uuid         │       │ uuid             │
│ nombre       │       │ nombre       │       │ nombre           │
└──────┬───────┘       │ nombre_norm  │       │ email            │
       │               │ img_url      │       │ password         │
       │               └──────┬───────┘       │ telefono         │
       │                      │               │ rol              │
       │    ┌─────────────────┘               │ ip_usuario       │
       ▼    ▼                                 │ token_verif_hash │
┌──────────────┐       ┌──────────────────┐   │ token_verif_exp  │
│   producto   │       │ producto_singles │   └──────┬───────────┘
│──────────────│       │──────────────────│          │
│ id_producto  │PK ◄───│ id_producto      │FK 1:1    │
│ uuid         │       │ atributos (jsonb)│          │
│ id_categoria │FK (nullable para singles) └─────────┘│
│ id_tag       │FK                                   │
│ nombre       │       ┌──────────────┐              │
│ descripcion  │       │   direccion  │              │
│ precio       │       │──────────────│              │
│ stock        │       │ id_direccion │PK             │
│ img_url      │       │ uuid         │              │
│ descuento    │       │ id_usuario   │FK→usuario←───┘
│ activo       │       │ ciudad       │
│ tipo_producto│       │ provincia    │
└──────┬───────┘       │ codigo_postal│
       │               │ direccion    │
       │               │ altura       │
       │               │ piso         │
       │               │ pais         │
       │               │ principal    │
       │               └──────────────┘
       │
       │               ┌──────────────┐       ┌──────────────┐
       │               │    pedido    │       │  auditoria   │
       │               │──────────────│       │──────────────│
       │               │ id_pedido    │PK     │ id_log       │PK
       │               │ uuid         │       │ nombre_usuario│
       │               │ id_usuario   │FK     │ fecha_audit  │
       │               │ id_direccion │FK     │ accion       │
       │               │ fecha_pedido │       │ tabla        │
       │               │ estado       │       └──────────────┘
       │               │ total        │
       │               │ ip_usuario   │       ┌──────────────┐
       │               └──────┬───────┘       │    evento    │
       │                      │               │──────────────│
       ▼                      ▼               │ id_evento    │PK
┌──────────────┐       ┌──────────────┐       │ uuid         │
│detallepedido │       │   carrito    │       │ nombre_evento│
│──────────────│       │──────────────│       │ ubicacion    │
│ id_detalle   │PK     │ id_carrito   │PK     │ fecha        │
│ uuid         │       │ uuid         │       │ hora         │
│ id_producto  │FK     │ id_usuario   │FK     │ precio_entrada│
│ id_pedido    │FK     │ session_id   │       │ estado       │
│ cantidad     │       │ fecha_creac  │       └──────────────┘
│ precio_unit  │       └──────┬───────┘
│ subtotal     │              │
└──────────────┘              ▼
                      ┌──────────────┐
                      │detallecarrito│
                      │──────────────│
                      │ id_detalle   │PK
                      │ uuid         │
                      │ id_carrito   │FK
                      │ id_producto  │FK
                      │ cantidad     │
                      └──────────────┘
```

> `carrito.session_id` es legacy: el carrito de invitados ahora vive 100% en el `localStorage` del front.

---

## Pipeline de Datos

```
Request HTTP
    │
    ▼
┌─────────────────────┐
│   RateLimitFilter   │──→ 429 si >10 req/min en /auth/**
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│   JwtAuthFilter     │──→ Valida Bearer token → carga el usuario y su rol DESDE LA DB → SecurityContext
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ SecurityConfig      │──→ Verifica permisos (permitAll / hasRole / authenticated)
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│    Controller       │──→ Valida @Valid, parsea Request DTO
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│     Service         │──→ Logica de negocio, validaciones, transacciones
│  (MapStruct mapper) │──→ Entity ↔ DTO conversion
│                     │──→ AuditService.registrar(accion, tabla)
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│    Repository       │──→ JPQL / derived queries / SQL nativo (facetas jsonb)
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│   PostgreSQL 16     │──→ Persistencia
└─────────┬───────────┘
          ▼
Response HTTP (JSON)
```

La auditoria no usa triggers: cada service llama a `AuditService.registrar(...)` y se guarda el usuario del `SecurityContext` (o `SISTEMA` si es anonimo).

---

## Seguridad

### Autenticacion (JWT)
- Token JWT con expiracion de 1 hora, renovable con `POST /auth/refresh`, pero **nunca mas de 7 dias desde el login** (claim `auth_time`, que se conserva al renovar)
- Algoritmo: HS256
- Claims: email (subject), rol (informativo), `sv` (version de sesion) y `auth_time`
- **Revocacion**: cada usuario tiene `sesion_version`. `JwtAuthFilter` rechaza el token si su `sv` no coincide. La version sube con `POST /auth/logout` (cierra la sesion en todos los dispositivos), al restablecer la contraseña y cuando un admin cambia la contraseña o el rol del usuario.
- Cuentas con el email sin confirmar (`email_verificado = false`) no obtienen sesion. `null` = cuenta anterior a esta funcion, se toma como confirmada.
- Header: `Authorization: Bearer <token>`
- **El rol NO se toma del token**: `JwtAuthFilter` carga el usuario de la DB en cada request y usa su rol actual. Un token firmado con un rol falso no da permisos, y un cambio de rol aplica en la siguiente request.
- Si el token es de un usuario borrado, la request sigue como anonima.
- **Login**: email inexistente y password incorrecta responden lo mismo (**401** "Email o contraseña incorrectos") y tardan lo mismo (se compara contra un hash de relleno), asi no se puede averiguar que emails estan registrados. Las cuentas GUEST no tienen password y nunca pueden loguearse.
- **Fuerza bruta**: ademas del limite por IP (`RateLimitFilter`), `LoginAttemptService` bloquea un email 15 minutos tras 5 contraseñas incorrectas en 15 minutos (**429**). Se cuenta aunque el email no exista, para no revelar nada. Vive en memoria (un reinicio lo limpia).
- **Contraseñas**: entre 8 y 72 caracteres (72 es el limite real de bcrypt).

### Registro con confirmacion de email
1. `POST /auth/register` responde **siempre 202** con el mismo mensaje, exista o no el email (no permite averiguar quien tiene cuenta):
   - email nuevo: crea la cuenta `USER` con `email_verificado = false` y manda `EmailAccionCuenta.html` con `${APP_FRONT_URL}/verificar-cuenta?tipo=registro&token=...` (24 h, solo se guarda el SHA-256);
   - cuenta sin confirmar: reenvia el link **sin tocar sus datos** (nadie puede cambiar la contraseña pendiente de otro);
   - cuenta confirmada: le manda al dueño un aviso "ya tenes una cuenta" (como mucho uno cada 30 min);
   - GUEST: flujo de reclamo (abajo).
2. `POST /auth/confirmar-email` `{ token, password }` pide **la contraseña elegida al registrarse**: si otra persona registro ese email primero, el dueño real no la conoce y no puede confirmar esa cuenta; usa "olvide mi contraseña", que prueba que el buzon es suyo y le deja elegir la propia.
3. Login con la cuenta sin confirmar → **403** y se reenvia el link.
4. Un pedido como invitado con el email de una cuenta sin confirmar se acepta (no bloquea al dueño real).

### Olvide mi contraseña
- `POST /auth/olvide-contrasena` `{ email }` → **siempre 202** con el mismo mensaje. Solo si la cuenta existe (y no es GUEST) se manda el link `${APP_FRONT_URL}/restablecer-contrasena?token=...` (1 h, un solo uso, SHA-256 en `token_reset_hash`, no se reenvia antes de 2 min).
- `POST /auth/restablecer-contrasena` `{ token, password }` → cambia la contraseña, confirma el email, cierra las demas sesiones y devuelve un JWT nuevo.

### Desarrollo sin SMTP
`app.email.log-links=true` (solo en el `application.properties` local) escribe en el log los links de confirmacion / restablecimiento / reclamo y no corta el registro si el mail falla. **Nunca en produccion**: quien lea los logs podria tomar cuentas. `application-prod.properties` no lo define (queda en `false`).

### Autorizacion (Roles)
| Rol | Descripcion |
|---|---|
| `ADMIN` | Acceso total al panel administrativo y endpoints protegidos |
| `USER` | Usuario registrado con carrito persistente y pedidos |
| `GUEST` | Usuario efimero creado para pedidos sin registro (sin password) |

### Reclamo de cuentas GUEST (verificacion por email)
Cuando alguien se registra con un email que ya tiene pedidos como invitado (`GUEST`), la cuenta **no** se convierte en `USER` en ese momento, porque cualquiera podria escribir ese email:

1. `POST /auth/register` responde **202** `{ "verificacionPendiente": true, "message": ... }` y manda un email (`EmailVerificarCuenta.html`) con un link `${APP_FRONT_URL}/verificar-cuenta?token=...`.
2. El token es aleatorio (32 bytes); en la DB solo se guarda su **SHA-256** (`token_verificacion_hash`) y vence a las **24 h**. Si se pide de nuevo dentro de los 2 minutos no se reenvia.
3. El dueño del email abre el link, **elige la contraseña ahi** y acepta los TyC: `POST /auth/verificar-cuenta`. La cuenta pasa a `USER`, se crea su carrito, se borra el token y se devuelve un JWT.

Elegir la contraseña al confirmar (y no al registrarse) evita que quien inicio el registro conozca la contraseña, aunque un escaner de correo abra el link automaticamente.

### Firebase Storage (solo admins escriben)
Las imagenes (productos, tags, carrusel, singles) se suben desde el front directo a Firebase Storage. Para que solo los admins puedan escribir:

- `GET /firebase/admin/token` (solo `ADMIN`) emite un **custom token de Firebase** con el claim `admin: true` (uid = uuid del usuario).
- El front hace `signInWithCustomToken` y las reglas (`Front-Top-Deck/storage.rules`) exigen `request.auth.token.admin == true` para escribir.
- Requiere `FIREBASE_CREDENTIALS_PATH` apuntando al JSON de una service account. Sin eso el endpoint responde **503** y los admins no pueden subir imagenes.

### IP del cliente (`ClientIpResolver`)
Un solo lugar decide la IP del cliente, para el rate limit y para la IP que se guarda al aceptar los TyC (registro, pedidos, activacion). **La IP siempre la toma el servidor**: el campo `ipUsuario` que mande el front se ignora.

| `app.trust-proxy` (`APP_TRUST_PROXY`) | IP usada |
|---|---|
| `false` (default) | La de la conexion (`getRemoteAddr`). Los headers `X-Forwarded-For` / `X-Real-IP` se ignoran |
| `true` | `X-Real-IP`, si no el ultimo valor de `X-Forwarded-For`, si no la de la conexion |

`true` solo es seguro si la API **unicamente** es accesible a traves del nginx propio (`Front-Top-Deck/nginx.conf`), que pisa esos headers con la IP que vio. Si la API queda expuesta directo, cualquiera puede inventarse la IP.

### Rate Limiting
Ventana fija por IP y por regla, en memoria (alcanza con una sola instancia de la API; con varias haria falta un store compartido como Redis):

| Regla | Limite | Respuesta |
|---|---|---|
| `/auth/**` | 10 requests / minuto | 429 + `Retry-After` |
| `POST /pedidos/public/pedidoEfimero` | 5 pedidos / 10 minutos | 429 + `Retry-After` |

### CORS
- Origenes permitidos: `app.cors.allowed-origins` (`APP_CORS_ALLOWED_ORIGINS`, separados por coma). Vacio = `http://localhost:5173`
- Con el nginx de produccion front y API comparten dominio, asi que CORS no interviene
- Metodos: GET, POST, PATCH, DELETE, OPTIONS
- Credenciales: habilitadas

---

## Endpoints

### Publicos (`permitAll`)

| Metodo | Path | Descripcion |
|---|---|---|
| POST | `/auth/login` | Login con email/password, retorna JWT |
| POST | `/auth/register` | Registro. Siempre 202 sin token: hay que confirmar el email (ver arriba) |
| POST | `/auth/confirmar-email` | Confirma una cuenta nueva: `{ token, password }` → JWT |
| POST | `/auth/verificar-cuenta` | Confirma el reclamo de un GUEST: `{ token, password, terminosAceptados, ... }` |
| POST | `/auth/olvide-contrasena` | `{ email }` → siempre 202; manda el link si la cuenta existe |
| POST | `/auth/restablecer-contrasena` | `{ token, password }` → JWT; cierra las demas sesiones |
| POST | `/auth/logout` | Cierra todas las sesiones del usuario (204) |
| POST | `/auth/refresh` | Renueva un token vigente, hasta 7 dias desde el login |
| GET | `/products/public/obtenerPaginados` | Productos sellados paginados (solo activos). Params: `pagina`, `tamanio`, `sortBy`, `direction`, `filter`, `idTag`, `idCategoria` |
| GET | `/products/public/singles` | Singles paginados. Params: los de arriba + `attr=clave:valor` (repetible) |
| GET | `/products/public/singles/facetas` | Facetas (atributos y valores con conteo) de singles. Param: `idTag` |
| GET | `/products/public/ofertas` | Top 8 productos con descuento |
| GET | `/products/public/{id}` | Producto por UUID |
| GET | `/category/public/getAll` | Todas las categorias |
| GET | `/category/public/{id}` | Categoria por UUID |
| GET | `/tags/public/getAll` | Todos los tags |
| GET | `/eventos/public/getAll` | Todos los eventos |
| POST | `/pedidos/public/pedidoEfimero` | Crear pedido como invitado (crea/reusa un usuario GUEST) |

### Admin (`hasRole("ADMIN")`)

| Metodo | Path | Descripcion |
|---|---|---|
| GET | `/stats/admin` | Estadisticas completas del sistema |
| GET | `/firebase/admin/token` | Custom token de Firebase con claim `admin` |
| GET | `/products/admin/obtenerPaginados` | Productos paginados (incluye inactivos y singles) |
| POST | `/products/admin/post` | Crear producto (o single con `tipoProducto: "SINGLE"` + `atributos`) |
| PATCH | `/products/admin/edit/{id}` | Editar producto |
| PATCH | `/products/admin/deslistar/{id}` | Deslistar producto |
| PATCH | `/products/admin/relistar/{id}` | Volver a listar producto |
| DELETE | `/products/admin/delete/{id}` | Eliminar producto |
| GET | `/products/admin/tcg/{franquicia}/{nombre}/{page}/{limit}` | Proxy a API externa de TCG. `franquicia` debe ser un slug (`[a-z0-9-]`), `limit` max 50 |
| GET | `/user/admin/obtenerPaginados` | Usuarios paginados |
| GET | `/user/admin/{id}` | Usuario por UUID |
| POST | `/user/admin/crearUsuario` | Crear usuario |
| PATCH | `/user/admin/{id}` | Actualizacion parcial: solo cambia los campos enviados (`nombre`, `email`, `password`, `telefono`, `rol`). No toca TyC ni IP |
| DELETE | `/user/admin/delete/{id}` | Eliminar usuario (si tiene pedidos, se anonimiza y se conservan los pedidos) |
| GET | `/pedidos/admin/obtenerPaginados` | Pedidos paginados |
| GET | `/pedidos/admin/{id}/getDetalles` | Detalles de un pedido |
| PATCH | `/pedidos/admin/{id}/newState` | Cambiar estado de pedido (descuenta/repone stock) |
| DELETE | `/pedidos/admin/delete/{id}` | Eliminar pedido (si estaba CONFIRMADO, repone stock) |
| POST | `/category/admin/new` | Crear categoria |
| PATCH | `/category/admin/edit/{id}` | Editar categoria |
| DELETE | `/category/admin/delete/{id}` | Eliminar categoria |
| GET | `/category/admin/getId/{nombre}` | UUID de categoria por nombre |
| POST | `/tags/admin/post` | Crear tag |
| PATCH | `/tags/admin/edit/{id}` | Editar tag |
| DELETE | `/tags/admin/delete/{id}` | Eliminar tag |
| GET | `/tags/admin/getId/{nombre}` | UUID de tag por nombre |
| GET | `/tags/admin/getById/{id}` | Tag por UUID |
| POST | `/eventos/admin/save` | Crear evento |
| DELETE | `/eventos/admin/delete/{id}` | Eliminar evento |
| GET | `/audit/admin/getAll` | Todos los logs de auditoria |
| GET | `/audit/admin/obtenerPaginados` | Logs paginados. Params: `pagina`, `tamanio`, `filtro` |

### User (`hasAnyRole("USER", "ADMIN")`)

| Metodo | Path | Descripcion |
|---|---|---|
| GET | `/user/me` | Perfil del usuario autenticado |
| PATCH | `/user/me` | Editar nombre y teléfono propios |
| POST | `/user/me/password` | Cambiar la contraseña (`{actual, nueva}`): cierra las otras sesiones y devuelve un token nuevo |
| GET | `/carrito/user/getCarrito` | Carrito del usuario autenticado |
| GET | `/carrito/user/{id}/detalles` | Detalles del carrito |
| POST | `/carrito/user/agregarDetalle` | Agregar producto al carrito |
| PATCH | `/carrito/user/detalle/actualizar/{id}` | Actualizar cantidad (`?nuevaCantidad=`) |
| DELETE | `/carrito/user/detalle/{id}` | Eliminar producto del carrito |
| DELETE | `/carrito/user/{id}/empty` | Vaciar carrito |
| POST | `/pedidos/user/newPedido` | Crear pedido autenticado |
| GET | `/pedidos/user/pedidos` | Pedidos del usuario |
| POST | `/direccion/user/save` | Crear direccion |
| GET | `/direccion/user/getAll` | Direcciones del usuario |
| PATCH | `/direccion/user/{id}` | Editar una dirección propia (si la usa algún pedido, se guarda como nueva y la vieja queda para ese pedido) |
| PATCH | `/direccion/user/{id}/principal` | Marcarla como principal |
| DELETE | `/direccion/user/{id}` | Eliminarla (si la usa algún pedido, solo se oculta) |

Todas las operaciones de carrito, direcciones y pedidos de usuario validan que el recurso pertenezca al usuario autenticado. Los precios y totales de los pedidos se calculan en el servidor.

---

## Modelo de Datos - Entidades

### Usuario
- `idUsuario` (Long, PK), `uuid` (String), `nombre`, `email`, `password` (write-only), `telefono`, `rol` (ADMIN/USER/GUEST), `ipUsuario`, `versionTerminosYCondicionesAceptados`, `terminosAceptados`, `fechaAceptacionTerminos`, `tokenVerificacionHash` y `tokenVerificacionExpira` (JSON-ignored, ver reclamo de GUEST)

### Producto
- `idProducto` (Long, PK), `uuid`, `categoria` (FK→Categoria, opcional en singles), `tag` (FK→Tag), `nombre`, `descripcion`, `precio` (BigDecimal), `stock` (Integer), `imgUrl`, `descuento` (Integer, porcentaje), `activo` (Boolean), `tipoProducto` (`PRODUCTO` | `SINGLE`)

### ProductoSingles
- `idProductoSingles` (Long, PK), `uuid`, `producto` (1:1 con Producto), `atributos` (`jsonb`: set, rarity, color, type, etc.)
- Las facetas se arman con SQL nativo sobre el `jsonb` y se ordenan por prioridad: set, rarity, color, colors, type, card type, y despues alfabetico.

### Categoria
- `idCategoria` (Long, PK), `uuid`, `nombre`

### Tag
- `idTag` (Long, PK), `uuid`, `nombre`, `nombreNormalizado` (lowercase, sin acentos), `imgUrl`

### Pedido
- `idPedido` (Long, PK), `uuid`, `usuario` (FK→Usuario), `direccion` (FK→Direccion), `fechaPedido` (LocalDateTime), `estado` (PENDIENTE/CONFIRMADO/RECHAZADO), `total` (BigDecimal), `detalles` (OneToMany), `ipUsuario`
- Transiciones permitidas: PENDIENTE → CONFIRMADO/RECHAZADO, CONFIRMADO → RECHAZADO. Confirmar descuenta stock (con lock pesimista); rechazar o **borrar** uno confirmado lo repone.
- Al crear un pedido, las lineas con el mismo producto se **unifican** y el stock se valida contra la cantidad total pedida. Los precios salen siempre de la DB.

### DetallePedido
- `idDetallePedido` (Long, PK), `uuid`, `producto` (FK→Producto), `pedido` (FK→Pedido), `cantidad`, `precioUnitario` (BigDecimal), `subTotal` (BigDecimal)

### Carrito
- `idCarrito` (Long, PK), `uuid`, `usuario` (FK→Usuario), `sessionId` (legacy), `fechaCreacion`, `detalles` (OneToMany)

### DetalleCarrito
- `idDetalleCarrito` (Long, PK), `uuid`, `carrito` (FK→Carrito), `producto` (FK→Producto), `cantidad`

### Direccion
- `idDireccion` (Long, PK), `uuid`, `usuario` (FK→Usuario), `ciudad`, `provincia`, `pais`, `codigoPostal`, `direccion`, `altura`, `piso`, `principal` (Boolean)

### Evento
- `idEvento` (Long, PK), `uuid`, `nombreEvento`, `ubicacion`, `fecha` (LocalDate), `hora` (LocalTime), `precioEntrada` (BigDecimal), `estado` (PROXIMAMENTE/EN_CURSO/FINALIZADO)

### Montos
- Todos los importes son `BigDecimal` en Java y `NUMERIC(12,2)` en la DB. En el JSON siguen siendo numeros (`"precio": 8500.00`).
- `util/Montos.subtotal(precio, descuento%, cantidad)` es el unico calculo de subtotal (lo usan carrito y pedidos): redondeo `HALF_UP` a 2 decimales.

### Audit
- `idAuditoria` (Long, PK, columna: `id_log`), `nombreUsuario`, `fechaAudit` (LocalDateTime), `accion`, `tabla`

---

## DTOs y Mappers

### Convenciones
- **Request DTOs**: `{Entity}Request.java` con `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder`
- **Response DTOs**: `{Entity}Response.java` con las mismas anotaciones
- **Mappers**: `{Entity}Mapper.java` con `@Mapper(componentModel = "spring")`
- **ID publico**: Todos los DTOs exponen `uuid` como `id{Entity}` (nunca el Long interno)

### Mappers disponibles
`UsuarioMapper`, `ProductoMapper`, `PedidoMapper`, `DetallePedidoMapper`, `CarritoMapper`, `DetalleCarritoMapper`, `DireccionMapper`, `CategoriaMapper`, `TagMapper`, `EventoMapper`

### Response de estadisticas
`StatsResponse` es un `record` de Java con sub-records anidados:
- `ResumenGeneral`: KPIs generales
- `VentaPorPeriodo`: Revenue agrupado por mes
- `ConteoEstado`: Conteo por estado (pedidos, eventos)
- `ConteoNombre`: Conteo generico (categorias, tags, roles, acciones)
- `TopProducto`: Productos mas vendidos
- `DistribucionStock`: Sin stock / bajo / ok
- `RegistroPorMes`: Registros agrupados por mes
- `RevenueTag`: Revenue por franquicia

### Facetas de singles
`FacetaResponse` (`atributo` + lista de `ValorFacetaResponse` con valor y cantidad).

---

## Excepciones

```
RuntimeException
├── BussinesException              → 409 CONFLICT
│   └── EmailYaRegistradoException → 409 CONFLICT (code: EMAIL_YA_REGISTRADO)
└── ResourceNotFoundException      → 404 NOT FOUND
    ├── CarritoNotFoundException
    ├── PedidoNotFoundException
    ├── UsuarioNotFoundException
    └── ProductNotFoundException
```

**GlobalExceptionHandler** (`@RestControllerAdvice`):

| Excepcion | Status |
|---|---|
| `BussinesException` | 409 |
| `ResponseStatusException` | El status que trae (ej. 503 de Firebase) |
| `ResourceNotFoundException` | 404 |
| `MethodArgumentNotValidException` | 400 (con `fieldErrors`) |
| `DataIntegrityViolationException` | 409 |
| `DateTimeParseException`, `IllegalArgumentException`, `HttpMessageNotReadableException` | 400 |
| `AccessDeniedException` | 403 |
| `Exception` (catch-all) | 500 |

---

## Emails

`EmailService` manda HTML con plantillas Thymeleaf de `src/main/resources/templates/`:

| Plantilla | Cuando | A quien |
|---|---|---|
| `EmailNuevoPedido.html` | Se crea un pedido (usuario o invitado) | Todos los ADMIN |
| `EmailPedidoConfirmado.html` | Un pedido pasa a CONFIRMADO | El cliente |
| `EmailVerificarCuenta.html` | Registro con email de un GUEST | El dueño del email |

- Los datos de contacto del pie salen de `app.contacto.*` (`ContactConfig`). Hoy son placeholders: completarlos con los datos reales.
- El logo se sirve desde la propia API: `${APP_BASE_URL}/img/logo.webp`.
- **Mails de pedidos (nuevo pedido, confirmado)**: el HTML se arma en el request y el envio va a un pool acotado (`mailExecutor` en `AsyncConfig`: 2-4 hilos, cola de 200). Si el SMTP esta lento o caido, el pedido responde igual; si la cola se llena, se descartan envios y se loguea.
- **Mail de verificacion**: es sincronico, porque si falla el registro tiene que avisar (409).

---

## Tareas Programadas

| Cron | Tarea |
|---|---|
| `0 0 0 * * ?` (medianoche) | Actualiza estados de eventos: `PROXIMAMENTE` → `EN_CURSO` → `FINALIZADO` segun la fecha |
| `0 30 3 * * *` (03:30) | **Borra logs de auditoria con mas de 90 dias** (`AuditService.DIAS_RETENCION_LOGS`) |

---

## Variables de Entorno

| Variable | Descripcion | Requerida |
|---|---|---|
| `DB_HOST` | Host de PostgreSQL (default `localhost`) | No |
| `DB_PORT` | Puerto de PostgreSQL (default `5432`; el compose expone `5433`) | No |
| `DB_NAME` | Nombre de la base (default `topdeck_database`) | No |
| `DB_USER` | Usuario (default `postgres`) | No |
| `DB_PASSWORD` | Password de PostgreSQL | Si |
| `JWT_SECRET` | Secret para firmar JWT (minimo 32 bytes) | Si |
| `TCG_API_KEY` | API key para api.apitcg.com | Si |
| `SPRING_EMAIL` | Cuenta Gmail que envia los mails | Si |
| `SPRING_EMAIL_PASSWORD` | App password de esa cuenta | Si |
| `APP_BASE_URL` | URL publica de la API (logo en mails). Default `http://localhost:8080` | No |
| `APP_FRONT_URL` | URL publica del front (links en mails). Default `http://localhost:5173` | No |
| `FIREBASE_CREDENTIALS_PATH` | Ruta al JSON de la service account de Firebase | Para subir imagenes |
| `APP_TRUST_PROXY` | `true` solo detras del nginx propio (ver IP del cliente) | No (default `false`) |
| `APP_CORS_ALLOWED_ORIGINS` | Origenes CORS separados por coma | No |
| `APP_CONTACTO_TELEFONO`, `APP_CONTACTO_EMAIL`, `APP_CONTACTO_DIRECCION`, `APP_CONTACTO_HORARIO` | Datos de contacto del pie de los mails (perfil `prod`) | No |
| `SPRING_PROFILES_ACTIVE` | `prod` en produccion | Si (prod) |

`application.properties` esta en el `.gitignore` (es la config de desarrollo de cada uno). El perfil **`prod`** (`application-prod.properties`, versionado) es autosuficiente: todo sale de variables de entorno, con logs en `INFO`/`WARN`, sin `show-sql` ni parametros SQL, y cache de Thymeleaf activa.

---

## Como Ejecutar

### Con Docker Compose (recomendado)
```bash
# Desde la raiz del proyecto (TopDeck/)
cp .env.example .env
# Editar .env con tus valores (formato CLAVE=valor, sin espacios alrededor del =)
# Copiar la service account de Firebase a ./secrets/firebase-service-account.json

docker-compose up --build
```

Al arrancar, la API crea o actualiza las tablas con Flyway; en desarrollo además carga datos de ejemplo (ver [Datos Iniciales](#datos-iniciales-seed)).

### Migraciones (Flyway)
El esquema lo manejan las migraciones de `src/main/resources/db/migration/`, que Flyway aplica al arrancar la API. Hibernate está en `ddl-auto=validate`: solo verifica que las entidades coincidan con las tablas y **no crea ni cambia nada**.

| Migración | Qué hace |
|---|---|
| `V1__initial_schema.sql` | Tablas e índices iniciales |
| `V2__accounts_and_orders_columns.sql` | Columnas de cuentas (confirmación de mail, sesiones, reset, baja) y de pedidos (entrega, teléfono); importes en `NUMERIC(12,2)` |
| `V3__direccion_activa.sql` | `direccion.activa`: las direcciones usadas en pedidos se ocultan en vez de borrarse |

- **Cambiar el esquema** (columna nueva, tipo, índice): agregar `V<n>__descripcion.sql` con el número siguiente. Nunca editar una migración ya aplicada (Flyway compara checksums y no arranca).
- **Bases creadas antes de Flyway** (con tablas y sin historial): `baseline-on-migrate` las marca como versión 1 y les aplica de la V2 en adelante. La V2 es idempotente, así que no toca lo que ya existe.
- El historial queda en la tabla `flyway_schema_history`.

### Desarrollo local
```bash
# Requiere Java 21 y PostgreSQL 16 (o levantar solo la DB: docker compose up -d db)
cd API-top-deck
./mvnw spring-boot:run
```

- El wrapper necesita la carpeta `.mvn/wrapper/`, que hoy no esta en el repo. Si falla, usa un Maven instalado (`mvn spring-boot:run`) o el de IntelliJ.
- **Windows**: si la JVM toma la zona `America/Buenos_Aires`, Postgres rechaza la conexion (`invalid value for parameter "TimeZone"`). Corre con `-Duser.timezone=America/Argentina/Buenos_Aires`.
- El perfil `local` (`application-local.properties`, ignorado por git) pisa valores para desarrollo. **No activarlo en produccion.**

La API corre en `http://localhost:8080`.

### Tests
```bash
./mvnw test
```

Tests unitarios (JUnit 5 + Mockito, sin DB): `JwtAuthFilter` (rol desde la DB), `ClientIpResolver`, `RateLimitFilter`, `AuthService` (login uniforme y reclamo de GUEST), `PedidoService` (productos repetidos, stock al borrar) y `Montos`. `TopDeckApiApplicationTests` levanta el contexto completo y solo corre si hay `DB_PASSWORD` configurado.

---

## Produccion con Docker

`docker-compose.prod.yml` (en la raiz `TopDeck/`) levanta:

```
Internet ──► frontend (nginx :80) ──┬── /        → SPA (build de Vite)
                                    └── /api/... → backend:8080 (sin puertos publicados)
                                                      └── db (sin puertos publicados)
```

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

- El backend corre con `SPRING_PROFILES_ACTIVE=prod` y `APP_TRUST_PROXY=true`, valido porque solo nginx lo alcanza.
- `APP_PUBLIC_URL` (ej. `https://topdeck.com.ar`) arma `APP_FRONT_URL` y `APP_BASE_URL` (`.../api`).
- HTTPS: terminarlo en un proxy/balanceador delante, o agregar el certificado al nginx. Si hay otro proxy o CDN delante de nginx, configurar `real_ip` en nginx para que `$remote_addr` sea el cliente real.

## Checklist para produccion

1. **Secrets nuevos**, nunca los de desarrollo: el `jwt.secret` y la password de la DB de desarrollo quedaron en el historial de git (commit `429ace3`). Generar con `openssl rand -base64 48`.
2. Cargarlos como **variables de entorno** de la plataforma, no en archivos. Activar el perfil `prod`, nunca el `local`.
3. Service account de Firebase disponible en `FIREBASE_CREDENTIALS_PATH`, y **despues** publicar `Front-Top-Deck/storage.rules` en la consola de Firebase.
4. `APP_PUBLIC_URL` con el dominio real. Si front y API van en dominios distintos (sin el nginx), cargar `APP_CORS_ALLOWED_ORIGINS`.
5. La API **no** debe quedar accesible directo desde internet cuando `APP_TRUST_PROXY=true`.
6. Completar `APP_CONTACTO_*` con los datos reales del negocio.
7. Crear el ADMIN a mano: el perfil `prod` no carga el seed, así que la base arranca sin usuarios.

---

## Datos Iniciales (Seed)

`src/main/resources/db/seed/V1_1__dev_seed.sql` carga datos de ejemplo **solo en desarrollo** (`spring.flyway.locations` incluye `classpath:db/seed`; el perfil `prod` no) y solo si la base no tiene categorías:

**Categorias**: Sobres, Cajas, Barajas, Accesorios, Figuras

**Tags (Franquicias)**: Pokemon, Dragon Ball Super, One Piece, Yu-Gi-Oh!, Magic: The Gathering

**Productos**: 10 productos de ejemplo distribuidos entre las categorias y franquicias

**Usuario ADMIN por defecto**: con su password como hash bcrypt dentro del archivo. Como el archivo está en el repo, ese hash es público: sirve solo para desarrollo.
