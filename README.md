# SEA — Sistema de Empadronamiento Animal

Padrón Estatal de Perros y Gatos del **Instituto de Bienestar Animal (IBA)**,
operado por el **Sistema Estatal DIF Puebla**.

Registra seres sintientes (perros y gatos) junto con su tutor responsable y
expide tres documentos oficiales: **certificado de empadronamiento**,
**credencial** ("Credencial Peluda") y **acuse** de trámite.

---

## Arranque desde cero

### Requisitos
- Docker y Docker Compose v2
- WSL/Ubuntu (el proyecto vive bajo `~/dev`)
- Git con `core.autocrlf=input`

### 1. Clonar y configurar

```bash
# Bash (WSL/Ubuntu)
git clone <url-del-repo> sea
cd sea
cp .env.example .env
```

### 2. Generar los secretos

Cada secreto se genera; ninguno se reutiliza ni se deja con el valor de ejemplo:

| Variable de `.env` | Comando |
|---|---|
| `APP_JWT_SECRET` | `openssl rand -base64 64` |
| `POSTGRES_PASSWORD`, `DB_OWNER_PASSWORD`, `DB_APP_PASSWORD` | `openssl rand -base64 24` (una distinta cada una) |
| `APP_ADMIN_PASSWORD` | `openssl rand -base64 18` (mínimo 12 caracteres) |
| `RESPALDO_CLAVE` | `openssl rand -base64 32` |

El arranque **falla a propósito** si `APP_JWT_SECRET` es corto, de baja entropía
o sigue siendo el valor de ejemplo.

La aplicación nunca se conecta a la base como superusuario: en el primer
arranque, `deploy/db/init/01-roles.sh` crea la cuenta dueña del esquema (la usa
Flyway) y la cuenta de la aplicación (solo lee y escribe datos). Ver
`deploy/README.md`.

### 3. Levantar

```bash
# Bash — desarrollo local
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

| Servicio   | URL local               |
|------------|-------------------------|
| Frontend   | http://localhost        |
| Backend    | http://localhost:8080/api |
| PostgreSQL | `localhost:5433`        |

> El puerto **5433** es intencional: evita chocar con un PostgreSQL instalado
> directamente en la máquina, que ocupa el 5432.
>
> Si otro proyecto ya ocupa esos puertos, se cambian solo para esa ejecución:
> `FRONTEND_PORT_HOST=8088 BACKEND_PORT_HOST=8089 POSTGRES_PORT_HOST=5434 docker compose ...`

### Pruebas y revisión de código

```bash
# Bash — desde la raíz del proyecto
./backend/mvnw -f backend/pom.xml test     # unitarias + integracion (requiere Docker encendido)
cd frontend && npm run lint                # ESLint, 0 errores y 0 advertencias
cd frontend && npm run build               # tipos de TypeScript + compilacion de Vite
```

Las pruebas de integración levantan un PostgreSQL real con Testcontainers,
ejecutan el mismo script de cuentas de `deploy/db/init/` y generan al arrancar
sus propias contraseñas y su secreto JWT: no hay secretos escritos en el código.

> Si el editor (extensión de Java de VS Code) compila al mismo tiempo en
> `backend/target`, Maven puede fallar con `NoClassDefFoundError`. Se resuelve
> con `./backend/mvnw -f backend/pom.xml clean test`.

### 4. Recorrer el sistema

Con todo levantado:

| Que | Donde |
|---|---|
| Portal ciudadano | http://localhost |
| Registrar un tramite | http://localhost/registro |
| Consultar por folio y CURP | http://localhost/consulta |
| Verificar un certificado | http://localhost/verificar |
| Jornadas programadas | http://localhost/jornadas |
| Panel administrativo | http://localhost/admin/login |

**Flujo completo para probarlo de punta a punta:**

1. Entra al portal y registra un tramite. Anota el folio que devuelve.
2. Entra al panel, abre **Tramites**, y busca el folio.
3. Toma el tramite para revision y apruebalo. El certificado se emite solo.
4. Descarga el certificado y la credencial desde el detalle del tramite.
5. Abre el PDF del certificado y escanea su codigo QR: te lleva al verificador
   publico, que confirma la autenticidad sin mostrar datos del tutor.
6. Si registraste sin correo, revisa **Bandeja de contacto**: ahi esta la
   persona a la que hay que llamar.

**Para probar una jornada:**

1. En **Jornadas**, crea una con la fecha de hoy y publicala.
2. Desde el portal, entra a **Jornadas** y adelanta un registro.
3. Vuelve al panel, abre la jornada con **Operar**, y busca a la persona por
   apellido (puedes escribirlo sin acentos).
4. Valida el tramite en sitio, marca la casilla e imprime la credencial:
   salen tres por hoja con sus marcas de corte.

### 5. Primer acceso

El usuario inicial se crea desde `APP_ADMIN_USUARIO` / `APP_ADMIN_PASSWORD` con
la marca `b_cambio_password_requerido = true`: el sistema **obliga a cambiar la
contraseña en el primer login**. No existe ningún usuario con contraseña por
defecto conocida.

### Que hace el sistema

**Portal ciudadano, sin cuenta de usuario**
- Registro de tramite en un solo envio, con folio de seguimiento.
- Consulta del avance con folio y CURP.
- Verificacion publica de certificados por codigo QR, sin datos del tutor.
- Calendario de jornadas municipales.

**Panel administrativo**
- Bandeja de tramites con filtros por situacion, municipio y fechas.
- Detalle a dos columnas para cotejar lo capturado contra lo adjuntado.
- Resolucion con motivo de catalogo: aprobar, devolver para correccion,
  rechazar o cancelar. Toda transicion queda en el expediente.
- Emision automatica del certificado al aprobar.
- CRUD de los trece catalogos administrables.
- Jornadas municipales con vista de operacion en sitio e impresion por lote.
- Bandeja de contacto para las personas que solo dejaron telefono.

**Documentos**
- Certificado de empadronamiento en tamano carta, con foto del animal, codigo
  QR de verificacion y firma de quien autoriza.
- Credencial Peluda de dos caras, tres por hoja con marcas de corte.
- Acuse de recepcion del tramite.

Los colores, textos, leyendas, patrones de folio y datos de quien firma se
editan desde la tabla de parametros, sin recompilar.

### Configuracion pendiente

Estas cosas quedan listas pero requieren una decision o un dato del area:

| Que | Como se resuelve |
|---|---|
| Envio real de correo | Configurar `APP_MAIL_*` en `.env` y poner `APP_MAIL_ENABLED=true`. Mientras tanto, todos los avisos caen en la bandeja de contacto. |
| Firma electronica avanzada | `FirmaService` es enchufable. Hoy opera la implementacion que no firma y lo hace constar en el documento. |
| Colores de los documentos | Parametros del grupo `documento`. Por omision usan el guinda institucional. |
| Aviso de privacidad | El sembrado es un texto minimo marcado como pendiente. Debe sustituirlo el area juridica. |
| Logotipos institucionales | Los documentos rotulan los nombres. Para incrustar las imagenes hacen falta los archivos originales. |
| Costo del tramite y tiempo de respuesta | Sembrados en el contenido del portal pero sin publicar, a la espera de que el IBA los defina. |

### Despliegue en servidor

```bash
# Bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

