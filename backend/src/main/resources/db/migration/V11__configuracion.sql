-- ============================================================================
-- V11: Configuracion administrable, avisos de privacidad, folios y CMS.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Parametros del sistema.
-- Aqui viven la vigencia del certificado, los patrones de folio, las leyendas
-- de los documentos y la identidad grafica. Nada de eso se quema en el codigo:
-- la institucion pasa de SMADSOT a DIF y todo eso cambia.
-- ----------------------------------------------------------------------------
CREATE TABLE parametro (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(80)  NOT NULL,
    s_valor                   TEXT         NOT NULL,
    s_descripcion             VARCHAR(500),
    -- TEXTO, ENTERO, DECIMAL, BOOLEANO, FECHA, JSON. Permite validar el valor
    -- antes de guardarlo desde el panel.
    s_tipo_dato               VARCHAR(20)  NOT NULL DEFAULT 'TEXTO',
    -- Agrupa los parametros por seccion en la pantalla de configuracion.
    s_grupo                   VARCHAR(60),
    -- Protegido: no se borra desde el panel, el sistema lo necesita.
    b_sistema                 BOOLEAN      NOT NULL DEFAULT FALSE,

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_parametro PRIMARY KEY (pn_id),
    CONSTRAINT uq_parametro_clave UNIQUE (s_clave),
    CONSTRAINT fk_parametro_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_parametro_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT ck_parametro_tipo CHECK (
        s_tipo_dato IN ('TEXTO', 'ENTERO', 'DECIMAL', 'BOOLEANO', 'FECHA', 'JSON')
    )
);
CREATE INDEX ix_parametro_grupo ON parametro (s_grupo);

-- ----------------------------------------------------------------------------
-- Aviso de privacidad VERSIONADO.
-- Cada solicitud guarda la version que acepto la persona. Si el aviso cambia,
-- las solicitudes viejas conservan constancia de lo que se acepto entonces.
-- ----------------------------------------------------------------------------
CREATE TABLE aviso_privacidad (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_version                 VARCHAR(20)  NOT NULL,
    s_titulo                  VARCHAR(300) NOT NULL,
    s_contenido               TEXT         NOT NULL,
    d_fecha_vigencia          DATE         NOT NULL,
    b_vigente                 BOOLEAN      NOT NULL DEFAULT FALSE,

    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_aviso_privacidad PRIMARY KEY (pn_id),
    CONSTRAINT uq_aviso_privacidad_version UNIQUE (s_version),
    CONSTRAINT fk_aviso_privacidad_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_aviso_privacidad_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id)
);
-- Solo puede haber UN aviso vigente a la vez.
CREATE UNIQUE INDEX uq_aviso_privacidad_vigente
    ON aviso_privacidad ((TRUE)) WHERE b_vigente = TRUE;

COMMENT ON TABLE aviso_privacidad IS
    'Aviso versionado. La solicitud guarda que version acepto la persona.';

-- ----------------------------------------------------------------------------
-- Consecutivos de folio.
--
-- Control transaccional por tipo, municipio y ano. El servicio toma el renglon
-- con SELECT ... FOR UPDATE antes de incrementar: bajo concurrencia, dos
-- solicitudes simultaneas no pueden obtener el mismo folio.
-- ----------------------------------------------------------------------------
CREATE TABLE folio_consecutivo (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    -- SOLICITUD, CERTIFICADO, CITA, ANIMAL
    s_tipo                    VARCHAR(30)  NOT NULL,
    -- NULL para los consecutivos globales (que no dependen del municipio).
    fn_municipio_id           INTEGER,
    n_anio                    INTEGER      NOT NULL,
    n_consecutivo             BIGINT       NOT NULL DEFAULT 0,

    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,

    CONSTRAINT pk_folio_consecutivo PRIMARY KEY (pn_id),
    CONSTRAINT fk_folio_consecutivo_cat_municipio FOREIGN KEY (fn_municipio_id)
        REFERENCES cat_municipio (pn_id),
    CONSTRAINT ck_folio_consecutivo_anio CHECK (n_anio BETWEEN 2000 AND 2200),
    CONSTRAINT ck_folio_consecutivo_valor CHECK (n_consecutivo >= 0)
);

-- Dos indices unicos parciales: uno para los consecutivos por municipio y otro
-- para los globales. Un UNIQUE normal sobre (s_tipo, fn_municipio_id, n_anio)
-- no serviria, porque en PostgreSQL varios NULL no colisionan entre si y se
-- podrian crear renglones globales duplicados para el mismo tipo y ano.
CREATE UNIQUE INDEX uq_folio_consecutivo_municipio
    ON folio_consecutivo (s_tipo, fn_municipio_id, n_anio)
    WHERE fn_municipio_id IS NOT NULL;
CREATE UNIQUE INDEX uq_folio_consecutivo_global
    ON folio_consecutivo (s_tipo, n_anio)
    WHERE fn_municipio_id IS NULL;

COMMENT ON TABLE folio_consecutivo IS
    'Consecutivos con bloqueo transaccional (SELECT FOR UPDATE): sin folios duplicados.';

-- ----------------------------------------------------------------------------
-- Contenido publico: CMS ligero para requisitos, avisos y preguntas frecuentes.
-- ----------------------------------------------------------------------------
CREATE TABLE contenido_publico (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(60)  NOT NULL,
    s_titulo                  VARCHAR(300) NOT NULL,
    s_contenido               TEXT         NOT NULL,
    -- REQUISITOS, AVISO, FAQ, LEYENDA
    s_tipo                    VARCHAR(30)  NOT NULL DEFAULT 'FAQ',
    n_orden                   INTEGER      NOT NULL DEFAULT 0,
    b_publicado               BOOLEAN      NOT NULL DEFAULT FALSE,

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_contenido_publico PRIMARY KEY (pn_id),
    CONSTRAINT uq_contenido_publico_clave UNIQUE (s_clave),
    CONSTRAINT fk_contenido_publico_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_contenido_publico_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT ck_contenido_publico_tipo CHECK (
        s_tipo IN ('REQUISITOS', 'AVISO', 'FAQ', 'LEYENDA')
    )
);
CREATE INDEX ix_contenido_publico_tipo ON contenido_publico (s_tipo, n_orden)
    WHERE b_publicado = TRUE AND b_activo = TRUE;
