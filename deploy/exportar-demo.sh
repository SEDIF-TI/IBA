#!/usr/bin/env bash
# =============================================================================
# Exporta la base local del SEA a un archivo .sql para montar un DEMO en otro
# servidor.
#
# Propósito
#   Llevar al servidor de demostración lo que se capturó en local (trámites,
#   tutores, animales, jornadas, certificados, catálogos y cuentas) sin el rastro
#   de pruebas: el archivo no lleva datos de bitácora ni sesiones abiertas, omite
#   las cuentas dadas de baja que ningún registro usa y reinicia los contadores de
#   intentos fallidos. NO sustituye a deploy/respaldo.sh: aquel es el respaldo
#   cifrado y completo de un servidor real.
#
# Uso
#   deploy/exportar-demo.sh [carpeta-destino]      (por omisión ~/respaldos-sea)
#   Genera:
#     sea-demo-AAAAMMDD-HHMM.sql               estructura + datos (texto plano)
#     sea-demo-documentos-AAAAMMDD-HHMM.tar.gz solo si hay documentos subidos
#   Se carga en el servidor con deploy/importar-demo.sh (ver deploy/README.md).
#
# Variables que lee del .env de la raíz del proyecto
#   POSTGRES_USER, POSTGRES_DB, DB_OWNER_USER, APP_STORAGE_PATH
#
# Efectos
#   Crea y borra una base temporal (sea_demo_exportacion) dentro del contenedor
#   sea-db para depurar los datos sin tocar la base local. No modifica la base
#   local. El .sql contiene hashes de contraseñas y los datos capturados: se
#   escribe con permisos solo para el dueño y nunca debe subirse a git.
# =============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$RAIZ/.env"
CONTENEDOR_DB="sea-db"
CONTENEDOR_BACKEND="sea-backend"
IMAGEN_AUX="alpine:3.22@sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8"
BASE_TEMPORAL="sea_demo_exportacion"

leer_env() {
    local valor
    valor="$(grep -E "^$1=" "$ENV_FILE" | tail -n 1 | cut -d= -f2-)"
    if [ -z "$valor" ] && [ -n "${2:-}" ]; then valor="$2"; fi
    if [ -z "$valor" ]; then echo "Falta $1 en $ENV_FILE" >&2; exit 1; fi
    printf '%s' "$valor"
}

POSTGRES_USER="$(leer_env POSTGRES_USER)"
POSTGRES_DB="$(leer_env POSTGRES_DB)"
DB_OWNER_USER="$(leer_env DB_OWNER_USER)"
APP_STORAGE_PATH="$(leer_env APP_STORAGE_PATH /var/sea/storage)"

DESTINO="${1:-$HOME/respaldos-sea}"
umask 077
mkdir -p "$DESTINO"
FECHA="$(date +%Y%m%d-%H%M)"
SALIDA_SQL="$DESTINO/sea-demo-$FECHA.sql"
SALIDA_DOCS="$DESTINO/sea-demo-documentos-$FECHA.tar.gz"

psql_super() {
    docker exec -i "$CONTENEDOR_DB" psql -v ON_ERROR_STOP=1 -q -U "$POSTGRES_USER" "$@"
}

limpiar() {
    docker exec "$CONTENEDOR_DB" dropdb -U "$POSTGRES_USER" --if-exists "$BASE_TEMPORAL" >/dev/null 2>&1 || true
}
trap limpiar EXIT

# 1. Copia de trabajo sin datos de bitácora ni sesiones.
limpiar
docker exec "$CONTENEDOR_DB" createdb -U "$POSTGRES_USER" -O "$DB_OWNER_USER" "$BASE_TEMPORAL"
docker exec "$CONTENEDOR_DB" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc \
        --exclude-table-data=bitacora_auditoria --exclude-table-data=sesion_usuario \
    | docker exec -i "$CONTENEDOR_DB" pg_restore -U "$POSTGRES_USER" -d "$BASE_TEMPORAL" --exit-on-error

# 2. Depuración en la copia: cuentas dadas de baja sin uso y contadores de bloqueo.
psql_super -d "$BASE_TEMPORAL" <<'SQL'
DO $$
DECLARE
    cuenta RECORD;
BEGIN
    FOR cuenta IN SELECT pn_id FROM usuario WHERE b_activo = FALSE LOOP
        BEGIN
            DELETE FROM usuario_rol WHERE fn_usuario_id = cuenta.pn_id;
            DELETE FROM usuario WHERE pn_id = cuenta.pn_id;
        EXCEPTION WHEN foreign_key_violation THEN
            -- La cuenta aparece en registros (resolvió trámites, capturó datos): se conserva.
            NULL;
        END;
    END LOOP;
END $$;
UPDATE usuario SET n_intentos_fallidos = 0, d_bloqueo_hasta = NULL, n_token_version = 0;
SQL

# 3. Volcado en texto plano, con encabezado de instrucciones.
{
    cat <<EOF
-- =============================================================================
-- SEA - Base de demostración exportada el $(date '+%Y-%m-%d %H:%M') desde el entorno local.
--
-- Contiene la estructura completa (migraciones hasta la última versión) y los
-- datos capturados, sin bitácora ni sesiones. Incluye hashes de contraseñas:
-- NO se sube a git ni se comparte fuera del equipo.
--
-- Cargar en una base VACÍA con deploy/importar-demo.sh (deploy/README.md,
-- "Montar un demo con la base local"). Las cuentas $DB_OWNER_USER y la de la
-- aplicación deben existir con esos mismos nombres en el servidor.
-- =============================================================================

EOF
    docker exec "$CONTENEDOR_DB" pg_dump -U "$POSTGRES_USER" -d "$BASE_TEMPORAL" --format=plain
} > "$SALIDA_SQL"

# 4. Documentos subidos (fotos, INE, comprobantes), si hay.
ARCHIVOS="$(docker exec "$CONTENEDOR_BACKEND" sh -c "find '$APP_STORAGE_PATH' -type f | wc -l")"
if [ "$ARCHIVOS" -gt 0 ]; then
    docker run --rm --volumes-from "$CONTENEDOR_BACKEND:ro" "$IMAGEN_AUX" \
        tar -czf - -C "$APP_STORAGE_PATH" . > "$SALIDA_DOCS"
    echo "Documentos: $SALIDA_DOCS ($ARCHIVOS archivos)"
else
    echo "Documentos: no hay archivos subidos; no se genera el paquete."
fi

TABLAS="$(grep -c '^CREATE TABLE ' "$SALIDA_SQL" || true)"
echo "Base: $SALIDA_SQL ($(du -h "$SALIDA_SQL" | cut -f1), $TABLAS tablas)"
