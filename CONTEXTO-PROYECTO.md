# Contexto del proyecto: SEA — Sistema de Empadronamiento Animal

> Documento creado el 17 de noviembre de 2026 a partir del código existente (commit `e875f6c`).
> Lo marcado como **[PENDIENTE]** no se puede comprobar en el código y lo debe definir el
> responsable del proyecto.

## Objetivo

Padrón Estatal de Perros y Gatos del Instituto de Bienestar Animal (IBA), operado por el Sistema
Estatal DIF Puebla. Registra a cada animal junto con su tutor responsable y expide tres documentos:
certificado de empadronamiento, credencial ("Credencial Peluda") y acuse del trámite
(`README.md`).

La ciudadanía **no tiene cuenta**: registra y consulta su trámite en un portal público. Solo el
personal entra al panel administrativo.

## Estructura organizacional / alcance de datos

- **Decidido (17-11-2026):** todo el personal ve el padrón estatal completo. No hay jerarquía
  organizacional ni filtro por sede o municipio; el control de acceso es por permiso de rol, y el
  backend lo valida en cada endpoint.
- La sede de adscripción de la cuenta (`usuario.fn_sede_id`) es informativa.

## Roles y matriz de permisos

Sembrados en `V14__seed_seguridad.sql` y `V17__permiso_depurar.sql`. Los permisos son de la forma
`<modulo>:<accion>` y el backend los exige con `@PreAuthorize`.

| Rol | Quién es (según la semilla) | Ve | Captura | Valida / resuelve | Administra |
|---|---|---|---|---|---|
| SUPERADMINISTRADOR | Personal de informática | Todo, incluida la bitácora | Todo | Todo | Usuarios, roles, parámetros, catálogos |
| ADMINISTRADOR | Personal del IBA | Todo lo operativo y la lista de usuarios; **no** la bitácora | Trámites, documentos, tutores, animales, jornadas | Aprobar, observar, rechazar, cancelar, depurar | Catálogos y contenido; **no** usuarios, roles ni parámetros |
| (Ciudadanía, sin cuenta) | — | Su trámite con folio + CURP; verificación de certificados; jornadas | Registro del trámite y carga de documentos con el token | — | — |

- **Decidido (17-11-2026, "lo más lógico"):** se mantienen los dos roles. La bitácora la consulta
  solo SUPERADMINISTRADOR (informática), que administra el sistema; el IBA conserva el expediente de
  cada trámite (`solicitud_historial`). La carga de documentos tiene su propio permiso
  (`documento:subir`, ambos roles) en lugar de autorizarse con `documento:ver` (V20).

## Flujos y estados

Trámite (`util/enums/EstatusSolicitudEnum.java:71-78`):

```
REGISTRADA → EN_REVISION → APROBADA → CERTIFICADO_EMITIDO → ENTREGADA
                 │  ↑
                 ↓  │
             OBSERVADA
EN_REVISION → RECHAZADA
(cualquier estado abierto) → CANCELADA
```

- Canales de entrada: portal público, ventanilla y jornada municipal (`util/enums/CanalSolicitud.java`).
- Al aprobar se emite el certificado automáticamente; cada transición queda en `solicitud_historial`.
- Depuración del padrón: baja lógica con motivo y reactivación (permiso `solicitud:depurar`).

## Entidades principales

- `persona` (tutor) — 1..n — `titularidad` (con vigencia) — n..1 — `animal`.
- `solicitud` (trámite) → persona, animal, jornada; `solicitud_historial`.
- `documento` (archivos del trámite), `certificado`, `notificacion`, `jornada`, `cita` (sin uso todavía).
- `usuario`, `cat_rol`, `cat_permiso`, `bitacora_auditoria`, `parametro`, `folio_consecutivo`.
- 13 catálogos administrables con baja lógica (`cat_*`).

## Requisitos funcionales

Comprobados en el código (pantalla + endpoint):

- RF-01 Registro público del trámite en un solo envío, con folio de seguimiento.
- RF-02 Consulta pública del avance con folio y CURP; descarga del acuse.
- RF-03 Verificación pública de certificados por QR, sin datos del tutor.
- RF-04 Calendario público de jornadas.
- RF-05 Carga de documentos desde el portal (con token) y desde ventanilla.
- RF-06 Bandeja de trámites con filtros, detalle y resolución con motivo de catálogo.
- RF-07 Emisión del certificado y credencial (3 por hoja) en PDF.
- RF-08 Padrón de animales y de tutores, con fichas.
- RF-09 Jornadas: alta, publicación, operación en sitio e impresión por lote.
- RF-10 Bandeja de contacto para personas sin correo.
- RF-11 Administración de catálogos.
- RF-12 Administración de usuarios con contraseña temporal generada por el sistema.
- RF-13 Depuración del padrón con baja lógica y reactivación.

