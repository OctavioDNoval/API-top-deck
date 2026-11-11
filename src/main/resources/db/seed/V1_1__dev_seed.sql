-- DEV ONLY demo data: categories, tags, products and a default ADMIN.
-- Loaded only when spring.flyway.locations includes classpath:db/seed (never in
-- the prod profile). The admin password hash is public in this repo: change it.
-- Skipped when the database already has categories, so it never duplicates data.
DO $$
BEGIN
IF NOT EXISTS (SELECT 1 FROM categoria) THEN

    INSERT INTO categoria(nombre) VALUES
        ('Sobres'),
        ('Cajas'),
        ('Barajas'),
        ('Accesorios'),
        ('Figuras');

    INSERT INTO tag(nombre, img_url) VALUES
        ('Pokémon', 'https://placehold.co/600x600?text=Pokemon'),
        ('Dragon Ball Super', 'https://placehold.co/600x600?text=Dragon+Ball'),
        ('One Piece', 'https://placehold.co/600x600?text=One+Piece'),
        ('Yu-Gi-Oh!', 'https://placehold.co/600x600?text=Yu-Gi-Oh'),
        ('Magic: The Gathering', 'https://placehold.co/600x600?text=Magic');

    UPDATE tag SET nombre_normalizado = replace(replace(replace(replace(replace(replace(replace(replace(
        lower(nombre),
        'á','a'),'é','e'),'í','i'),'ó','o'),'ú','u'),'ü','u'),'ñ','n'),'-','')
    WHERE nombre_normalizado IS NULL;

    INSERT INTO producto(
        id_categoria, id_tag, nombre, descripcion, precio, stock, img_url, descuento
    ) VALUES
        (1, 1, 'Sobre Evoluciones Prismáticas Pokémon',
         'Sobre de 10 cartas de la colección Evoluciones Prismáticas',
         8500.00, 50, 'https://placehold.co/600x600?text=Sobre+Pokemon', 0),
        (2, 1, 'Caja Evoluciones Prismáticas Pokémon',
         'Caja sellada de 36 sobres de Evoluciones Prismáticas',
         45000.00, 10, 'https://placehold.co/600x600?text=Caja+Pokemon', 5),
        (3, 1, 'Baraja Liga Evoluciones Prismáticas',
         'Baraja lista para jugar de la temporada Evoluciones Prismáticas',
         12000.00, 20, 'https://placehold.co/600x600?text=Baraja+Pokemon', 0),
        (1, 2, 'Sobre Battle Spirits Saga Dragon Ball',
         'Sobre de 12 cartas de Battle Spirits Saga Dragon Ball',
         6500.00, 40, 'https://placehold.co/600x600?text=Sobre+Dragon+Ball', 0),
        (2, 2, 'Caja Battle Spirits Saga Dragon Ball',
         'Caja sellada de 24 sobres de Battle Spirits Saga',
         35000.00, 8, 'https://placehold.co/600x600?text=Caja+Dragon+Ball', 10),
        (1, 3, 'Sobre Romance Dawn One Piece',
         'Sobre de 12 cartas de la colección Romance Dawn',
         7500.00, 30, 'https://placehold.co/600x600?text=Sobre+One+Piece', 0),
        (2, 3, 'Caja Romance Dawn One Piece',
         'Caja sellada de 24 sobres de Romance Dawn',
         40000.00, 5, 'https://placehold.co/600x600?text=Caja+One+Piece', 0),
        (1, 4, 'Sobre Rage of the Abyss Yu-Gi-Oh!',
         'Sobre de 9 cartas de Rage of the Abyss',
         5500.00, 60, 'https://placehold.co/600x600?text=Yu-Gi-Oh', 0),
        (1, 5, 'Sobre Innistrad Remastered Magic',
         'Sobre de 15 cartas de Innistrad Remastered',
         9000.00, 25, 'https://placehold.co/600x600?text=Magic', 0),
        (4, 1, 'Album Pokémon 9 Bolsillos',
         'Álbum de colección con fundas para 9 cartas por página',
         3500.00, 100, 'https://placehold.co/600x600?text=Album+Pokemon', 0);

    -- ═══════════════════════════════════════════
    -- Usuario administrador por defecto
    -- ═══════════════════════════════════════════

    INSERT INTO usuario (nombre, email, password, rol, terminos_aceptados) VALUES
        ('orick', 'odnr2004@gmail.com', '$2a$10$59LK7r4kI/igkLH1.anN.OSx5T6DjHiCKw87GeOmx4W8/Ow6D.KK6', 'ADMIN', TRUE);

END IF;
END $$;
