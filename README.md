# TopDeck API

API RESTful construida con Spring Boot 3.5.4 y Java 21 para el e-commerce de cartas TCG (Trading Card Games) importados.

## Stack Tecnologico

- **Framework**: Spring Boot 3.5.4
- **Java**: 21
- **Base de datos**: PostgreSQL 16
- **ORM**: Spring Data JPA / Hibernate (`ddl-auto=update`)
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
- Token JWT con expiracion de 1 hora, renovable con `POST /auth/refresh`
- Algoritmo: HS256
- Claims: email (subject), rol (informativo)
- Header: `Authorization: Bearer <token>`
- **El rol NO se toma del token**: `JwtAuthFilter` carga el usuario de la DB en cada request y usa su rol actual. Un token firmado con un rol falso no da permisos, y un cambio de rol aplica en la siguiente request.
- Si el token es de un usuario borrado, la request sigue como anonima.

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

### Rate Limiting
- Endpoint: `/auth/**`
- Limite: 10 requests por minuto por IP
- Toma la IP de `X-Forwarded-For` si viene (ojo: el cliente puede falsificarlo si no hay un proxy que lo pise)

### CORS
- Origen permitido: `http://localhost:5173` (fijo en `SecurityConfig`)
- Metodos: GET, POST, PATCH, DELETE, OPTIONS
- Credenciales: habilitadas

---

## Endpoints

### Publicos (`permitAll`)

| Metodo | Path | Descripcion |
|---|---|---|
| POST | `/auth/login` | Login con email/password, retorna JWT |
| POST | `/auth/register` | Registro. 200 + JWT, o 202 si el email era de un GUEST (ver arriba) |
| POST | `/auth/verificar-cuenta` | Confirma el reclamo de un GUEST: `{ token, password, terminosAceptados, ... }` |
| GET | `/auth/validate/start` | Valida token Bearer existente |
| POST | `/auth/refresh` | Renueva un token vigente |
| GET | `/products/public/obtenerPaginados` | Productos sellados paginados (solo activos). Params: `pagina`, `tamanio`, `sortBy`, `direction`, `filter`, `idTag`, `idCategoria` |
| GET | `/products/public/singles` | Singles paginados. Params: los de arriba + `attr=clave:valor` (repetible) |
| GET | `/products/public/singles/facetas` | Facetas (atributos y valores con conteo) de singles. Param: `idTag` |
| GET | `/products/public/ofertas` | Top 8 productos con descuento |
| GET | `/products/public/{id}` | Producto por UUID |
| GET | `/products/test` | Health check simple |
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
| GET | `/products/admin/tcg/{franquicia}/{nombre}/{page}/{limit}` | Proxy a API externa de TCG |
| GET | `/user/admin/obtenerPaginados` | Usuarios paginados |
| GET | `/user/admin/{id}` | Usuario por UUID |
| POST | `/user/admin/crearUsuario` | Crear usuario |
| PATCH | `/user/admin/{id}` | Actualizar usuario |
| DELETE | `/user/admin/delete/{id}` | Eliminar usuario |
| GET | `/pedidos/admin/obtenerPaginados` | Pedidos paginados |
| GET | `/pedidos/admin/{id}/getDetalles` | Detalles de un pedido |
| PATCH | `/pedidos/admin/{id}/newState` | Cambiar estado de pedido (descuenta/repone stock) |
| DELETE | `/pedidos/admin/delete/{id}` | Eliminar pedido |
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

Todas las operaciones de carrito, direcciones y pedidos de usuario validan que el recurso pertenezca al usuario autenticado. Los precios y totales de los pedidos se calculan en el servidor.

---

## Modelo de Datos - Entidades

### Usuario
- `idUsuario` (Long, PK), `uuid` (String), `nombre`, `email`, `password` (write-only), `telefono`, `rol` (ADMIN/USER/GUEST), `ipUsuario`, `versionTerminosYCondicionesAceptados`, `terminosAceptados`, `fechaAceptacionTerminos`, `tokenVerificacionHash` y `tokenVerificacionExpira` (JSON-ignored, ver reclamo de GUEST)

### Producto
- `idProducto` (Long, PK), `uuid`, `categoria` (FK→Categoria, opcional en singles), `tag` (FK→Tag), `nombre`, `descripcion`, `precio` (Double), `stock` (Integer), `imgUrl`, `descuento` (Integer, porcentaje), `activo` (Boolean), `tipoProducto` (`PRODUCTO` | `SINGLE`)

