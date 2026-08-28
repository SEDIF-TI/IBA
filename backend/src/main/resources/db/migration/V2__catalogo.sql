-- ============================================================================
-- V2: Catalogos administrables.
--
-- Todos comparten la misma forma: pn_id, s_clave (referencia estable para el
-- codigo), s_nombre, s_descripcion, n_orden, b_activo y auditoria.
--
-- Motivo del rediseno: en el sistema legado estos valores eran texto libre.
-- Convivian PERRO y CANINO como especies distintas, 63 razas y 61 colores
-- escritos a mano, y edades como '1', '1 ANO', '2 MESES'. Aqui todo es
-- catalogo con llave foranea: no hay forma de escribir una variante nueva.
--
-- s_clave es la referencia estable para el codigo. s_nombre puede cambiar
-- (lo edita el personal desde el panel) sin romper nada.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Municipios de Puebla
-- ----------------------------------------------------------------------------
CREATE TABLE cat_municipio (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    -- Clave INEGI de 5 posiciones (ej. 21114 = Puebla). Se usa en el folio
    -- oficial del certificado.
    s_cve_inegi               VARCHAR(5)   NOT NULL,
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_municipio            PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_municipio_clave      UNIQUE (s_clave),
    CONSTRAINT uq_cat_municipio_cve_inegi  UNIQUE (s_cve_inegi),
    CONSTRAINT ck_cat_municipio_cve_inegi  CHECK (s_cve_inegi ~ '^[0-9]{5}$')
);
COMMENT ON TABLE  cat_municipio IS 'Catalogo de los 217 municipios del estado de Puebla';
COMMENT ON COLUMN cat_municipio.s_cve_inegi IS 'Clave INEGI de 5 digitos; se usa en el folio oficial';

-- ----------------------------------------------------------------------------
-- Especie: canino y felino. UN SOLO nombre por especie.
-- ----------------------------------------------------------------------------
CREATE TABLE cat_especie (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_especie       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_especie_clave UNIQUE (s_clave)
);
COMMENT ON TABLE cat_especie IS 'Especies registrables: canino y felino';

-- ----------------------------------------------------------------------------
-- Raza: depende de la especie. Un gato no puede ser Chihuahua.
-- ----------------------------------------------------------------------------
CREATE TABLE cat_raza (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    fn_especie_id             INTEGER      NOT NULL,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_raza             PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_raza_clave       UNIQUE (s_clave),
    CONSTRAINT fk_cat_raza_cat_especie FOREIGN KEY (fn_especie_id)
        REFERENCES cat_especie (pn_id)
);
CREATE INDEX ix_cat_raza_especie ON cat_raza (fn_especie_id);
COMMENT ON TABLE cat_raza IS 'Razas por especie; incluye Mestizo/Criollo, la mas frecuente';

-- ----------------------------------------------------------------------------
-- Color, sexo y tamano
-- ----------------------------------------------------------------------------
CREATE TABLE cat_color (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_color       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_color_clave UNIQUE (s_clave)
);

CREATE TABLE cat_sexo (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_sexo       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_sexo_clave UNIQUE (s_clave)
);

CREATE TABLE cat_tamano (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_tamano       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_tamano_clave UNIQUE (s_clave)
);

-- ----------------------------------------------------------------------------
-- Metodo de identificacion del animal: microchip, placa, tatuaje, ninguno
-- ----------------------------------------------------------------------------
CREATE TABLE cat_metodo_identificacion (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    -- Si es TRUE, la UI exige capturar el numero de chip o placa.
    b_requiere_identificador  BOOLEAN      NOT NULL DEFAULT FALSE,
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_metodo_identificacion       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_metodo_identificacion_clave UNIQUE (s_clave)
);

