# Traspaso del proyecto SEA

Estado del Sistema de Empadronamiento Animal y qué falta por construir.
Documento para retomar el trabajo en otra conversación.

**Última actualización:** 4 de septiembre de 2026

---

## Cómo retomar

```bash
# Bash (WSL)
cd ~/iba
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d
```

| Servicio | Dónde |
|---|---|
| Portal ciudadano | http://localhost |
| Panel administrativo | http://localhost/admin/login |
| API | http://localhost/api |
| PostgreSQL | localhost:5433 |

**Acceso:** `superadmin` / `<la de APP_ADMIN_PASSWORD en .env>` (también en `.env`)

> Si el login falla, la cuenta pudo bloquearse por intentos:
> ```bash
> docker exec sea-db psql -U sea_app -d sea -c \
>   "update usuario set n_intentos_fallidos=0, d_bloqueo_hasta=null;"
> ```

**Git:** 25 commits locales, **sin remoto ni push**. El usuario decidirá dónde
se publica. Nunca se ha ejecutado `git push` ni se ha configurado un remoto.

---

## Lo que está construido y verificado

### Backend
- **16 migraciones Flyway**, 34 tablas, 217 municipios sembrados.
- **24 entidades JPA** validadas con `ddl-auto=validate`.
- **123 pruebas** en verde (`./backend/mvnw test`).
- Seguridad: JWT con refresco en cookie HttpOnly, BCrypt factor 12, bloqueo por
  intentos, límite de peticiones por IP y ruta, CORS con lista explícita.
- Almacén de documentos fuera del alcance web, con validación de tipo MIME por
  contenido y bitácora de cada descarga.
- Máquina de estados completa, con expediente de cada transición.
- Generación de certificado, credencial (3 por hoja) y acuse en PDF, con QR.
- Notificaciones con bandeja de contacto manual.
- Jornadas municipales con búsqueda en sitio e impresión por lote.
- Depuración del padrón: baja lógica de registros que nunca fueron reales, con
  motivo obligatorio, papelera y reactivación.

### Frontend
- Portal ciudadano: portada, registro, consulta, verificador de QR, calendario
  de jornadas.
- Panel: tablero, bandeja de trámites, detalle con resolución, padrón de
  animales, tutores, catálogos, jornadas con operación en sitio, bandeja de
  contacto, captura en ventanilla.

### Ciclo completo verificado
```
Registro (público / ventanilla / jornada)
  → EN_REVISION → APROBADA → CERTIFICADO_EMITIDO → ENTREGADA
```
Con desvíos a OBSERVADA (corrección), RECHAZADA y CANCELADA.

---

## Lo que falta

Ordenado por lo que más bloquea el uso real.

### 1. Administración de usuarios — **HECHO** (7 sep 2026)

El IBA ya puede dar de alta a su personal desde *Personal* en el menú.

- `UsuarioAdminService` + `UsuarioResource` (`/usuarios`): alta, edición,
  baja lógica, reactivación, reinicio de contraseña, desbloqueo.
- Pantalla `AdminUsuarios.tsx`, ruta `/admin/usuarios`.
- **La contraseña la genera el sistema**, se muestra una sola vez y obliga a
  cambiarla al entrar. Nadie elige la contraseña de otra persona.
- Dos protecciones: no puedes darte de baja a ti mismo, ni dar de baja al
  último superadministrador activo (dejaría el sistema sin quien administre).
- `usuario:ver` lo tiene el IBA; `usuario:administrar` solo informática.
- 9 pruebas en `UsuarioAdminServiceTest`.

**Pendiente relacionado**: administrar los *roles* en sí (qué permisos lleva
cada rol) sigue siendo por base de datos. Con dos roles fijos no urge.

### 2. Corrección de documentos por liga con token — **falta completo**

Cuando un trámite queda OBSERVADA, la persona debe poder corregir sin volver a
empezar. El campo `s_token_consulta` existe y el aviso ya menciona la liga,
pero la pantalla no existe.

- Ruta pública `/corregir/:token`.
- Endpoint que permita adjuntar documentos con el token, sin sesión.
- Al reenviar, el trámite vuelve a EN_REVISION.

### 3. Carga de documentos — **HECHO** (7 sep 2026)

Se carga desde dos lugares:

- **Portal ciudadano**: al terminar el registro, con el token del trámite
  (`POST /publico/documento?token=...`). Sin cuenta, como todo el portal.
- **Ventanilla**: en la ficha del trámite, para el personal.

Componente común `CargaDocumento.tsx`. La fotografía **ya llega al
certificado** (verificado: 2 imágenes en el PDF con foto contra 1 sin ella).

Restricciones del destino público: solo tipos que la persona tiene en la mano
(no `CERTIFICADO_ADOPCION`), solo mientras el trámite no esté cerrado, y
10 cargas por minuto por dirección.

### 4. Reportes exportables — **falta completo**