No publica puertos: el acceso entra por el reverse proxy (NPM) conectado a la
red externa definida en `PROXY_NETWORK`. Instalación, actualización, IP real del
cliente, respaldos y restauración: `deploy/README.md`.

### Actualizar imágenes base

Las imágenes de los `Dockerfile` y de `docker-compose.yml` van fijadas por
digest (`imagen:etiqueta@sha256:...`): la etiqueta puede cambiar de contenido,
el digest no. Para actualizar una:

```bash
# Bash
docker pull nginx:1.27-alpine
docker image inspect --format '{{index .RepoDigests 0}}' nginx:1.27-alpine
```

Se copia el digest nuevo al archivo correspondiente, se reconstruye y se corren
las pruebas.

---

## Stack

| Capa           | Tecnología                                   |
|----------------|----------------------------------------------|
| Backend        | Java 21 + Spring Boot 3.5 (Maven)            |
| Persistencia   | Spring Data JPA / Hibernate + PostgreSQL 16  |
| Migraciones    | Flyway (SQL versionado)                      |
| Seguridad      | Spring Security + JWT                        |
| Frontend       | React 18 + Vite + TypeScript + Material UI   |
| Contenedores   | Docker + Docker Compose                      |

---

## Reglas del proyecto

### Base de datos
- `spring.jpa.hibernate.ddl-auto=validate`. **Hibernate nunca genera ni
  modifica el esquema.** Toda la estructura vive en migraciones Flyway.
- Nunca se edita una migración ya aplicada: solo se agrega una nueva `Vn__`.
- Nomenclatura: tablas en **singular**, `snake_case`, sin acentos ni ñ.
  Prefijos de columna: `pn_` (PK), `fn_` (FK), `s_` (texto), `n_` (numérico),
  `b_` (booleano), `d_` (fecha), `j_` (JSONB).
- Borrado **lógico** (`b_activo`), nunca físico.
- La **CURP nunca es llave primaria ni foránea**: va como columna con índice
  único parcial. RENAPO emite correcciones y no se propaga el cambio.

### Organización del código
El backend se organiza **por dominio/feature**, no por capa técnica. Cada
feature vive en `core/<feature>/` con todas sus clases juntas:

```
core/persona/
├── Persona.java            @Entity
├── PersonaRepository.java  @Repository
├── PersonaService.java     @Service
├── PersonaResource.java    @RestController  (sufijo Resource, no Controller)
├── PersonaRecord.java      DTO de salida (record)
└── CrearPersonaRequest.java DTO de entrada (record)
```

No existen carpetas `controllers/`, `services/` ni `repositories/` separadas.

