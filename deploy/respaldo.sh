#!/usr/bin/env bash
# =============================================================================
# Respaldo cifrado del SEA: base de datos y volumen de documentos personales.
#
# Propósito
#   Generar, en cada ejecución, dos archivos cifrados con GPG (AES-256) más su
#   suma SHA-256:
#     sea-db-AAAAMMDD-HHMM.dump.gpg          volcado de PostgreSQL (pg_dump -Fc)
#     sea-documentos-AAAAMMDD-HHMM.tar.gz.gpg contenido del volumen de documentos
#   Los documentos son INE, CURP y comprobantes de domicilio: un respaldo sin
#   cifrar sería una copia más de datos personales al alcance de cualquiera.
#
# Uso
#   deploy/respaldo.sh             genera un respaldo y borra los vencidos
#   deploy/respaldo.sh --verificar comprueba que el último respaldo se descifra y
#                                  que el volcado de la base es legible
#   Programado con cron (ver deploy/README.md, "Respaldos").
#
# Variables que lee del .env de la raíz del proyecto
#   POSTGRES_USER, POSTGRES_DB  cuenta y base dentro del contenedor sea-db
#   APP_STORAGE_PATH            ruta del volumen de documentos dentro de sea-backend
#   RESPALDO_DIR                carpeta destino en el servidor (fuera del proyecto)
#   RESPALDO_DIAS               días que se conservan los respaldos
#   RESPALDO_CLAVE              frase de cifrado; guardar una copia FUERA del servidor
#
# Efectos
#   Escribe en RESPALDO_DIR y borra ahí los respaldos con más de RESPALDO_DIAS
#   días. No detiene ningún servicio: pg_dump obtiene una foto consistente con
#   la base en uso. Nunca imprime la frase de cifrado.
# =============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$RAIZ/.env"
CONTENEDOR_DB="sea-db"
CONTENEDOR_BACKEND="sea-backend"
IMAGEN_AUX="alpine:3.22@sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8"

# Lee una variable del .env sin ejecutarlo (un .env no debe tratarse como código).
leer_env() {
    local valor
    valor="$(grep -E "^$1=" "$ENV_FILE" | tail -n 1 | cut -d= -f2-)"
    if [ -z "$valor" ] && [ -n "${2:-}" ]; then valor="$2"; fi
    if [ -z "$valor" ]; then echo "Falta $1 en $ENV_FILE" >&2; exit 1; fi
    printf '%s' "$valor"
}

[ -f "$ENV_FILE" ] || { echo "No existe $ENV_FILE" >&2; exit 1; }
POSTGRES_USER="$(leer_env POSTGRES_USER)"
POSTGRES_DB="$(leer_env POSTGRES_DB)"
APP_STORAGE_PATH="$(leer_env APP_STORAGE_PATH /var/sea/storage)"
RESPALDO_DIR="$(leer_env RESPALDO_DIR)"
RESPALDO_DIAS="$(leer_env RESPALDO_DIAS 30)"
RESPALDO_CLAVE="$(leer_env RESPALDO_CLAVE)"

case "$RESPALDO_CLAVE" in
    *cambiame*) echo "RESPALDO_CLAVE sigue con el valor de ejemplo: genera una con openssl rand -base64 32" >&2; exit 1 ;;
esac
[ "${#RESPALDO_CLAVE}" -ge 20 ] || { echo "RESPALDO_CLAVE debe tener al menos 20 caracteres" >&2; exit 1; }

# Cifra la entrada estándar hacia el archivo indicado; la clave entra por un descriptor, no por argumentos.
cifrar() {
    gpg --batch --yes --quiet --symmetric --cipher-algo AES256 \
        --passphrase-fd 3 --pinentry-mode loopback --output "$1" 3< <(printf '%s' "$RESPALDO_CLAVE")
}

# Descifra el archivo indicado hacia la salida estándar.
descifrar() {
    gpg --batch --quiet --decrypt --passphrase-fd 3 --pinentry-mode loopback "$1" \
        3< <(printf '%s' "$RESPALDO_CLAVE")
}

verificar() {
    local ultimo_db ultimo_docs
    ultimo_db="$(ls -1t "$RESPALDO_DIR"/sea-db-*.dump.gpg 2>/dev/null | head -n 1 || true)"
    ultimo_docs="$(ls -1t "$RESPALDO_DIR"/sea-documentos-*.tar.gz.gpg 2>/dev/null | head -n 1 || true)"
    [ -n "$ultimo_db" ] && [ -n "$ultimo_docs" ] || { echo "No hay respaldos en $RESPALDO_DIR" >&2; exit 1; }

    (cd "$RESPALDO_DIR" && sha256sum --quiet -c "$(basename "$ultimo_db").sha256" \
        && sha256sum --quiet -c "$(basename "$ultimo_docs").sha256")
    local tablas
    tablas="$(descifrar "$ultimo_db" | docker exec -i "$CONTENEDOR_DB" pg_restore --list | grep -c ' TABLE public ' || true)"
    local archivos
    archivos="$(descifrar "$ultimo_docs" | tar -tzf - | grep -vc '/$' || true)"
    echo "Verificado $(basename "$ultimo_db"): $tablas tablas en el volcado."
    echo "Verificado $(basename "$ultimo_docs"): $archivos archivos de documentos."
}

if [ "${1:-}" = "--verificar" ]; then
    verificar
    exit 0
fi

umask 077
mkdir -p "$RESPALDO_DIR"
FECHA="$(date +%Y%m%d-%H%M)"
DESTINO_DB="$RESPALDO_DIR/sea-db-$FECHA.dump.gpg"
DESTINO_DOCS="$RESPALDO_DIR/sea-documentos-$FECHA.tar.gz.gpg"

# Base de datos: formato personalizado (-Fc), restaurable por partes con pg_restore.
docker exec "$CONTENEDOR_DB" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc | cifrar "$DESTINO_DB"

# Documentos: se lee el volumen del backend en solo lectura desde un contenedor auxiliar.
docker run --rm --volumes-from "$CONTENEDOR_BACKEND:ro" "$IMAGEN_AUX" \
    tar -czf - -C "$APP_STORAGE_PATH" . | cifrar "$DESTINO_DOCS"

(cd "$RESPALDO_DIR" && sha256sum "$(basename "$DESTINO_DB")" > "$(basename "$DESTINO_DB").sha256" \
    && sha256sum "$(basename "$DESTINO_DOCS")" > "$(basename "$DESTINO_DOCS").sha256")

# Conservación: se borran los respaldos (y sus sumas) con más de RESPALDO_DIAS días.
find "$RESPALDO_DIR" -maxdepth 1 -type f -name 'sea-*' -mtime +"$RESPALDO_DIAS" -delete

echo "Respaldo listo: $(basename "$DESTINO_DB") ($(du -h "$DESTINO_DB" | cut -f1)), $(basename "$DESTINO_DOCS") ($(du -h "$DESTINO_DOCS" | cut -f1))"