Pendientes de construir, en orden de prioridad:

- Corrección de documentos por liga con token (`/corregir/:token`).
- Reportes exportables (Excel/PDF).
- Pantalla de consulta de la bitácora.
- Indicadores del tablero.
- Captcha en el formulario público.
- Baja de tutores y animales desde su ficha.
- Citas de entrega.

Confirmado por el responsable (17-11-2026): la lista está completa y en orden de prioridad.

## Datos personales

Se capturan del tutor (`V4__persona_animal.sql`): CURP, nombre completo, correo, teléfono,
teléfono de WhatsApp, domicilio completo (calle, números, colonia, código postal, municipio), fecha de
nacimiento, sexo y entidad de nacimiento derivados de la CURP. Documentos adjuntos: INE, comprobante de
domicilio, fotografía del animal, comprobante de vacuna y certificado de adopción. Se guarda la IP de
quien registra como evidencia del consentimiento.

- **Decidido (17-11-2026): no se cifran en la base.** Se protegen con permiso por rol, enmascarado
  de la CURP en listados, bitácora de cada consulta de ficha y de cada búsqueda (sin
  guardar el valor buscado), y respaldos cifrados con AES-256.
- El verificador público no muestra datos del tutor. La consulta pública exige folio y CURP en el
  cuerpo de un POST, nunca en la dirección.
- El certificado impreso incluye dirección, teléfono y correo porque así es el formato oficial del IBA
  (decisión del responsable).
- **[PENDIENTE]** Aviso de privacidad definitivo (lo debe redactar el área jurídica).

## Integraciones y servicios externos

- Servidor SMTP institucional para avisos (deshabilitado hoy; `APP_MAIL_*`).
- Firma electrónica avanzada: interfaz enchufable (`FirmaService`), sin implementación real.
- El frontend no carga recursos de dominios externos (la CSP de `frontend/nginx.conf` solo permite `'self'`).

## Desviaciones del estándar

Documentadas en `README.md`, sección "Desviaciones del estándar".

## Bitácora de decisiones

| Fecha | Decisión |
|---|---|
| 2026-11-17 | Se crea este documento a partir del código, como paso previo a la revisión con el estándar del equipo. |
| 2026-11-17 | Alcance: todo el personal ve el padrón estatal. Datos personales sin cifrar en la base. No hay datos reales ni servidor todavía. |
| 2026-11-17 | Se reescribe el historial de git para quitar la contraseña del superadministrador que estaba en un documento de traspaso; se generan contraseñas nuevas en `.env`. |
| 2026-11-17 | Sesiones revocables: token de acceso solo en memoria, rotación del token de renovación con detección de reutilización, cierre real de sesión y `token_version` (V18). |
| 2026-11-17 | La aplicación se conecta con una cuenta sin superusuario; Flyway con la cuenta dueña (`deploy/db/init/01-roles.sh`). |
| 2026-11-17 | Bloqueo optimista en todas las tablas editables (V19). |
| 2026-11-18 | Bitácora de todos los movimientos, de solo inserción; `bitacora:ver` solo para informática; permiso `documento:subir` (V20). |
| 2026-11-18 | Respaldos cifrados con verificación y restauración probada (`deploy/respaldo.sh`, `deploy/restaurar.sh`). |
| 2026-11-18 | Pruebas de integración con Testcontainers (sesión, permisos, bitácora, edición concurrente). |
| 2026-11-18 | Demo con los DATOS REALES del sistema anterior (decisión del responsable: se cotejan contra la documentación): 216 tutores, 137 animales, 137 trámites, 130 certificados y 140 avisos (`deploy/demo/convertir_legado_demo.py`; `--anonimizar` queda como opción). |
| 2026-11-18 | El listado de tutores muestra correo y teléfono completos (decisión del responsable: no ocultar información salvo que se pida). La CURP sigue enmascarada en listados. |
| 2026-11-18 | Validación de captura unificada: anotaciones `@CurpValida`, `@Correo`, `@Telefono` y `@CodigoPostal` en todos los `*Request`; la consulta pública y la captura de CURP en la ficha del tutor validan la CURP en el navegador; el teclado numérico del celular ya se abre en teléfono, código postal y edad; el teléfono del personal se valida antes de guardar. |
| 2026-11-18 | Se corrige el verificador público del QR y la búsqueda de la bitácora: un cambio de ortografía había puesto acentos en `@Param("código")` y `@Param("acción")` y las consultas fallaban. |
| 2026-11-18 | Ajustes medios y bajos: tipo de archivo solo por contenido, consulta pública por POST, manejo de errores 400/405/409/415 y mensajes de validación en español, encabezados de Nginx completos, imágenes por digest, ESLint 9 sin advertencias, rutas diferidas, campos numéricos solo con dígitos, listas de autocompletado, colores del tema y teléfono/correo enmascarados en el listado de tutores. |

