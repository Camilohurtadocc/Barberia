-- Catalogo de servicios, tomado del mockup de referencia.
--
-- Se ejecuta en CADA arranque (spring.sql.init.mode=always), por eso todo el
-- bloque va condicionado a que la tabla este VACIA. Antes se comprobaba nombre a
-- nombre, pero eso reinsertaba cualquier servicio que el administrador hubiera
-- borrado a proposito desde el panel: el catalogo es suyo, no del arranque.

INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, tag)
SELECT * FROM (VALUES
    ('Corte Clásico',   'Tijera y máquina, lavado y peinado final',        25.0, 45, 'BESTSELLER'),
    ('Fade & Líneas',   'Degradado a piel con perfilado de líneas',        35.0, 60, 'TRENDING'),
    ('Barba Completa',  'Perfilado, toalla caliente y aceites',            20.0, 30, NULL),
    ('Corte + Barba',   'El combo completo: corte, barba y ritual',        50.0, 90, 'PREMIUM'),
    ('Tratamiento',     'Hidratación capilar y masaje',                    30.0, 45, NULL),
    ('Afeitado Navaja', 'Afeitado tradicional a navaja con toalla',        28.0, 40, 'RITUAL')
) AS v(nombre, descripcion, precio, duracion_minutos, tag)
WHERE NOT EXISTS (SELECT 1 FROM servicios);


-- Contenido editable de la portada. Una sola fila, id fijo = 1.
INSERT INTO configuracion_sitio (id, hero_titulo, hero_subtitulo, hero_imagen_url,
                                 sobre_imagen_url, sobre_texto, direccion, telefono, instagram)
SELECT 1,
       'EL MEJOR CORTE DE TU VIDA',
       'Más de 12 años definiendo el estilo de Bogotá. Fade, diseño y precisión en cada detalle.',
       'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=900&h=1000&fit=crop&auto=format',
       'https://images.unsplash.com/photo-1621645582931-d1d3e6564943?w=900&h=1000&fit=crop&auto=format',
       'Abrimos en 2012 con una idea simple: que salir de la barbería se sintiera como estrenar. Hoy seguimos igual, un corte a la vez.',
       'Cra 7 #85-32, Chapinero, Bogotá',
       '+57 310 111 2233',
       '@thebarbershop'
WHERE NOT EXISTS (SELECT 1 FROM configuracion_sitio);

-- Frases de la cinta animada.
INSERT INTO ticker_mensajes (configuracion_id, mensaje)
SELECT * FROM (VALUES
    (1, 'CORTES'), (1, 'BARBA'), (1, 'FADE'), (1, 'DISEÑOS'),
    (1, 'TRATAMIENTOS'), (1, 'AFEITADO NAVAJA'), (1, 'BOGOTÁ'), (1, 'EST. 2012')
) AS v(configuracion_id, mensaje)
WHERE NOT EXISTS (SELECT 1 FROM ticker_mensajes);

-- Galeria de trabajos de la pagina principal.
INSERT INTO portafolio (titulo, categoria, imagen_url, orden)
SELECT * FROM (VALUES
    ('Skin Fade',      'FADE',      'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=600&h=800&fit=crop&auto=format', 1),
    ('Tijera Clásica', 'CLASSIC',   'https://images.unsplash.com/photo-1647140655214-e4a2d914971f?w=600&h=500&fit=crop&auto=format', 2),
    ('The Chair',      'LIFESTYLE', 'https://images.unsplash.com/photo-1621645582931-d1d3e6564943?w=600&h=500&fit=crop&auto=format', 3),
    ('Detail Work',    'PRECISION', 'https://images.unsplash.com/photo-1593702275687-f8b402bf1fb5?w=600&h=800&fit=crop&auto=format', 4),
    ('Beard Art',      'BEARD',     'https://images.unsplash.com/photo-1517832606299-7ae9b720a186?w=600&h=500&fit=crop&auto=format', 5)
) AS v(titulo, categoria, imagen_url, orden)
WHERE NOT EXISTS (SELECT 1 FROM portafolio);