### ProductoSingles
- `idProductoSingles` (Long, PK), `uuid`, `producto` (1:1 con Producto), `atributos` (`jsonb`: set, rarity, color, type, etc.)
- Las facetas se arman con SQL nativo sobre el `jsonb` y se ordenan por prioridad: set, rarity, color, colors, type, card type, y despues alfabetico.

### Categoria
- `idCategoria` (Long, PK), `uuid`, `nombre`

### Tag
- `idTag` (Long, PK), `uuid`, `nombre`, `nombreNormalizado` (lowercase, sin acentos), `imgUrl`

### Pedido
- `idPedido` (Long, PK), `uuid`, `usuario` (FK→Usuario), `direccion` (FK→Direccion), `fechaPedido` (LocalDateTime), `estado` (PENDIENTE/CONFIRMADO/RECHAZADO), `total` (Double), `detalles` (OneToMany), `ipUsuario`
- Transiciones permitidas: PENDIENTE → CONFIRMADO/RECHAZADO, CONFIRMADO → RECHAZADO. Confirmar descuenta stock (con lock pesimista); rechazar uno confirmado lo repone.

### DetallePedido
- `idDetallePedido` (Long, PK), `uuid`, `producto` (FK→Producto), `pedido` (FK→Pedido), `cantidad`, `precioUnitario`, `subTotal`

### Carrito
- `idCarrito` (Long, PK), `uuid`, `usuario` (FK→Usuario), `sessionId` (legacy), `fechaCreacion`, `detalles` (OneToMany)

### DetalleCarrito
- `idDetalleCarrito` (Long, PK), `uuid`, `carrito` (FK→Carrito), `producto` (FK→Producto), `cantidad`

### Direccion
- `idDireccion` (Long, PK), `uuid`, `usuario` (FK→Usuario), `ciudad`, `provincia`, `pais`, `codigoPostal`, `direccion`, `altura`, `piso`, `principal` (Boolean)

### Evento
- `idEvento` (Long, PK), `uuid`, `nombreEvento`, `ubicacion`, `fecha` (LocalDate), `hora` (LocalTime), `precioEntrada`, `estado` (PROXIMAMENTE/EN_CURSO/FINALIZADO)

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
- El envio es sincronico. Si falla el mail de pedidos, se loguea y el pedido igual se guarda; si falla el de verificacion, el registro devuelve 409.

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

`application.properties` esta en el `.gitignore`: si clonas el repo de cero, crealo a partir de estas variables.

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

La base se inicializa con `TopDeck-DB-Tables.sql` solo la primera vez que se crea el volumen `db_data`.

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

---

## Checklist para produccion

1. **Secrets nuevos**, nunca los de desarrollo: el `jwt.secret` y la password de la DB de desarrollo quedaron en el historial de git (commit `429ace3`). Generar con `openssl rand -base64 48`.
2. Cargarlos como **variables de entorno** de la plataforma, no en archivos. No activar el perfil `local`.
3. Service account de Firebase disponible en `FIREBASE_CREDENTIALS_PATH`, y **despues** publicar `Front-Top-Deck/storage.rules` en la consola de Firebase.
4. `APP_FRONT_URL` y `APP_BASE_URL` con los dominios reales; agregar el dominio del front a CORS en `SecurityConfig`.
5. Bajar los logs: hoy `application.properties` tiene `DEBUG`/`TRACE` (incluye parametros SQL) y `show-sql=true`.
6. Completar `app.contacto.*` con los datos reales del negocio.
7. Cambiar la password del ADMIN que crea el seed (su hash esta publico en `TopDeck-DB-Tables.sql`).

---

## Datos Iniciales (Seed)

El archivo `TopDeck-DB-Tables.sql` crea las tablas e incluye datos semilla:

**Categorias**: Sobres, Cajas, Barajas, Accesorios, Figuras

**Tags (Franquicias)**: Pokemon, Dragon Ball Super, One Piece, Yu-Gi-Oh!, Magic: The Gathering

**Productos**: 10 productos de ejemplo distribuidos entre las categorias y franquicias

**Usuario ADMIN por defecto**: con su password como hash bcrypt dentro del archivo. Como el archivo esta en el repo, ese hash es publico: en produccion no usar ese usuario (o cambiarle la password apenas se crea la base).
