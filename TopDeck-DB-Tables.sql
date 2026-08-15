create database if not exists topdeck_database;

create table categoria(
	id_categoria bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    nombre varchar(255)
);

create table tag(
	id_tag bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    nombre varchar(255) unique,
    nombre_normalizado varchar(255) unique,
    img_url varchar(255)
);

create table producto(
	id_producto bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    id_categoria bigint,
    id_tag bigint,
    nombre varchar(255),
    descripcion longtext,
    precio double precision,
    stock int,
    img_url varchar(255),
    descuento int default 0,
    activo bit(1) default b'1',
    foreign key (id_categoria) references categoria(id_categoria),
    foreign key (id_tag) references tag(id_tag)
);

create table usuario(
	id_usuario bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    nombre varchar(255),
    email varchar(255) not null unique,
    password varchar(255),
    telefono varchar(255) unique,
    rol enum('ADMIN','USER','GUESS'),
    ip_usuario varchar(255),
    version_terminos_y_condiciones_aceptadas varchar(255),
    terminos_aceptados bit(1)
);

create table direccion(
	id_direccion bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    id_usuario bigint not null,
    ciudad varchar(255),
    provincia varchar(255),
    codigo_postal varchar(255),
    altura varchar(255),
    pais varchar(255),
    direccion varchar(255),
    piso varchar(255),
    principal bit(1),
    foreign key (id_usuario) references usuario(id_usuario)
);

create table pedido(
	id_pedido bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    id_usuario bigint not null,
    id_direccion bigint,
    fecha_pedido datetime not null default current_timestamp,
    estado enum('PENDIENTE','CONFIRMADO','RECHAZADO'),
    total double precision not null,
    ip_usuario varchar(255),
    foreign key (id_usuario) references usuario(id_usuario),
    foreign key (id_direccion) references direccion(id_direccion)
);

create table detallepedido(
	id_detalle_pedido bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    id_pedido bigint not null,
    id_producto bigint not null,
	cantidad int,
    precio_unitario double precision,
    subtotal double precision,
    foreign key (id_pedido) references pedido (id_pedido),
    foreign key (id_producto) references producto(id_producto)
);

create table carrito (
	id_carrito bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    id_usuario bigint,
    session_id varchar(255),
    fecha_creacion datetime default current_timestamp,
    foreign key (id_usuario) references usuario(id_usuario)
);

create table detallecarrito(
	id_detalle_carrito bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    id_carrito bigint not null,
    id_producto bigint not null,
    cantidad int default 1,
    foreign key (id_carrito) references carrito(id_carrito),
    foreign key (id_producto) references producto (id_producto)
);

create table auditoria(
	id_log bigint primary key auto_increment,
    nombre_usuario varchar(255),
    fecha_audit datetime default current_timestamp,
    accion varchar(255),
    tabla varchar(255)
);

create table evento(
	id_evento bigint primary key auto_increment,
    uuid varchar(36) not null unique,
    nombre_evento varchar(255),
    ubicacion varchar(255),
    fecha date,
    hora time,
    precio_entrada double precision,
    estado enum('PROXIMAMENTE','EN_CURSO','FINALIZADO')
);

use topdeck_database;

insert into categoria(uuid, nombre) values
    (UUID(), 'Sobres'),
    (UUID(), 'Cajas'),
    (UUID(), 'Barajas'),
    (UUID(), 'Accesorios'),
    (UUID(), 'Figuras');

insert into tag(uuid, nombre, img_url) values
                                           (UUID(), 'Pokémon', 'https://placehold.co/600x600?text=Pokemon'),
                                           (UUID(), 'Dragon Ball Super', 'https://placehold.co/600x600?text=Dragon+Ball'),
                                           (UUID(), 'One Piece', 'https://placehold.co/600x600?text=One+Piece'),
                                           (UUID(), 'Yu-Gi-Oh!', 'https://placehold.co/600x600?text=Yu-Gi-Oh'),
                                           (UUID(), 'Magic: The Gathering', 'https://placehold.co/600x600?text=Magic');

