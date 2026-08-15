# TopDeck API

API RESTful construida con Spring Boot 3.5.4 y Java 21 para el e-commerce de cartas TCG (Trading Card Games) importados.

## Stack Tecnologico

- **Framework**: Spring Boot 3.5.4
- **Java**: 21
- **Base de datos**: MySQL 8.0
- **ORM**: Spring Data JPA / Hibernate
- **Seguridad**: Spring Security + JWT (jjwt 0.11.5)
- **Mapeo**: MapStruct 1.6.3
- **Validacion**: Jakarta Bean Validation
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
                    │  UsuarioController   (admin)                │
                    │  CarritoController   (user)                 │
                    │  CategoriaController (public/admin)         │
                    │  TagController       (public/admin)         │
                    │  EventosController   (public/admin)         │
                    │  DireccionController (user)                 │
                    │  AuditController     (admin)                │
                    │  StatsController     (admin)                │
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
                    │  PaginacionService, AuditService            │
                    └─────────────────┬───────────────────────────┘
                                      ▼
                    ┌─────────────────────────────────────────────┐
                    │          Repository Layer (JPA)             │
                    │  IUsuarioRepo, IProductoRepo,               │
                    │  IPedidoRepo, IDetallePedidoRepo,           │
                    │  ICategoriasRepo, ITagRepository,           │
                    │  ICarritoRepository, IDetalleCarritoRepo,   │
                    │  IDireccionRepo, IEventoRepository,         │
                    │  IAuditRepository                           │
                    └─────────────────┬───────────────────────────┘
                                      ▼
                    ┌─────────────────────────────────────────────┐
                    │              MySQL 8.0                      │
                    │         topdeck_database                    │
                    └─────────────────────────────────────────────┘
```

---

## Modelo de Datos (ER)

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│  categoria   │       │     tag      │       │   usuario    │
│──────────────│       │──────────────│       │──────────────│
│ id_categoria │PK     │ id_tag       │PK     │ id_usuario   │PK
│ uuid         │       │ uuid         │       │ uuid         │
│ nombre       │       │ nombre       │       │ nombre       │
└──────┬───────┘       │ nombre_norm  │       │ email        │
       │               │ img_url      │       │ password     │
       │               └──────┬───────┘       │ telefono     │
       │                      │               │ rol          │
       │    ┌─────────────────┘               │ ip_usuario   │
       │    │                                 └──────┬───────┘
       ▼    ▼                                        │
┌──────────────┐                                     │
│   producto   │                                     │
│──────────────│                                     │
│ id_producto  │PK                                   │
│ uuid         │                                     │
│ id_categoria │FK→categoria                         │
│ id_tag       │FK→tag                               │
│ nombre       │       ┌──────────────┐              │
│ descripcion  │       │   direccion  │              │
│ precio       │       │──────────────│              │
│ stock        │       │ id_direccion │PK             │
│ img_url      │       │ uuid         │              │
│ descuento    │       │ id_usuario   │FK→usuario←───┘
│ activo       │       │ ciudad       │
└──────┬───────┘       │ provincia    │
       │               │ codigo_postal│
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
│   JwtAuthFilter     │──→ Extrae Bearer token → valida → setea SecurityContext
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
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│    Repository       │──→ JPQL queries / derived queries
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│    MySQL 8.0        │──→ Persistencia, triggers de auditoria
└─────────┬───────────┘
          ▼
Response HTTP (JSON)
```

---

## Seguridad

### Autenticacion (JWT)
- Token JWT con expiracion de 1 hora
- Algoritmo: HS256
- Claims: email (subject), rol
- Header: `Authorization: Bearer <token>`

### Autorizacion (Roles)
| Rol | Descripcion |
|---|---|
| `ADMIN` | Acceso total al panel administrativo y endpoints protegidos |
| `USER` | Usuario registrado con carrito persistente y pedidos |
| `GUEST` | Usuario efimero creado para pedidos sin registro |

### Rate Limiting
- Endpoint: `/auth/**`
- Limite: 10 requests por minuto por IP
- Soporta header `X-Forwarded-For` para deteccion de IP real

### CORS
- Origen permitido: `http://localhost:5173`
- Metodos: GET, POST, PATCH, DELETE, OPTIONS
- Credenciales: habilitadas

---

## Endpoints

### Publicos (`permitAll`)

| Metodo | Path | Descripcion |
|---|---|---|
| POST | `/auth/login` | Login con email/password, retorna JWT |
| POST | `/auth/register` | Registro de nuevo usuario, retorna JWT |
| GET | `/auth/validate/start` | Valida token Bearer existente |
| GET | `/products/public/obtenerPaginados` | Productos paginados (solo activos) |
| GET | `/products/public/ofertas` | Top 8 productos con descuento |
| GET | `/products/public/{id}` | Producto por UUID |
| GET | `/category/public/getAll` | Todas las categorias |
| GET | `/category/public/{id}` | Categoria por UUID |
| GET | `/tags/public/getAll` | Todos los tags |
| GET | `/eventos/public/getAll` | Todos los eventos |
| POST | `/pedidos/public/pedidoEfimero` | Crear pedido como invitado |

### Admin (`hasRole("ADMIN")`)

