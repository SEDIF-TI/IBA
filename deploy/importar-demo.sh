#!/usr/bin/env bash
# =============================================================================
# Carga en el servidor la base de demostración generada con
# deploy/exportar-demo.sh.
#
# Propósito
#   Montar un demo con lo capturado en local. Solo trabaja sobre una base VACÍA
#   para no mezclar el demo con datos existentes.
#
# Uso (en el servidor, desde la raíz del proyecto)
#   1. docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d db
#      (solo la base: el script de deploy/db/init crea las cuentas)
#   2. deploy/importar-demo.sh sea-demo-AAAAMMDD-HHMM.sql
#   3. docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
#   4. Solo si se exportaron documentos (con el backend ya arrancado):
#      deploy/importar-demo.sh --documentos sea-demo-documentos-AAAAMMDD-HHMM.tar.gz
#
# Variables que lee del .env de la raíz del proyecto
#   POSTGRES_USER, POSTGRES_DB, APP_STORAGE_PATH
#
# Efectos
#   Paso 2: carga el .sql en una sola transacción (si algo falla, la base queda
#   vacía como estaba). No arranca el backend: Flyway valida al arrancar y
#   encuentra las migraciones ya aplicadas.
#   Paso 4: extrae los documentos en el volumen del backend y los deja a nombre
#   del usuario sin privilegios del contenedor, para que pueda seguir escribiendo.
# =============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$RAIZ/.env"
CONTENEDOR_DB="sea-db"
IMAGEN_AUX="alpine:3.22@sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8"

CONTENEDOR_BACKEND="sea-backend"
USO="Uso: $0 <sea-demo-*.sql>   |   $0 --documentos <sea-demo-documentos-*.tar.gz>"
[ $# -ge 1 ] || { echo "$USO" >&2; exit 1; }

leer_env() {
    local valor
    valor="$(grep -E "^$1=" "$ENV_FILE" | tail -n 1 | cut -d= -f2-)"
    if [ -z "$valor" ] && [ -n "${2:-}" ]; then valor="$2"; fi
    if [ -z "$valor" ]; then echo "Falta $1 en $ENV_FILE" >&2; exit 1; fi
    printf '%s' "$valor"
}
POSTGRES_USER="$(leer_env POSTGRES_USER)"
POSTGRES_DB="$(leer_env POSTGRES_DB)"
APP_STORAGE_PATH="$(leer_env APP_STORAGE_PATH /var/sea/storage)"

# ---- Paso 4: documentos ----
if [ "$1" = "--documentos" ]; then
    [ $# -eq 2 ] || { echo "$USO" >&2; exit 1; }
    ARCHIVO_DOCS="$(realpath "$2")"
    docker ps --format '{{.Names}}' | grep -qx "$CONTENEDOR_BACKEND" \
        || { echo "Arranca primero el backend (paso 3): los documentos van en su volumen." >&2; exit 1; }
    DUENO="$(docker exec "$CONTENEDOR_BACKEND" id -u):$(docker exec "$CONTENEDOR_BACKEND" id -g)"
    docker run --rm -i --volumes-from "$CONTENEDOR_BACKEND" "$IMAGEN_AUX" \
        sh -c "tar -xzf - -C '$APP_STORAGE_PATH' && chown -R $DUENO '$APP_STORAGE_PATH'" < "$ARCHIVO_DOCS"
    echo "Documentos extraídos: $(docker exec "$CONTENEDOR_BACKEND" sh -c "find '$APP_STORAGE_PATH' -type f | wc -l") archivos."
    exit 0
fi

# ---- Paso 2: base de datos ----
ARCHIVO_SQL="$(realpath "$1")"

docker exec "$CONTENEDOR_DB" pg_isready -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    || { echo "El contenedor $CONTENEDOR_DB no está listo. Levántalo primero (paso 1)." >&2; exit 1; }

if docker ps --format '{{.Names}}' | grep -qx sea-backend; then
    echo "El backend está corriendo: deténlo antes de cargar el demo (docker stop sea-backend)." >&2
    exit 1
fi

TABLAS="$(docker exec "$CONTENEDOR_DB" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
    "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'")"
if [ "$TABLAS" != "0" ]; then
    echo "La base '$POSTGRES_DB' ya tiene $TABLAS tablas. El demo solo se carga en una base vacía." >&2
    echo "Si es un servidor nuevo sin datos que conservar: docker compose ... down -v y repetir el paso 1." >&2
    exit 1
fi

docker exec -i "$CONTENEDOR_DB" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v ON_ERROR_STOP=1 --single-transaction -q -o /dev/null < "$ARCHIVO_SQL"
echo "Base cargada: migración $(docker exec "$CONTENEDOR_DB" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
    "SELECT version || ' (' || description || ')' FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")."

echo "Listo. Arranca todo con: docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build"