update tag set nombre_normalizado = replace(replace(replace(replace(replace(replace(replace(replace(
    lower(nombre),
    'á','a'),'é','e'),'í','i'),'ó','o'),'ú','u'),'ü','u'),'ñ','n'),'-','')
where nombre_normalizado is null;

insert into producto(
    uuid,
    id_categoria,
    id_tag,
    nombre,
    descripcion,
    precio,
    stock,
    img_url,
    descuento
) values
      (
          UUID(),
          1,
          1,
          'Sobre Evoluciones Prismáticas Pokémon',
          'Sobre de 10 cartas de la colección Evoluciones Prismáticas',
          8500.00,
          50,
          'https://placehold.co/600x600?text=Sobre+Pokemon',
          0
      ),
      (
          UUID(),
          2,
          1,
          'Caja Evoluciones Prismáticas Pokémon',
          'Caja sellada de 36 sobres de Evoluciones Prismáticas',
          45000.00,
          10,
          'https://placehold.co/600x600?text=Caja+Pokemon',
          5
      ),
      (
          UUID(),
          3,
          1,
          'Baraja Liga Evoluciones Prismáticas',
          'Baraja lista para jugar de la temporada Evoluciones Prismáticas',
          12000.00,
          20,
          'https://placehold.co/600x600?text=Baraja+Pokemon',
          0
      ),
      (
          UUID(),
          1,
          2,
          'Sobre Battle Spirits Saga Dragon Ball',
          'Sobre de 12 cartas de Battle Spirits Saga Dragon Ball',
          6500.00,
          40,
          'https://placehold.co/600x600?text=Sobre+Dragon+Ball',
          0
      ),
      (
          UUID(),
          2,
          2,
          'Caja Battle Spirits Saga Dragon Ball',
          'Caja sellada de 24 sobres de Battle Spirits Saga',
          35000.00,
          8,
          'https://placehold.co/600x600?text=Caja+Dragon+Ball',
          10
      ),
      (
          UUID(),
          1,
          3,
          'Sobre Romance Dawn One Piece',
          'Sobre de 12 cartas de la colección Romance Dawn',
          7500.00,
          30,
          'https://placehold.co/600x600?text=Sobre+One+Piece',
          0
      ),
      (
          UUID(),
          2,
          3,
          'Caja Romance Dawn One Piece',
          'Caja sellada de 24 sobres de Romance Dawn',
          40000.00,
          5,
          'https://placehold.co/600x600?text=Caja+One+Piece',
          0
      ),
      (
          UUID(),
          1,
          4,
          'Sobre Rage of the Abyss Yu-Gi-Oh!',
          'Sobre de 9 cartas de Rage of the Abyss',
          5500.00,
          60,
          'https://placehold.co/600x600?text=Yu-Gi-Oh',
          0
      ),
      (
          UUID(),
          1,
          5,
          'Sobre Innistrad Remastered Magic',
          'Sobre de 15 cartas de Innistrad Remastered',
          9000.00,
          25,
          'https://placehold.co/600x600?text=Magic',
          0
      ),
      (
          UUID(),
          4,
          1,
          'Album Pokémon 9 Bolsillos',
          'Álbum de colección con fundas para 9 cartas por página',
          3500.00,
          100,
          'https://placehold.co/600x600?text=Album+Pokemon',
          0
      );

-- ═══════════════════════════════════════════
-- Indices para optimizar queries del dashboard
-- ═══════════════════════════════════════════

-- pedido: acelera GROUP BY y WHERE por estado (6 queries del dashboard)
CREATE INDEX idx_pedido_estado ON pedido(estado);

-- detallepedido: acelera JOINs de top productos y revenue por tag
CREATE INDEX idx_detalle_pedido_producto ON detallepedido(id_pedido, id_producto);

-- producto: acelera filtros de stock y ofertas
CREATE INDEX idx_producto_activo_stock ON producto(activo, stock);
CREATE INDEX idx_producto_activo_descuento ON producto(activo, descuento);

-- auditoria: acelera GROUP BY por accion
CREATE INDEX idx_auditoria_accion ON auditoria(accion);

-- evento: acelera GROUP BY por estado
CREATE INDEX idx_evento_estado ON evento(estado);