#!/bin/sh
# =============================================================================
# Cuentas de PostgreSQL del SEA (primer arranque de la base).
#
# Propósito
#   Que la aplicación nunca se conecte como superusuario. Crea dos cuentas sin
#   privilegios especiales:
#     - DB_OWNER_USER: dueña de la base y del esquema public. Solo la usa Flyway
#       para crear y alterar tablas (DDL).
#     - DB_APP_USER: la que usa la aplicación en cada petición. Solo puede leer,
#       insertar, actualizar y borrar datos (DML); no puede crear ni borrar tablas,
#       ni desactivar los disparadores de la bitácora.
#   El superusuario (POSTGRES_USER) queda solo dentro del contenedor de la base.
#
# Uso
#   La imagen oficial de PostgreSQL ejecuta este archivo UNA sola vez, cuando el
#   volumen de datos está vacío (montado en /docker-entrypoint-initdb.d). Para una
#   base que ya existe, ver deploy/README.md, sección "Cuentas de la base".
#
# Variables que lee (vienen de .env a través de docker-compose.yml)
#   POSTGRES_USER, POSTGRES_DB, DB_OWNER_USER, DB_OWNER_PASSWORD,
#   DB_APP_USER, DB_APP_PASSWORD
#
# Efectos
#   Crea las dos cuentas, transfiere la base y el esquema public a la dueña,
#   retira a PUBLIC los permisos de conexión y de creación, y deja permisos por
#   omisión para que toda tabla que cree Flyway quede disponible para la
#   aplicación solo con DML.
# =============================================================================
set -eu

: "${DB_OWNER_USER:?Falta DB_OWNER_USER en .env}"
: "${DB_OWNER_PASSWORD:?Falta DB_OWNER_PASSWORD en .env}"
: "${DB_APP_USER:?Falta DB_APP_USER en .env}"
: "${DB_APP_PASSWORD:?Falta DB_APP_PASSWORD en .env}"

# Las contraseñas viajan como variables de psql (:'variable'), que las cita de forma
# segura; nunca se concatenan dentro del SQL.
psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v owner="$DB_OWNER_USER" -v owner_pass="$DB_OWNER_PASSWORD" \
     -v app="$DB_APP_USER" -v app_pass="$DB_APP_PASSWORD" \
     -v db="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"owner" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION
    PASSWORD :'owner_pass';
CREATE ROLE :"app" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION
    PASSWORD :'app_pass';

-- La dueña puede crear las extensiones de confianza (pgcrypto, pg_trgm, unaccent).
ALTER DATABASE :"db" OWNER TO :"owner";
ALTER SCHEMA public OWNER TO :"owner";

-- Nadie más se conecta ni crea objetos.
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO :"owner", :"app";
GRANT USAGE ON SCHEMA public TO :"app";

-- Lo que cree la dueña (vía Flyway) queda disponible para la aplicación solo con DML.
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner" IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app";
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner" IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO :"app";
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner" IN SCHEMA public
    GRANT EXECUTE ON FUNCTIONS TO :"app";
SQL

echo "Cuentas de la base creadas: dueña '$DB_OWNER_USER' y aplicación '$DB_APP_USER' (sin superusuario)."
