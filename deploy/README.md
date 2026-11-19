# deploy/

Operación del SEA en el servidor: instalación, actualización, respaldos y restauración.

| Archivo | Para qué |
|---|---|
| `db/init/01-roles.sh` | Crea las cuentas de PostgreSQL sin superusuario en el primer arranque de la base |
| `respaldo.sh` | Respaldo cifrado de la base y de los documentos, con verificación |
| `restaurar.sh` | Restauración desde un respaldo |
| `exportar-demo.sh` | Exporta la base local a un `.sql` para montar un demo |
| `importar-demo.sh` | Carga ese `.sql` (y los documentos) en un servidor nuevo |
| `demo/convertir_legado_demo.py` | Convierte el volcado MySQL del sistema anterior en datos de demo con los tutores anonimizados |

> El volumen `sea_storage` contiene documentos personales (INE, CURP, comprobantes de
> domicilio). Los respaldos se cifran siempre; nunca se copian sin cifrar.

---

## 1. Instalación en un servidor nuevo

Requisitos: Docker y Docker Compose v2, Nginx Proxy Manager (NPM) funcionando con su red de Docker
(por omisión `npm_default`), `gpg` y `cron`.

```bash
# Bash (servidor)
git clone <url-del-repo> sea && cd sea
cp .env.example .env
chmod 600 .env
```

Llenar `.env`. Cada secreto se genera con un comando; ninguno se reutiliza:

| Variable | Cómo se genera |
|---|---|
| `POSTGRES_PASSWORD`, `DB_OWNER_PASSWORD`, `DB_APP_PASSWORD` | `openssl rand -base64 24` (una distinta para cada una) |
| `APP_JWT_SECRET` | `openssl rand -base64 64` |
| `APP_ADMIN_PASSWORD` | `openssl rand -base64 18` (mínimo 12 caracteres; se cambia en el primer ingreso) |
| `RESPALDO_CLAVE` | `openssl rand -base64 32` (guardar una copia fuera del servidor) |
| `APP_PROXIES_CONFIABLES` | ver la sección 3 |
| `APP_CORS_ALLOWED_ORIGINS` | el dominio público con `https://`, por ejemplo `https://padron.ejemplo.gob.mx`. **Obligatorio:** detrás de NPM el backend recibe la petición por HTTP y, si el dominio no está aquí, rechaza el inicio de sesión con 403 |

Levantar:

```bash
# Bash (servidor)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

En NPM, crear el *Proxy Host* del dominio con destino `sea-frontend:8080` y certificado SSL; el
Nginx del frontend reenvía `/api` al backend. Ningún servicio publica puertos en el servidor.

Primer ingreso: usuario `APP_ADMIN_USUARIO` con la contraseña `APP_ADMIN_PASSWORD`. El sistema obliga
a cambiarla antes de permitir cualquier otra operación.

## 2. Cuentas de la base

La aplicación nunca se conecta como superusuario:

| Cuenta | Variable | Uso |
|---|---|---|
| Superusuario | `POSTGRES_USER` | Solo inicializa el contenedor de la base |
| Dueña del esquema | `DB_OWNER_USER` | Flyway: crea y altera tablas |
| Aplicación | `DB_APP_USER` | Lee y escribe datos; no puede cambiar la estructura ni la bitácora |

`db/init/01-roles.sh` las crea **solo cuando el volumen de la base está vacío**. Si se instala sobre
una base existente, hay que crearlas a mano con el mismo SQL de ese archivo, ejecutado con
`docker exec -it sea-db psql -U <POSTGRES_USER> -d <POSTGRES_DB>`, y transferir la propiedad de las
tablas existentes a la dueña (`REASSIGN OWNED BY <usuario-anterior> TO <DB_OWNER_USER>;`).

La bitácora (`bitacora_auditoria`) es de solo inserción: un disparador rechaza `UPDATE`, `DELETE` y
`TRUNCATE`, incluso a la cuenta dueña.

## 3. IP real del cliente

El límite de peticiones, la bitácora y la evidencia del consentimiento usan la IP del cliente. El
backend solo cree la cabecera `X-Forwarded-For` que anotan los proxies de las subredes en
`APP_PROXIES_CONFIABLES` (formato CIDR, separadas por comas):

- `172.31.250.128/25`: red interna del proyecto (fija en `docker-compose.yml`).
- La subred de la red de NPM:

  ```bash
  # Bash (servidor)
  docker network inspect npm_default -f '{{range .IPAM.Config}}{{.Subnet}}{{end}}'
  ```

Ejemplo: `APP_PROXIES_CONFIABLES=127.0.0.1/32,172.31.250.128/25,172.18.0.0/16`.

Si falta la subred de NPM, el sistema sigue sin poder engañarse, pero ve a todos los clientes con la
IP de NPM y comparten el mismo límite de peticiones.

Si la subred `172.31.250.0/24` ya está ocupada en el servidor, se cambia en `docker-compose.yml`
(`networks.sea_interna.ipam`) y en `APP_PROXIES_CONFIABLES`.

## 4. Actualización

```bash
# Bash (servidor)
cd sea
deploy/respaldo.sh                       # respaldo previo, siempre
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f backend   # Flyway aplica las migraciones nuevas
```

Si la versión nueva agrega variables, aparecen en `.env.example`; se copian a `.env` con valores
reales antes de levantar. Las migraciones ya aplicadas nunca se editan.

## 5. Respaldos

`respaldo.sh` genera en `RESPALDO_DIR`, con permisos solo para el dueño:

- `sea-db-AAAAMMDD-HHMM.dump.gpg`: volcado de PostgreSQL, cifrado con AES-256.
- `sea-documentos-AAAAMMDD-HHMM.tar.gz.gpg`: volumen de documentos, cifrado con AES-256.
- Un `.sha256` por archivo.

Borra los respaldos con más de `RESPALDO_DIAS` días. La clave `RESPALDO_CLAVE` debe guardarse
**también fuera del servidor**: sin ella los respaldos no se pueden abrir.

Programarlo todos los días a las 2:00 (`crontab -e` del usuario que opera Docker):

```cron
0 2 * * * /ruta/a/sea/deploy/respaldo.sh >> /var/log/sea-respaldo.log 2>&1
30 2 * * 0 /ruta/a/sea/deploy/respaldo.sh --verificar >> /var/log/sea-respaldo.log 2>&1
```

La segunda línea comprueba cada domingo que el último respaldo se descifra y es legible. Además,
conviene copiar `RESPALDO_DIR` a otro equipo (por ejemplo con `rsync`): los archivos ya van cifrados.

## 6. Restauración

```bash
# Bash (servidor)
deploy/restaurar.sh /var/backups/sea/sea-db-AAAAMMDD-HHMM.dump.gpg \
                    /var/backups/sea/sea-documentos-AAAAMMDD-HHMM.tar.gz.gpg
