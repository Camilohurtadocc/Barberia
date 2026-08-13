-- Cuentas de acceso iniciales: el administrador y un usuario por barbero.
--
-- Se ejecuta en CADA arranque (spring.sql.init.mode=always), por eso va
-- condicionado a que la tabla este VACIA: si el administrador borra o le cambia
-- la clave a un barbero, el arranque siguiente no debe deshacerlo.
--
-- barbero_id apunta al id de la tabla barberos de barberia_barberos (1 Marcos,
-- 2 Santiago, 3 Diego). No es una clave foranea porque esa tabla vive en otra
-- base de datos; los valores se mantienen alineados a mano con el data.sql de
-- servicio-barberos, que tambien fija sus ids explicitamente.
--
-- Las contrasenas quedan en claro a proposito: es un proyecto de practica. Para
-- produccion hay que guardar el hash BCrypt y comparar en AuthController.

INSERT INTO usuarios (username, password, rol, nombre, barbero_id)
SELECT * FROM (VALUES
    ('admin',    'admin123',    'ADMIN',   'Administrador',   CAST(NULL AS BIGINT)),
    ('marcos',   'marcos123',   'BARBERO', 'Marcos Villegas', CAST(1 AS BIGINT)),
    ('santiago', 'santiago123', 'BARBERO', 'Santiago Ríos',   CAST(2 AS BIGINT)),
    ('diego',    'diego123',    'BARBERO', 'Diego Castillo',  CAST(3 AS BIGINT))
) AS v(username, password, rol, nombre, barbero_id)
WHERE NOT EXISTS (SELECT 1 FROM usuarios);