Punto 6.B.10 del alcance. La dependencia POI ya está en el `pom.xml`.
- Solicitudes por periodo, municipio, estatus.
- Exportación a Excel y PDF.

### 5. Consulta de bitácora de auditoría — **falta el frontend**

El backend está completo (`BitacoraAuditoriaRepository.buscar`), pero no hay
pantalla. El permiso `bitacora:ver` ya existe.

### 6. Indicadores del tablero — **el tablero está vacío**

`Tablero.tsx` solo muestra tarjetas descriptivas. Faltan los indicadores del
punto 6.B.1: solicitudes pendientes, aprobadas por periodo, tiempo promedio de
resolución, solicitudes por municipio.

### 7. Captcha en el formulario público — **falta**

El parámetro `portal.captcha_habilitado` existe pero nadie lo lee. El límite
por IP sí funciona (20 registros/minuto).

### 8. Baja lógica de tutores y animales desde su propia ficha — **parcial**

Hoy la depuración se hace desde el trámite, que es el caso frecuente. Falta
poder dar de baja a un tutor o a un animal directamente desde su ficha, para
depurar registros que ya no tienen trámite asociado.

### 9. Citas de entrega — **modelo sin uso**

Las tablas `cita` y `cat_estatus_cita` existen y están sembradas, pero nada las
usa. El alcance dice que al aprobar se asigna cita; hoy no ocurre.

---

## Decisiones pendientes del IBA

Ninguna impide usar el sistema. Todas están anotadas en el README.

| Pendiente | Cómo se resuelve |
|---|---|
| Envío real de correo | Configurar `APP_MAIL_*` en `.env` y `APP_MAIL_ENABLED=true`. Mientras tanto los avisos caen en la bandeja de contacto. |
| Colores de certificado y credencial | Parámetros del grupo `documento`. Hoy usan el guinda institucional; el IBA aún no define el rosa DIBA. |
| Logotipos institucionales | Los documentos rotulan los nombres. Para incrustar las imágenes hacen falta los archivos originales. |
| Aviso de privacidad | El sembrado es un texto mínimo marcado como PENDIENTE. Debe sustituirlo el área jurídica. |
| Firma electrónica avanzada | `FirmaService` es enchufable. La implementación vigente no firma y lo hace constar. |
| Costo del trámite y tiempo de respuesta | Sembrados en `contenido_publico` pero sin publicar. |

---

## Convenciones que hay que respetar

Están aplicadas en todo el código; conviene seguirlas.

**Base de datos**
- Tablas en singular, `snake_case`, sin acentos ni ñ.
- Prefijos: `pn_` (PK), `fn_` (FK), `s_` (texto), `n_` (numérico), `b_`
  (booleano), `d_` (fecha), `j_` (JSONB).
- Borrado lógico con `b_activo`, nunca físico.
- La CURP nunca es llave; va con índice único parcial.
- `ddl-auto=validate`: Hibernate no toca el esquema, solo Flyway.
- **Nunca editar una migración aplicada.** Solo agregar `Vn__` nueva.

**Backend**
- Organizado por dominio en `core/<feature>/`, no por capa técnica.
- Controladores con sufijo `Resource` (excepto `AuthController`).
- Entidades en camelCase, mapeadas con `@Column(name = "s_nombre")`.
- Respuesta uniforme `ApiResponse<T>`.

**Frontend**
- Ningún componente llama a axios: todo pasa por `services/`.
- `theme.ts` es la única fuente de color.
- Los colores nunca se escriben a mano en un componente.

**Mensajes** (criterio que pidió el usuario)
- Todo mensaje dice qué pasó y qué hacer. Nada de "operación inválida".
- Los de error nombran la causa concreta; los de éxito, lo que se logró.
- Ocho pruebas en `MensajesTest` fijan este criterio.
- Excepción deliberada: el mensaje de credenciales es vago a propósito.

---

## Errores recurrentes en este proyecto

Tres patrones que ya causaron fallos. Conviene vigilarlos.

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

Regla: al agregar un enlace, verificar que su ruta exista en `App.tsx`. Hay un
script de auditoría en el historial de esta conversación que recorre los
destinos contra las rutas declaradas.

---

## Comandos útiles

```bash
# Bash (WSL) — desde ~/iba

# Pruebas del backend
./backend/mvnw -f backend/pom.xml test

# Comprobación de tipos del frontend
cd frontend && npx tsc --noEmit

# Levantar todo
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d

# Ver la bitácora del backend
docker compose logs -f backend

# Consola de la base
docker exec -it sea-db psql -U sea_app -d sea

# Estado de las migraciones
docker exec sea-db psql -U sea_app -d sea -c \
  "select version, description, success from flyway_schema_history order by installed_rank;"

# Recrear la base desde cero (BORRA los datos)
docker compose -f docker-compose.yml -f docker-compose.local.yml down -v
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d
```

---

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
