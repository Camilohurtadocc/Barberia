-- Crea una base de datos por microservicio (patron database-per-service).
--
-- Postgres ejecuta los scripts de /docker-entrypoint-initdb.d SOLO cuando el
-- directorio de datos esta vacio, es decir, la primera vez que se crea el
-- volumen. Si ya tenias el volumen 'postgres_data' de antes, este script NO se
-- ejecutara: usa  .\reiniciar-proyecto.ps1 -BorrarVolumenes  para recrearlo.

CREATE DATABASE barberia_servicios;
CREATE DATABASE barberia_barberos;
CREATE DATABASE barberia_citas;
CREATE DATABASE barberia_auth;
