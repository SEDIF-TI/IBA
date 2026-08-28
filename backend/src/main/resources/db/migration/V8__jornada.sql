-- ============================================================================
-- V8: Jornadas municipales.
--
-- El personal viaja a los municipios. La jornada se publica por anticipado, la
-- ciudadania precarga sus registros, y el dia del evento se busca por CURP o
-- apellidos, se valida en sitio y se imprime todo en el momento.
-- ============================================================================

CREATE TABLE jornada (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    s_nombre                  VARCHAR(200) NOT NULL,
    s_descripcion             VARCHAR(1000),

    fn_municipio_id           INTEGER      NOT NULL,
    s_domicilio_sede          VARCHAR(400) NOT NULL,

    d_fecha_inicio            DATE         NOT NULL,
    d_fecha_fin               DATE         NOT NULL,
    s_horario                 VARCHAR(120),

    -- NULL = sin limite de cupo.
    n_cupo                    INTEGER,

    -- Solo las publicadas aparecen en el portal publico.
    b_publicada               BOOLEAN      NOT NULL DEFAULT FALSE,
    d_fecha_publicacion       TIMESTAMPTZ,

    fn_usuario_responsable_id BIGINT,

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_jornada PRIMARY KEY (pn_id),
    CONSTRAINT fk_jornada_cat_municipio FOREIGN KEY (fn_municipio_id)
        REFERENCES cat_municipio (pn_id),
    CONSTRAINT fk_jornada_usuario_responsable FOREIGN KEY (fn_usuario_responsable_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_jornada_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_jornada_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),

    CONSTRAINT ck_jornada_fechas CHECK (d_fecha_fin >= d_fecha_inicio),
    CONSTRAINT ck_jornada_cupo CHECK (n_cupo IS NULL OR n_cupo > 0),
    CONSTRAINT ck_jornada_publicacion CHECK (
        b_publicada = FALSE OR d_fecha_publicacion IS NOT NULL
    )
);

CREATE INDEX ix_jornada_municipio ON jornada (fn_municipio_id);
CREATE INDEX ix_jornada_fechas    ON jornada (d_fecha_inicio, d_fecha_fin);
-- Calendario publico: solo las publicadas y vigentes.
CREATE INDEX ix_jornada_publicada ON jornada (b_publicada, d_fecha_inicio)
    WHERE b_publicada = TRUE AND b_activo = TRUE;

COMMENT ON TABLE jornada IS 'Jornada municipal de empadronamiento en sitio';

-- La FK de solicitud a jornada se declara aqui, ya que solicitud (V5) se creo
-- antes que esta tabla.
ALTER TABLE solicitud
    ADD CONSTRAINT fk_solicitud_jornada FOREIGN KEY (fn_jornada_id)
        REFERENCES jornada (pn_id);