| Metodo | Path | Descripcion |
|---|---|---|
| GET | `/admin/stats` | Estadisticas completas del sistema |
| GET | `/products/admin/obtenerPaginados` | Productos paginados (incluye inactivos) |
| POST | `/products/admin/post` | Crear producto |
| PATCH | `/products/admin/edit/{id}` | Editar producto |
| PATCH | `/products/admin/deslistar/{id}` | Activar/deslistar producto |
| DELETE | `/products/admin/delete/{id}` | Eliminar producto |
| GET | `/products/admin/tcg/{franquicia}/{nombre}/{page}/{limit}` | Proxy a API externa de TCG |
| GET | `/user/admin/obtenerPaginados` | Usuarios paginados |
| GET | `/user/admin/{id}` | Usuario por UUID |
| POST | `/user/admin/crearUsuario` | Crear usuario |
| PATCH | `/user/admin/{id}` | Actualizar usuario |
| DELETE | `/user/admin/delete/{id}` | Eliminar usuario |
| GET | `/pedidos/admin/obtenerPaginados` | Pedidos paginados |
| GET | `/pedidos/admin/{id}/getDetalles` | Detalles de un pedido |
| PATCH | `/pedidos/admin/{id}/newState` | Cambiar estado de pedido |
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

### User (`hasAnyRole("USER", "ADMIN")`)

| Metodo | Path | Descripcion |
|---|---|---|
| GET | `/carrito/user/getCarrito` | Carrito del usuario autenticado |
| GET | `/carrito/user/{id}/detalles` | Detalles del carrito |
| POST | `/carrito/user/agregarDetalle` | Agregar producto al carrito |
| PATCH | `/carrito/user/detalle/actualizar/{id}` | Actualizar cantidad |
| DELETE | `/carrito/user/detalle/{id}` | Eliminar producto del carrito |
| DELETE | `/carrito/user/{id}/empty` | Vaciar carrito |
| POST | `/pedidos/user/newPedido` | Crear pedido autenticado |
| GET | `/pedidos/user/pedidos` | Pedidos del usuario |
| POST | `/direccion/user/save` | Crear direccion |
| GET | `/direccion/user/getAll` | Direcciones del usuario |
| GET | `/user/public/{id}` | Usuario por UUID |

---

## Modelo de Datos - Entidades

### Usuario
- `idUsuario` (Long, PK), `uuid` (String), `nombre`, `email`, `password` (JSON-ignored), `telefono`, `rol` (ADMIN/USER/GUEST), `ipUsuario`, `versionTerminosYCondicionesAceptados`, `terminosAceptados`

### Producto
- `idProducto` (Long, PK), `uuid`, `categoria` (FK→Categoria), `tag` (FK→Tag), `nombre`, `descripcion`, `precio` (Double), `stock` (Integer), `imgUrl`, `descuento` (Integer, porcentaje), `activo` (Boolean)

### Categoria
- `idCategoria` (Long, PK), `uuid`, `nombre`

### Tag
- `idTag` (Long, PK), `uuid`, `nombre`, `nombreNormalizado` (lowercase, sin acentos), `imgUrl`

### Pedido
- `idPedido` (Long, PK), `uuid`, `usuario` (FK→Usuario), `direccion` (FK→Direccion), `fechaPedido` (LocalDateTime), `estado` (PENDIENTE/CONFIRMADO/RECHAZADO), `total` (Double), `detalles` (OneToMany), `ipUsuario`

### DetallePedido
- `idDetallePedido` (Long, PK), `uuid`, `producto` (FK→Producto), `pedido` (FK→Pedido), `cantidad`, `precioUnitario`, `subTotal`

### Carrito
- `idCarrito` (Long, PK), `uuid`, `usuario` (FK→Usuario), `sessionId`, `fechaCreacion`, `detalles` (OneToMany)

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

---

## Excepciones

```
RuntimeException
├── BussinesException              → 409 CONFLICT
│   └── EmailYaRegistradoException → 409 CONFLICT
└── ResourceNotFoundException      → 404 NOT FOUND
    ├── CarritoNotFoundException
    ├── PedidoNotFoundException
    ├── UsuarioNotFoundException
    └── ProductNotFoundException
```

**GlobalExceptionHandler** (`@RestControllerAdvice`):
- `BussinesException` → 409
- `ResourceNotFoundException` → 404
- `MethodArgumentNotValidException` → 400 (con errores de campo)
- `DataIntegrityViolationException` → 409
- `Exception` (catch-all) → 500

---

## Tareas Programadas

- **Actualizacion de estados de eventos**: Cron `0 0 0 * * ?` (medianoche diario). Cambia automaticamente `PROXIMAMENTE` → `EN_CURSO` → `FINALIZADO` segun la fecha del evento.

---

## Variables de Entorno

| Variable | Descripcion | Requerida |
|---|---|---|
| `DB_HOST` | Host de MySQL | Si |
| `DB_PORT` | Puerto de MySQL | Si |
| `DB_NAME` | Nombre de la base de datos | Si |
| `DB_USER` | Usuario de MySQL | Si |
| `DB_PASSWORD` | Password de MySQL | Si |
| `JWT_SECRET` | Secret para firmar JWT | Si |
| `TCG_API_KEY` | API key para api.apitcg.com | Si |

---

## Como Ejecutar

### Con Docker Compose (recomendado)
```bash
# Desde la raiz del proyecto
cp .env.example .env
# Editar .env con tus valores

docker-compose up --build
```

### Desarrollo local
```bash
# Requiere Java 21 y MySQL 8.0
cd API-top-deck

# Compilar
./mvnw clean install

# Ejecutar
./mvnw spring-boot:run
```

La API corre en `http://localhost:8080`.

---

## Datos Iniciales (Seed)

El archivo `TopDeck-DB-Tables.sql` incluye datos semilla:

**Categorias**: Sobres, Cajas, Barajas, Accesorios, Figuras

**Tags (Franquicias)**: Pokemon, Dragon Ball Super, One Piece, Yu-Gi-Oh!, Magic: The Gathering

**Productos**: 10 productos de ejemplo distribuidos entre las categorias y franquicias