En el frontend, ningún componente o página llama a `axios` directamente:
**toda** llamada al backend pasa por un servicio de `src/services/`.

### Seguridad
- Los documentos personales (INE, CURP, comprobante) se guardan **fuera del
  document root**, con nombre UUID, y se entregan solo por endpoint autenticado
  que valida rol y deja registro en bitácora.
- La CURP se muestra **enmascarada** en listados y **nunca** aparece en el
  verificador público.
- Cada consulta por CURP queda registrada en `bitacora_auditoria`.
- Sin credenciales en el repositorio: todo por variables de entorno.
- **Sesión:** token de acceso de 1 hora solo en memoria del navegador; token de
  renovación de 7 días en cookie `HttpOnly`, `Secure` y `SameSite=Strict` (prod),
  que rota en cada uso. Reutilizar uno ya usado cierra la sesión. Cerrar sesión,
  cambiar o reiniciar la contraseña y dar de baja la cuenta invalidan los tokens
  al instante (tabla `sesion_usuario` y columna `usuario.n_token_version`).
- **Contraseña temporal:** la genera el sistema, se muestra una vez y, mientras no
  se cambie, el servidor solo permite cambiarla (403 en todo lo demás).
- **Límite de peticiones** por IP y ruta en login, renovación y portal público.
  La IP real la resuelve `IpRealFilter`, que solo cree a los proxies de
  `APP_PROXIES_CONFIABLES`.
- **Bitácora de todos los movimientos:** cada petición al API (consultas, altas,
  ediciones, bajas, accesos denegados y errores) más los eventos de sesión, con
  usuario, IP, navegador y resultado. Es de solo inserción (disparador en la
  base) y solo la consulta informática (`bitacora:ver`). De una búsqueda se
  guardan los nombres de los filtros, nunca los valores.
- **Validación de campos con formato, en un solo lugar:** CURP (estructura, fecha, entidad y
  dígito verificador), correo (usuario, arroba y dominio con terminación), teléfono (10 dígitos) y
  código postal (5 dígitos). En el navegador, `frontend/src/utils/validaciones.ts`: los campos
  numéricos descartan letras al escribir y abren el teclado numérico, y la CURP solo admite letras y
  números. En el servidor, las anotaciones de `util/validacion/` (`@CurpValida`, `@Correo`,
  `@Telefono`, `@CodigoPostal`); `ValidacionCamposTest` falla si un `*Request` tiene uno de esos
  campos sin su anotación.
- **Edición concurrente:** toda tabla editable lleva `n_version`; guardar con una
  versión vieja responde 409 en lugar de pisar el cambio de otra persona.

### Commits
En español, formato convencional: `feat:`, `fix:`, `refactor:`, `docs:`.

---

## Desviaciones del estándar

Diferencias con `ESTANDAR-DESARROLLO-Y-REVISION.md` (v1.1), justificadas:

| Desviación | Motivo |
|---|---|
| Spring Boot 3.5 en lugar de 4.x | Proyecto existente; el estándar admite versiones anteriores documentadas. La migración a 4.x (Jackson 3, Hibernate 7, Spring Security 7) se hará como trabajo aparte, con su propia ronda de pruebas. |
| Frontend en TypeScript (`.tsx`) en lugar de JavaScript | Da revisión de tipos en compilación; no cambia la estructura de carpetas del estándar. |
| CSP con `style-src 'unsafe-inline'` | Material UI (Emotion) inserta estilos en tiempo de ejecución. Los scripts no llevan `unsafe-inline` y no se carga nada de dominios externos. |
| Datos personales sin cifrar en la base | Decisión del responsable del proyecto (17-11-2026). Se compensa con acceso restringido por permiso, enmascarado en listados, bitácora de cada consulta y respaldos cifrados. |
| Sin alcance jerárquico de datos | Decisión del responsable (17-11-2026): todo el personal ve el padrón estatal completo. Los permisos por rol sí se validan en cada endpoint. |
| IP real con un filtro propio (`IpRealFilter`) y no con `server.forward-headers-strategy` | `framework` cree el primer valor de `X-Forwarded-For` (lo escribe el cliente) y `native` confía en todas las redes privadas. El filtro solo cree a las subredes de `APP_PROXIES_CONFIABLES`. |
| La bitácora no guarda las respuestas 429 ni las rutas `/auth/**` del filtro de actividad | Registrar cada 429 convertiría un ataque en miles de escrituras; los eventos de `/auth` se registran de forma explícita y más precisa (con número de intento y motivo). |

---

## Estructura del repositorio

```
sea/
├── backend/                  Spring Boot (Java 21)
├── frontend/                 React + Vite + TypeScript
├── deploy/                   Archivos de despliegue (config de proxy)
├── .env                      Credenciales reales — NUNCA se sube a git
├── .env.example              Plantilla con valores ficticios
├── docker-compose.yml        Base: 3 servicios, sin puertos
├── docker-compose.local.yml  Override local: publica 5433 / 8080 / 80
└── docker-compose.prod.yml   Override servidor: red del reverse proxy
```