```

Comprueba las sumas, pide escribir `RESTAURAR`, detiene el backend, reemplaza la base y los
documentos, y vuelve a arrancar. En un servidor nuevo, primero se instala (sección 1) para que
existan las cuentas de la base, y después se restaura.

Se recomienda ensayar la restauración en un equipo de pruebas al menos cada tres meses.

## 7. Montar un demo con la base local

Para presentar el sistema con datos capturados en local. No sustituye a los respaldos: el `.sql` no
va cifrado, no lleva bitácora ni sesiones, omite las cuentas dadas de baja sin uso y reinicia los
contadores de intentos fallidos.

**En local**, después de capturar los datos del demo:

```bash
# Bash (local)
deploy/exportar-demo.sh            # deja los archivos en ~/respaldos-sea/
```

Genera `sea-demo-AAAAMMDD-HHMM.sql` y, si hay documentos o fotos subidos,
`sea-demo-documentos-AAAAMMDD-HHMM.tar.gz`. Contienen hashes de contraseñas y datos capturados:
se copian al servidor por `scp` y **nunca** se suben a git.

**En el servidor nuevo** (base vacía), después de llenar `.env` como en la sección 1. Los nombres
`DB_OWNER_USER` y `DB_APP_USER` deben ser los mismos que en local (`sea_owner` y `sea_app`):

```bash
# Bash (servidor)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d db
deploy/importar-demo.sh sea-demo-AAAAMMDD-HHMM.sql
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
deploy/importar-demo.sh --documentos sea-demo-documentos-AAAAMMDD-HHMM.tar.gz   # solo si existe
```

Las cuentas del demo conservan las contraseñas que tenían en local: `APP_ADMIN_PASSWORD` del `.env`
del servidor ya no se usa, porque solo crea al administrador cuando la base no tiene cuentas.

### Datos del sistema anterior para la demo

`demo/convertir_legado_demo.py` toma el volcado MySQL del sistema anterior de empadronamiento y genera
un script de carga para el SEA con los **datos reales**, para cotejarlos con la documentación:

- los ciudadanos registrados (tengan o no mascota) con nombre separado en nombre y apellidos, correo,
  teléfono, domicilio y municipio;
- las mascotas con su clave original, los trámites con sus folios, los certificados (incluido el
  cancelado), las correcciones en el expediente y los avisos enviados por correo.

La CURP no se guardaba como dato en el sistema anterior (solo el documento escaneado): queda vacía y
la persona aparece con identidad pendiente de cotejo. No se importan los administradores, las
contraseñas ni los archivos de documentos del sistema anterior. Con `--anonimizar` se generan nombres,
correos, teléfonos, domicilios y CURP ficticios, para presentaciones fuera de la institución.

```bash
# Bash (local), con la base sin tutores
python3 deploy/demo/convertir_legado_demo.py empadronamiento.sql ~/respaldos-sea/legado-demo-carga.sql
docker exec -i sea-db psql -U <POSTGRES_USER> -d <POSTGRES_DB> -v ON_ERROR_STOP=1 < ~/respaldos-sea/legado-demo-carga.sql
deploy/exportar-demo.sh    # genera el .sql para el servidor (sección 7)
```

El volcado del sistema anterior y los `.sql` generados contienen datos personales reales: se guardan
fuera del proyecto, se copian al servidor por `scp` y nunca se suben a git (`empadronamiento*.sql`,
`empadronamiento*.xlsx` y `sea-demo-*.sql` están en `.gitignore`).
