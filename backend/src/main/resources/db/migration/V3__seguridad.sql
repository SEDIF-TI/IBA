-- ============================================================================
-- V3: Seguridad. Usuarios administrativos, roles y permisos.
--
-- IMPORTANTE: aqui solo vive el PERSONAL (IBA, DIF, informatica). La
-- ciudadania NO tiene cuenta: el registro publico es anonimo por diseno.
-- Sus datos viven en la tabla persona (V4), que no es una tabla de acceso.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Rol
-- ----------------------------------------------------------------------------
CREATE TABLE cat_rol (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    -- Rol de sistema: no se puede borrar ni renombrar desde el panel.
    -- Evita que alguien elimine SUPERADMINISTRADOR y deje el sistema sin acceso.
    b_sistema                 BOOLEAN      NOT NULL DEFAULT FALSE,
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_rol       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_rol_clave UNIQUE (s_clave)
);
COMMENT ON COLUMN cat_rol.b_sistema IS
    'Rol protegido: no se borra ni renombra desde el panel';

-- ----------------------------------------------------------------------------
-- Permiso. Granularidad <modulo>:<accion>, ej. solicitud:aprobar
-- ----------------------------------------------------------------------------
CREATE TABLE cat_permiso (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(80)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    -- Agrupa los permisos en la pantalla de administracion de roles.
    s_modulo                  VARCHAR(50)  NOT NULL,
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_permiso       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_permiso_clave UNIQUE (s_clave)
);
CREATE INDEX ix_cat_permiso_modulo ON cat_permiso (s_modulo);

-- ----------------------------------------------------------------------------
-- Usuario administrativo
-- ----------------------------------------------------------------------------
CREATE TABLE usuario (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    s_usuario                 VARCHAR(80)  NOT NULL,
    -- Hash BCrypt (60 caracteres). Jamas texto plano, jamas en bitacora,
    -- jamas en una respuesta del API.
    s_password_hash           VARCHAR(100) NOT NULL,
    s_nombre                  VARCHAR(120) NOT NULL,
    s_apellido_paterno        VARCHAR(120) NOT NULL,
    s_apellido_materno        VARCHAR(120),
    s_correo                  VARCHAR(150) NOT NULL,
    s_telefono                VARCHAR(30),
    -- Sede a la que esta adscrito; sirve para filtrar su bandeja de trabajo.
    fn_sede_id                INTEGER,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    -- El usuario semilla se crea con TRUE: obliga a cambiar la contrasena
    -- en el primer acceso. No existe contrasena por defecto conocida.
    b_cambio_password_requerido BOOLEAN    NOT NULL DEFAULT TRUE,
    d_ultimo_acceso           TIMESTAMPTZ,
    n_intentos_fallidos       INTEGER      NOT NULL DEFAULT 0,
    d_bloqueo_hasta           TIMESTAMPTZ,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_usuario            PRIMARY KEY (pn_id),
    CONSTRAINT uq_usuario_usuario    UNIQUE (s_usuario),
    CONSTRAINT uq_usuario_correo     UNIQUE (s_correo),
    CONSTRAINT fk_usuario_cat_sede   FOREIGN KEY (fn_sede_id)
        REFERENCES cat_sede (pn_id),
    CONSTRAINT ck_usuario_intentos   CHECK (n_intentos_fallidos >= 0),
    CONSTRAINT ck_usuario_correo     CHECK (s_correo ~* '^[^@[:space:]]+@[^@[:space:]]+\.[a-z]{2,}$')
);
CREATE INDEX ix_usuario_activo ON usuario (b_activo);
CREATE INDEX ix_usuario_sede   ON usuario (fn_sede_id);
COMMENT ON TABLE  usuario IS 'Personal administrativo. La ciudadania no tiene cuenta.';
COMMENT ON COLUMN usuario.d_bloqueo_hasta IS
    'Bloqueo temporal por intentos fallidos; NULL si no esta bloqueado';

-- Autorreferencia de auditoria: se agrega despues de crear la tabla.
ALTER TABLE usuario
    ADD CONSTRAINT fk_usuario_usuario_creacion
        FOREIGN KEY (fn_usuario_creacion_id) REFERENCES usuario (pn_id),
    ADD CONSTRAINT fk_usuario_usuario_modificacion
        FOREIGN KEY (fn_usuario_modificacion_id) REFERENCES usuario (pn_id);

-- ----------------------------------------------------------------------------
-- usuario_rol (N:N)
-- ----------------------------------------------------------------------------
CREATE TABLE usuario_rol (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    fn_usuario_id             BIGINT       NOT NULL,
    fn_rol_id                 INTEGER      NOT NULL,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fn_usuario_creacion_id    BIGINT,
    CONSTRAINT pk_usuario_rol          PRIMARY KEY (pn_id),
    CONSTRAINT uq_usuario_rol          UNIQUE (fn_usuario_id, fn_rol_id),
    -- Si se borra el usuario se van sus asignaciones; el rol nunca se borra
    -- fisicamente (borrado logico con b_activo).
    CONSTRAINT fk_usuario_rol_usuario  FOREIGN KEY (fn_usuario_id)
        REFERENCES usuario (pn_id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_rol_cat_rol  FOREIGN KEY (fn_rol_id)
        REFERENCES cat_rol (pn_id)
);
CREATE INDEX ix_usuario_rol_usuario ON usuario_rol (fn_usuario_id);
CREATE INDEX ix_usuario_rol_rol     ON usuario_rol (fn_rol_id);

-- ----------------------------------------------------------------------------
-- rol_permiso (N:N)
-- ----------------------------------------------------------------------------
CREATE TABLE rol_permiso (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    fn_rol_id                 INTEGER      NOT NULL,
    fn_permiso_id             INTEGER      NOT NULL,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fn_usuario_creacion_id    BIGINT,
    CONSTRAINT pk_rol_permiso             PRIMARY KEY (pn_id),
    CONSTRAINT uq_rol_permiso             UNIQUE (fn_rol_id, fn_permiso_id),
    CONSTRAINT fk_rol_permiso_cat_rol     FOREIGN KEY (fn_rol_id)
        REFERENCES cat_rol (pn_id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_permiso_cat_permiso FOREIGN KEY (fn_permiso_id)
        REFERENCES cat_permiso (pn_id) ON DELETE CASCADE
);
CREATE INDEX ix_rol_permiso_rol     ON rol_permiso (fn_rol_id);
CREATE INDEX ix_rol_permiso_permiso ON rol_permiso (fn_permiso_id);
