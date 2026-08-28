-- ============================================================================
-- V14: Semilla de roles y permisos.
--
-- Dos roles por ahora, segun lo definido con el area:
--   SUPERADMINISTRADOR - personal de informatica. Acceso total, incluida la
--                        administracion de usuarios y la configuracion tecnica.
--   ADMINISTRADOR      - personal del IBA. Opera el padron completo, pero no
--                        toca configuracion tecnica ni crea superadministradores.
--
-- Mas adelante se pueden agregar roles desde el panel sin recompilar: por eso
-- los permisos viven en tabla y no en un enum de Java.
--
-- NOTA: el usuario inicial NO se crea aqui. Lo crea la aplicacion al arrancar,
-- con la contrasena tomada de variable de entorno y b_cambio_password_requerido
-- en TRUE. Una migracion con contrasena quemada quedaria en el repositorio para
-- siempre; el legado publicaba 'administrador / 12345678' en su manual.
-- ============================================================================

INSERT INTO cat_rol (s_clave, s_nombre, s_descripcion, b_sistema, n_orden) VALUES
    ('SUPERADMINISTRADOR', 'Superadministrador',
     'Personal de informatica. Control total del sistema.', TRUE, 1),
    ('ADMINISTRADOR', 'Administrador',
     'Personal del IBA. Opera el padron y resuelve tramites.', TRUE, 2);

-- ---------------------------------------------------------------------------
-- Permisos, agrupados por modulo (<modulo>:<accion>)
-- ---------------------------------------------------------------------------
INSERT INTO cat_permiso (s_clave, s_nombre, s_modulo, n_orden) VALUES
    -- Solicitudes
    ('solicitud:ver',        'Ver solicitudes',              'solicitud', 1),
    ('solicitud:crear',      'Registrar solicitud',          'solicitud', 2),
    ('solicitud:editar',     'Editar solicitud',             'solicitud', 3),
    ('solicitud:aprobar',    'Aprobar solicitud',            'solicitud', 4),
    ('solicitud:observar',   'Observar solicitud',           'solicitud', 5),
    ('solicitud:rechazar',   'Rechazar solicitud',           'solicitud', 6),
    ('solicitud:cancelar',   'Cancelar solicitud',           'solicitud', 7),
    -- Documentos. Descargar un documento personal queda en bitacora.
    ('documento:ver',        'Ver documentos',               'documento', 10),
    ('documento:descargar',  'Descargar documento personal', 'documento', 11),
    ('documento:revisar',    'Marcar documento legible o ilegible', 'documento', 12),
    ('documento:eliminar',   'Eliminar documento',           'documento', 13),
    -- Padron. Consultar por CURP queda en bitacora.
    ('persona:ver',          'Ver padron de personas',       'persona', 20),
    ('persona:consultar_curp','Consultar por CURP',          'persona', 21),
    ('persona:editar',       'Editar persona',               'persona', 22),
    ('animal:ver',           'Ver padron de animales',       'animal', 25),
    ('animal:editar',        'Editar animal',                'animal', 26),
    ('titularidad:transferir','Transferir titularidad',      'animal', 27),
    -- Certificados
    ('certificado:ver',      'Ver certificados',             'certificado', 30),
    ('certificado:emitir',   'Emitir certificado',           'certificado', 31),
    ('certificado:cancelar', 'Cancelar certificado',         'certificado', 32),
    ('certificado:imprimir', 'Imprimir certificados y credenciales', 'certificado', 33),
    -- Citas
    ('cita:ver',             'Ver citas',                    'cita', 40),
    ('cita:agendar',         'Agendar cita',                 'cita', 41),
    ('cita:reagendar',       'Reagendar cita',               'cita', 42),
    -- Jornadas
    ('jornada:ver',          'Ver jornadas',                 'jornada', 50),
    ('jornada:crear',        'Crear jornada',                'jornada', 51),
    ('jornada:publicar',     'Publicar jornada',             'jornada', 52),
    ('jornada:operar',       'Operar jornada en sitio',      'jornada', 53),
    -- Notificaciones
    ('notificacion:ver',     'Ver notificaciones',           'notificacion', 60),
    ('notificacion:contactar','Atender bandeja de contacto manual', 'notificacion', 61),
    ('notificacion:plantilla','Administrar plantillas',      'notificacion', 62),
    -- Catalogos
    ('catalogo:ver',         'Ver catalogos',                'catalogo', 70),
    ('catalogo:administrar', 'Administrar catalogos',        'catalogo', 71),
    -- Reportes
    ('reporte:ver',          'Ver reportes y tablero',       'reporte', 80),
    ('reporte:exportar',     'Exportar reportes',            'reporte', 81),
    -- Administracion (reservada a informatica)
    ('usuario:ver',          'Ver usuarios',                 'administracion', 90),
    ('usuario:administrar',  'Crear y editar usuarios',      'administracion', 91),
    ('rol:administrar',      'Administrar roles y permisos', 'administracion', 92),
    ('parametro:administrar','Administrar parametros del sistema', 'administracion', 93),
    ('bitacora:ver',         'Consultar bitacora de auditoria',   'administracion', 94),
    ('contenido:administrar','Administrar contenido del portal',  'administracion', 95);

-- ---------------------------------------------------------------------------
-- SUPERADMINISTRADOR: todos los permisos.
-- ---------------------------------------------------------------------------
INSERT INTO rol_permiso (fn_rol_id, fn_permiso_id)
SELECT r.pn_id, p.pn_id
FROM cat_rol r CROSS JOIN cat_permiso p
WHERE r.s_clave = 'SUPERADMINISTRADOR';

-- ---------------------------------------------------------------------------
-- ADMINISTRADOR: todo lo operativo del padron.
-- Se excluye la administracion tecnica: usuarios, roles, parametros.
-- Conserva la consulta de bitacora, porque el IBA necesita rendir cuentas de
-- sus propias resoluciones.
-- ---------------------------------------------------------------------------
INSERT INTO rol_permiso (fn_rol_id, fn_permiso_id)
SELECT r.pn_id, p.pn_id
FROM cat_rol r CROSS JOIN cat_permiso p
WHERE r.s_clave = 'ADMINISTRADOR'
  AND p.s_clave NOT IN (
      'usuario:administrar',
      'rol:administrar',
      'parametro:administrar'
  );