-- ----------------------------------------------------------------------------
-- Tipo de documento: define que se pide, cuanto puede pesar y en que formato
-- ----------------------------------------------------------------------------
CREATE TABLE cat_tipo_documento (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    -- Que se exige para la persona y que para el animal.
    b_requerido_persona       BOOLEAN      NOT NULL DEFAULT FALSE,
    b_requerido_animal        BOOLEAN      NOT NULL DEFAULT FALSE,
    n_tamano_maximo_mb        INTEGER      NOT NULL DEFAULT 5,
    -- Lista separada por comas: 'pdf,jpg,jpeg,png'. Se valida ademas el MIME
    -- real por contenido, no solo la extension.
    s_extensiones_permitidas  VARCHAR(200) NOT NULL DEFAULT 'pdf,jpg,jpeg,png',
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_tipo_documento        PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_tipo_documento_clave  UNIQUE (s_clave),
    CONSTRAINT ck_cat_tipo_documento_tamano CHECK (n_tamano_maximo_mb BETWEEN 1 AND 50)
);

-- ----------------------------------------------------------------------------
-- Motivo de rechazo u observacion
-- ----------------------------------------------------------------------------
CREATE TABLE cat_motivo_rechazo (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    -- TRUE: la persona puede corregir y reenviar (pasa a OBSERVADA).
    -- FALSE: es causa de rechazo definitivo.
    b_permite_correccion      BOOLEAN      NOT NULL DEFAULT TRUE,
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_motivo_rechazo       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_motivo_rechazo_clave UNIQUE (s_clave)
);

-- ----------------------------------------------------------------------------
-- Estatus de la solicitud (maquina de estados)
--
-- Sustituye las banderas magicas del legado: un campo 'activo' con valores
-- 1, 0 y 3 sin documentar, y un 'estado' que siempre valia 'Proceso'.
-- ----------------------------------------------------------------------------
CREATE TABLE cat_estatus_solicitud (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    -- Estado terminal: no admite mas transiciones.
    b_terminal                BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Exige motivo del catalogo al entrar (OBSERVADA y RECHAZADA).
    b_requiere_motivo         BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Color con el que se pinta la insignia en el panel.
    s_color_ui                VARCHAR(20),
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_estatus_solicitud       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_estatus_solicitud_clave UNIQUE (s_clave)
);

-- ----------------------------------------------------------------------------
-- Estatus de la cita de entrega
-- (Se agrega por coherencia: en el diseno original venia como 'fn_estatus'
--  suelto en la tabla cita, sin tabla referida.)
-- ----------------------------------------------------------------------------
CREATE TABLE cat_estatus_cita (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    b_terminal                BOOLEAN      NOT NULL DEFAULT FALSE,
    s_color_ui                VARCHAR(20),
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_estatus_cita       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_estatus_cita_clave UNIQUE (s_clave)
);

-- ----------------------------------------------------------------------------
-- Medio de contacto: correo, telefono, WhatsApp
-- ----------------------------------------------------------------------------
CREATE TABLE cat_medio_contacto (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    -- FALSE cuando el aviso lo tiene que dar una persona (llamada o WhatsApp):
    -- esas notificaciones caen en la bandeja de contacto manual.
    b_automatizable           BOOLEAN      NOT NULL DEFAULT FALSE,
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_medio_contacto       PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_medio_contacto_clave UNIQUE (s_clave)
);
COMMENT ON COLUMN cat_medio_contacto.b_automatizable IS
    'FALSE: requiere intervencion humana; la notificacion va a la bandeja manual';

-- ----------------------------------------------------------------------------
-- Sede de entrega. La direccion NO se codifica en duro en ningun lado.
-- ----------------------------------------------------------------------------
CREATE TABLE cat_sede (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(50)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),
    s_domicilio               VARCHAR(400) NOT NULL,
    fn_municipio_id           INTEGER      NOT NULL,
    s_horario                 VARCHAR(200),
    s_telefono                VARCHAR(30),
    s_correo                  VARCHAR(150),
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,
    CONSTRAINT pk_cat_sede                PRIMARY KEY (pn_id),
    CONSTRAINT uq_cat_sede_clave          UNIQUE (s_clave),
    CONSTRAINT fk_cat_sede_cat_municipio  FOREIGN KEY (fn_municipio_id)
        REFERENCES cat_municipio (pn_id)
);
CREATE INDEX ix_cat_sede_municipio ON cat_sede (fn_municipio_id);
COMMENT ON TABLE cat_sede IS 'Puntos de entrega; la direccion se administra, no se programa';
