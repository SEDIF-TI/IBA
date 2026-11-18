#!/usr/bin/env bash
# =============================================================================
# Restauración del SEA desde un respaldo cifrado de deploy/respaldo.sh.
#
# Propósito
#   Reponer la base de datos y el volumen de documentos a la fecha de un
#   respaldo, por ejemplo tras perder el servidor o para ensayar la recuperación.
#
# Uso
#   deploy/restaurar.sh <respaldo-db.dump.gpg> <respaldo-documentos.tar.gz.gpg>
#   Pide confirmación escribiendo RESTAURAR, porque reemplaza los datos actuales.
#
# Variables que lee del .env de la raíz del proyecto
#   POSTGRES_USER, POSTGRES_DB, APP_STORAGE_PATH, RESPALDO_CLAVE
#
# Efectos
#   1. Comprueba las sumas SHA-256 de ambos archivos.
#   2. Detiene el backend para que nadie escriba durante la restauración.
#   3. Restaura la base con pg_restore --clean (borra y recrea cada objeto,
#      conservando dueños y permisos de las cuentas sea_owner y sea_app, que
#      deben existir: las crea deploy/db/init/01-roles.sh).
#   4. Vacía el volumen de documentos y extrae el respaldo.
#   5. Vuelve a arrancar el backend.
#   La bitácora restaurada conserva sus disparadores de solo inserción.
# =============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$RAIZ/.env"
CONTENEDOR_DB="sea-db"
CONTENEDOR_BACKEND="sea-backend"
IMAGEN_AUX="alpine:3.22@sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8"

[ $# -eq 2 ] || { echo "Uso: $0 <sea-db-*.dump.gpg> <sea-documentos-*.tar.gz.gpg>" >&2; exit 1; }
RESPALDO_DB="$(realpath "$1")"
RESPALDO_DOCS="$(realpath "$2")"

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
RESPALDO_CLAVE="$(leer_env RESPALDO_CLAVE)"

descifrar() {
    gpg --batch --quiet --decrypt --passphrase-fd 3 --pinentry-mode loopback "$1" \
        3< <(printf '%s' "$RESPALDO_CLAVE")
}

for archivo in "$RESPALDO_DB" "$RESPALDO_DOCS"; do
    (cd "$(dirname "$archivo")" && sha256sum --quiet -c "$(basename "$archivo").sha256") \
        || { echo "La suma SHA-256 de $archivo no coincide: el archivo está dañado o fue alterado." >&2; exit 1; }
done

echo "Se reemplazarán la base '$POSTGRES_DB' y el volumen de documentos por el respaldo:"
echo "  $(basename "$RESPALDO_DB")"
echo "  $(basename "$RESPALDO_DOCS")"
read -r -p "Escribe RESTAURAR para continuar: " confirmacion
[ "$confirmacion" = "RESTAURAR" ] || { echo "Cancelado."; exit 1; }

docker stop "$CONTENEDOR_BACKEND" >/dev/null
trap 'docker start "$CONTENEDOR_BACKEND" >/dev/null' EXIT

descifrar "$RESPALDO_DB" | docker exec -i "$CONTENEDOR_DB" \
    pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --exit-on-error

descifrar "$RESPALDO_DOCS" | docker run --rm -i --volumes-from "$CONTENEDOR_BACKEND" "$IMAGEN_AUX" \
    sh -c "find '$APP_STORAGE_PATH' -mindepth 1 -delete && tar -xzf - -C '$APP_STORAGE_PATH'"

echo "Restauración terminada. El backend se reinicia."