## Pendientes

- Funcionalidad: los siete puntos de "Requisitos funcionales" marcados como pendientes, en ese orden.
- Colores institucionales: sin definir; el tema actual usa el guinda institucional como provisional.
- Migración a Spring Boot 4.x (ver "Desviaciones del estándar" en `README.md`).
- En el servidor: configurar `APP_PROXIES_CONFIABLES` con la subred de Nginx Proxy Manager y programar
  los respaldos (ver `deploy/README.md`).

## Contexto de negocio que no está en el código

Lo que hay que entender para tomar decisiones de diseño.

**Por qué la ciudadanía no tiene cuenta.** El sistema anterior obligaba a crear
usuario y esperar hasta 24 horas a que un administrador lo validara, antes
siquiera de poder registrar al animal. Se creaban más usuarios que trámites
concluidos: abandono estructural del embudo. Por eso el registro es anónimo.

**Por qué la CURP no es llave.** RENAPO emite correcciones de CURP. Si fuera
llave primaria o foránea, corregir una obligaría a propagar el cambio por diez
tablas.

**Por qué la titularidad tiene vigencia.** Un animal se transfiere, se adopta,
el tutor fallece o el animal muere. Con una FK directa se pierde la historia y
el conteo "cuántos animales tiene esta persona" se vuelve ambiguo.

**Por qué el certificado no lleva CURP completa... salvo que el formato la
pida.** El certificado circula: se fotografía, se comparte, se deja en la
veterinaria. El usuario decidió incluir dirección, teléfono y correo porque así
es el formato oficial del IBA; queda anotado que es una decisión suya.

**Por qué tres credenciales por hoja.** Así las imprime hoy el personal en
opalina de 125 g/m². Cambiarlo desperdiciaría material en cada tirada.

**Por qué la búsqueda ignora acentos.** El día de una jornada se busca con la
gente formada enfrente. Nadie escribe la ñ de "Muñoz" con prisa.

**Por qué la baja del padrón es lógica y con motivo.** Hay dos casos que el
sistema no puede distinguir solo: una persona real que no concluyó su trámite
(su registro vale: si vuelve, se le reconoce por su CURP) y basura de alguien
probando. Confundirlos tiene costos opuestos, así que la decisión la toma el
personal y queda registrada. Nunca se borra: si alguien reclama, hay que poder
explicar qué pasó con su registro.

**Por qué no se puede dar de baja un trámite con certificado.** Ese documento
ya circula: se fotografió, se presentó en la veterinaria. Desaparecer a su
titular dejaría un certificado sin respaldo. Para esos casos se cancela el
certificado, que es una operación distinta y deja constancia.

**Por qué el stub de correo no finge éxito.** Si marcara los avisos como
enviados, el sistema daría por avisadas a personas que nunca recibieron nada, y
nadie se enteraría hasta que alguien reclamara.

## Errores recurrentes al desarrollar

Patrones que ya causaron fallos; conviene vigilarlos.

**1. Carga diferida al armar el DTO fuera de la transacción**

Apareció seis veces (`Raza`, `Sede`, `Solicitud`, `Certificado`,
`Notificacion`, `Titularidad`). Síntoma: `LazyInitializationException`.

Regla: si un servicio devuelve una entidad que el controlador convierte a DTO,
el repositorio debe traer sus relaciones con `@EntityGraph`.

**2. Parámetros nulos en consultas de PostgreSQL**

Apareció cinco veces. Síntoma: `could not determine data type of parameter $N`
o `function upper(bytea) does not exist`.

Regla: nunca pasar `null` a un parámetro dentro de `UPPER()`, `LIKE` o una
comparación de fechas. Sustituirlo antes por cadena vacía o por el extremo del
rango, con un método `default` en el repositorio.

**3. Enlaces y botones sin destino construido**

Apareció tres veces (menú del panel, botón de registro, transición a
ENTREGADA). Síntoma: el botón "no hace nada" porque el enrutador rebota.

Regla: al agregar un enlace, verificar que su ruta exista en `App.tsx`.
