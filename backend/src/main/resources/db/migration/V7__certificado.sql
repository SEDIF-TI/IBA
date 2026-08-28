-- ============================================================================
-- V7: Certificado emitido y cita de entrega.
-- ============================================================================

CREATE TABLE certificado (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    fn_solicitud_id           BIGINT       NOT NULL,

    -- Folio OFICIAL definitivo, distinto del folio de la solicitud. Se asigna
    -- al aprobar. Patron configurable en 'parametro' (V11).
    s_folio                   VARCHAR(80)  NOT NULL,

    -- Codigo que va dentro del QR. UUID opaco: quien lo escanea ve la validez
    -- del certificado, nunca datos personales del tutor.
    s_codigo_verificacion     UUID         NOT NULL DEFAULT gen_random_uuid(),

    d_fecha_emision           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_vigencia          DATE,

    fn_usuario_emisor_id      BIGINT       NOT NULL,

    -- SHA-256 del PDF emitido: permite probar que un documento presentado
    -- corresponde byte a byte con el que expidio el sistema.
    s_hash_documento          VARCHAR(64),

    -- La firma electronica avanzada es una decision administrativa pendiente.
    -- El servicio de firma es enchufable (FirmaService); estas columnas quedan
    -- listas para cuando el area juridica resuelva.
    b_firmado_electronicamente BOOLEAN     NOT NULL DEFAULT FALSE,
    d_fecha_firma             TIMESTAMPTZ,
    s_sello_firma             TEXT,

    -- Mientras tanto, el sello institucional se entrega en fisico.
    b_sello_fisico_entregado  BOOLEAN      NOT NULL DEFAULT FALSE,
    d_fecha_entrega           TIMESTAMPTZ,

    b_vigente                 BOOLEAN      NOT NULL DEFAULT TRUE,
    s_motivo_cancelacion      VARCHAR(500),
    d_fecha_cancelacion       TIMESTAMPTZ,
    fn_usuario_cancelacion_id BIGINT,

    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_certificado PRIMARY KEY (pn_id),
    CONSTRAINT uq_certificado_folio UNIQUE (s_folio),
    CONSTRAINT uq_certificado_codigo_verificacion UNIQUE (s_codigo_verificacion),
    -- Un certificado vigente por solicitud; la reposicion cancela el anterior.
    CONSTRAINT uq_certificado_solicitud UNIQUE (fn_solicitud_id),

    CONSTRAINT fk_certificado_solicitud FOREIGN KEY (fn_solicitud_id)
        REFERENCES solicitud (pn_id),
    CONSTRAINT fk_certificado_usuario_emisor FOREIGN KEY (fn_usuario_emisor_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_certificado_usuario_cancelacion FOREIGN KEY (fn_usuario_cancelacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_certificado_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_certificado_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),

    CONSTRAINT ck_certificado_hash CHECK (
        s_hash_documento IS NULL OR s_hash_documento ~ '^[a-f0-9]{64}$'
    ),
    -- Un certificado cancelado debe decir por que y cuando.
    CONSTRAINT ck_certificado_cancelacion CHECK (
        b_vigente = TRUE
        OR (s_motivo_cancelacion IS NOT NULL AND d_fecha_cancelacion IS NOT NULL)
    ),
    CONSTRAINT ck_certificado_firma CHECK (
        b_firmado_electronicamente = FALSE OR d_fecha_firma IS NOT NULL
    ),
    CONSTRAINT ck_certificado_entrega CHECK (
        b_sello_fisico_entregado = FALSE OR d_fecha_entrega IS NOT NULL
    )
);

CREATE INDEX ix_certificado_codigo   ON certificado (s_codigo_verificacion);
CREATE INDEX ix_certificado_vigente  ON certificado (b_vigente);
CREATE INDEX ix_certificado_emision  ON certificado (d_fecha_emision DESC);

COMMENT ON TABLE certificado IS 'Certificado de empadronamiento emitido al aprobar la solicitud';
COMMENT ON COLUMN certificado.s_codigo_verificacion IS
    'Destino del QR. El verificador publico NO muestra datos del tutor.';

-- ----------------------------------------------------------------------------
-- Cita de entrega
-- ----------------------------------------------------------------------------
CREATE TABLE cita (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    fn_solicitud_id           BIGINT       NOT NULL,
    fn_sede_id                INTEGER      NOT NULL,
    -- Referencia al catalogo cat_estatus_cita (V2). En el diseno original
    -- venia como 'fn_estatus' suelto, sin tabla referida: un prefijo fn_ exige
    -- llave foranea, asi que se normalizo.
    fn_estatus_cita_id        INTEGER      NOT NULL,

    s_numero_cita             VARCHAR(40)  NOT NULL,
    d_fecha_cita              DATE         NOT NULL,
    s_horario                 VARCHAR(60),

    -- Reagenda: apunta a la cita anterior para conservar la historia.
    fn_cita_anterior_id       BIGINT,
    s_observacion             VARCHAR(500),

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_cita PRIMARY KEY (pn_id),
    CONSTRAINT uq_cita_numero UNIQUE (s_numero_cita),
    CONSTRAINT fk_cita_solicitud FOREIGN KEY (fn_solicitud_id)
        REFERENCES solicitud (pn_id),
    CONSTRAINT fk_cita_cat_sede FOREIGN KEY (fn_sede_id)
        REFERENCES cat_sede (pn_id),
    CONSTRAINT fk_cita_cat_estatus_cita FOREIGN KEY (fn_estatus_cita_id)
        REFERENCES cat_estatus_cita (pn_id),
    CONSTRAINT fk_cita_cita_anterior FOREIGN KEY (fn_cita_anterior_id)
        REFERENCES cita (pn_id),
    CONSTRAINT fk_cita_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_cita_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id)
);

CREATE INDEX ix_cita_solicitud ON cita (fn_solicitud_id);
CREATE INDEX ix_cita_sede      ON cita (fn_sede_id);
CREATE INDEX ix_cita_estatus   ON cita (fn_estatus_cita_id);
-- Agenda del dia por sede.
CREATE INDEX ix_cita_fecha_sede ON cita (d_fecha_cita, fn_sede_id);

COMMENT ON TABLE cita IS 'Cita de entrega del documento fisico en una sede';
