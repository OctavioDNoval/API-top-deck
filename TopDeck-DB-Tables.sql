create database if not exists topdeck_database;

create table categoria(
	id_categoria bigint primary key auto_increment,
    nombre varchar(225)
);

create table tag(
	id_tag bigint primary key auto_increment,
    nombre varchar(225) unique,
    img_url varchar(225)
);

create table producto(
	id_producto bigint primary key auto_increment,
    id_categoria bigint,
    id_tag bigint,
    nombre varchar(225),
    descripcion text,
    precio decimal(10,2),
    stock int,
    img_url varchar(225),
    descuento int default 0,
    foreign key (id_categoria) references categoria(id_categoria),
    foreign key (id_tag) references tag(id_tag)
);

create table usuario(
	id_usuario bigint primary key auto_increment,
    nombre varchar(225),
    email varchar(225) unique,
    password varchar(225),
    telefono varchar(225) unique,
    rol enum('ADMIN','USER','GUESS'),
    ip_usuario varchar(225),
    version_terminos_y_condiciones_aceptadas varchar(50),
    terminos_aceptados tinyint(1)
);

create table direccion(
	id_direccion bigint primary key auto_increment,
    id_usuario bigint,
    ciudad varchar(225),
    provincia varchar(225),
    codigo_postal varchar(225),
    altura varchar(225),
    pais varchar(225),
    direccion varchar(225),
    piso varchar(225),
    principal tinyint(1),
    foreign key (id_usuario) references usuario(id_usuario)
);

create table pedido(
	id_pedido bigint primary key auto_increment,
    id_usuario bigint,
    id_direccion bigint,
    fecha_pedido datetime default current_timestamp,
    estado enum('pendiente','confirmado','rechazado'),
    total decimal(10,2),
    ip_usuario varchar(225),
    foreign key (id_usuario) references usuario(id_usuario),
    foreign key (id_direccion) references direccion(id_direccion)
);

create table detallepedido(
	id_detalle_pedido bigint primary key auto_increment,
    id_pedido bigint,
    id_producto bigint,
	cantidad int,
    precio_unitario decimal(10,2),
    subtotal decimal(10,2) generated always as (cantidad * precio_unitario) stored,
    foreign key (id_pedido) references pedido (id_pedido),
    foreign key (id_producto) references producto(id_producto)
);

create table carrito (
	id_carrito bigint primary key auto_increment,
    id_usuario bigint, 
    session_id varchar(225),
    fecha_creacion datetime default current_timestamp,
    foreign key (id_usuario) references usuario(id_usuario)
);

create table detallecarrito(
	id_detalle_carrito bigint primary key auto_increment,
    id_carrito bigint not null,
    id_producto bigint not null,
    cantidad int default 1,
    foreign key (id_carrito) references carrito(id_carrito),
    foreign key (id_producto) references producto (id_producto)
);

create table auditoria(
	id_log bigint primary key auto_increment,
    nombre_usuario varchar(225),
    fecha_audit datetime default current_timestamp,
    accion varchar(225),
    tabla varchar(225)
);

create table evento(
	id_evento bigint primary key auto_increment,
    nombre_evento varchar(225),
    ubicacion varchar(225),
    fecha date,
    hora time,
    precio_entrada decimal(10,2),
    estado enum('proximamente','en curso','finalizado')
);

use topdeck_database;

insert into categoria(nombre) values
    ('Sobres'),
    ('Cajas'),
    ('Barajas'),
    ('Accesorios'),
    ('Figuras');

insert into tag(nombre, img_url) values
    ('Pokémon', null),
    ('Dragon Ball', null),
    ('One Piece', null),
    ('Yu-Gi-Oh!', null),
    ('Magic: The Gathering', null);

insert into producto(id_categoria, id_tag, nombre, descripcion, precio, stock, img_url, descuento) values
    (1, 1, 'Sobre Evoluciones Prismáticas Pokémon', 'Sobre de 10 cartas de la colección Evoluciones Prismáticas', 8500.00, 50, null, 0),
    (2, 1, 'Caja Evoluciones Prismáticas Pokémon', 'Caja sellada de 36 sobres de Evoluciones Prismáticas', 45000.00, 10, null, 5),
    (3, 1, 'Baraja Liga Evoluciones Prismáticas', 'Baraja lista para jugar de la temporada Evoluciones Prismáticas', 12000.00, 20, null, 0),
    (1, 2, 'Sobre Battle Spirits Saga Dragon Ball', 'Sobre de 12 cartas de Battle Spirits Saga Dragon Ball', 6500.00, 40, null, 0),
    (2, 2, 'Caja Battle Spirits Saga Dragon Ball', 'Caja sellada de 24 sobres de Battle Spirits Saga', 35000.00, 8, null, 10),
    (1, 3, 'Sobre Romance Dawn One Piece', 'Sobre de 12 cartas de la colección Romance Dawn', 7500.00, 30, null, 0),
    (2, 3, 'Caja Romance Dawn One Piece', 'Caja sellada de 24 sobres de Romance Dawn', 40000.00, 5, null, 0),
    (1, 4, 'Sobre Rage of the Abyss Yu-Gi-Oh!', 'Sobre de 9 cartas de Rage of the Abyss', 5500.00, 60, null, 0),
    (1, 5, 'Sobre Innistrad Remastered Magic', 'Sobre de 15 cartas de Innistrad Remastered', 9000.00, 25, null, 0),
    (4, 1, 'Album Pokémon 9 Bolsillos', 'Álbum de colección con fundas para 9 cartas por página', 3500.00, 100, null, 0);
