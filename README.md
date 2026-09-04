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

### 2. Generar el secreto JWT

El arranque **falla a propósito** si el secreto es corto, de baja entropía o si
sigue siendo el valor de ejemplo. Genera uno real:

```bash
# Bash
openssl rand -base64 64
```

Pega el resultado en `APP_JWT_SECRET` dentro de `.env`. Cambia también
`POSTGRES_PASSWORD` y `APP_ADMIN_PASSWORD`.

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
red externa definida en `PROXY_NETWORK`.

---

## Stack

| Capa           | Tecnología                                   |
|----------------|----------------------------------------------|
| Backend        | Java 21 + Spring Boot 3.x (Maven)            |
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

### Commits
En español, formato convencional: `feat:`, `fix:`, `refactor:`, `docs:`.

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
