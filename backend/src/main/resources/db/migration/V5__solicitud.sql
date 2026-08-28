-- ============================================================================
-- V5: Solicitud. El corazon del sistema.
--
-- Maquina de estados (las transiciones validas se aplican en la capa de
-- servicio; aqui la BD garantiza la integridad de los datos):
--
--   REGISTRADA -> EN_REVISION -> APROBADA -> CERTIFICADO_EMITIDO -> ENTREGADA
--                      |
--                      +-> OBSERVADA -> EN_REVISION  (correccion por liga)
--                      |
--                      +-> RECHAZADA
--
--   Desde cualquier estado no terminal: -> CANCELADA
--
-- DECISION (confirmada con el area): al corregir una solicitud OBSERVADA se
-- CONSERVA el folio original. Es el mismo tramite y la persona ya lo tiene
-- anotado; las idas y vueltas quedan en solicitud_historial.
-- ============================================================================

CREATE TABLE solicitud (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,

    -- Folio visible para la ciudadania. Patron configurable en 'parametro'
    -- (V11), no quemado en el codigo: la institucion pasa de SMADSOT a DIF.
    s_folio_solicitud         VARCHAR(60)  NOT NULL,

    -- Token opaco para el seguimiento publico y la liga de correccion.
    -- Es un UUID sin significado: no se puede adivinar a partir del folio.
    s_token_consulta          UUID         NOT NULL DEFAULT gen_random_uuid(),

    fn_persona_id             BIGINT       NOT NULL,
    fn_animal_id              BIGINT       NOT NULL,
    fn_estatus_id             INTEGER      NOT NULL,

    -- Nullable: solo las solicitudes precargadas para una jornada la llevan.
    -- La FK se agrega en V8, cuando ya existe la tabla jornada.
    fn_jornada_id             BIGINT,

    -- Por donde entro el tramite: PUBLICO, JORNADA o VENTANILLA.
    s_canal                   VARCHAR(20)  NOT NULL,

    d_fecha_registro          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_resolucion        TIMESTAMPTZ,

    fn_usuario_resolucion_id  BIGINT,
    fn_motivo_rechazo_id      INTEGER,
    s_observacion_resolucion  VARCHAR(1000),

    -- La marcan las reglas anti-suplantacion: la CURP no cuadra con el nombre,
    -- o los datos de contacto difieren de los que ya tenia esa persona.
    -- Como no hay autenticacion ciudadana, es el unico control disponible.
    b_revision_reforzada      BOOLEAN      NOT NULL DEFAULT FALSE,
    s_motivo_revision_reforzada VARCHAR(500),

    -- EVIDENCIA DE CONSENTIMIENTO. Sin cuenta de usuario, esta es la unica
    -- constancia de que la persona acepto el tratamiento de sus datos
    -- personales. Es obligatoria: por eso las tres columnas son NOT NULL.
    s_aviso_privacidad_version VARCHAR(20) NOT NULL,
    d_fecha_aceptacion_aviso  TIMESTAMPTZ  NOT NULL,
    s_ip_origen               INET         NOT NULL,

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_solicitud PRIMARY KEY (pn_id),
    CONSTRAINT uq_solicitud_folio UNIQUE (s_folio_solicitud),
    CONSTRAINT uq_solicitud_token UNIQUE (s_token_consulta),

    CONSTRAINT fk_solicitud_persona FOREIGN KEY (fn_persona_id)
        REFERENCES persona (pn_id),
    CONSTRAINT fk_solicitud_animal FOREIGN KEY (fn_animal_id)
        REFERENCES animal (pn_id),
    CONSTRAINT fk_solicitud_cat_estatus FOREIGN KEY (fn_estatus_id)
        REFERENCES cat_estatus_solicitud (pn_id),
    CONSTRAINT fk_solicitud_cat_motivo_rechazo FOREIGN KEY (fn_motivo_rechazo_id)
        REFERENCES cat_motivo_rechazo (pn_id),
    CONSTRAINT fk_solicitud_usuario_resolucion FOREIGN KEY (fn_usuario_resolucion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_solicitud_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_solicitud_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),

    CONSTRAINT ck_solicitud_canal CHECK (
        s_canal IN ('PUBLICO', 'JORNADA', 'VENTANILLA')
    ),
    -- Si ya se resolvio, debe constar quien y cuando. Sin esto no hay
    -- rendicion de cuentas: el legado no registraba quien aprobo o rechazo.
    CONSTRAINT ck_solicitud_resolucion CHECK (
        (d_fecha_resolucion IS NULL AND fn_usuario_resolucion_id IS NULL)
        OR (d_fecha_resolucion IS NOT NULL AND fn_usuario_resolucion_id IS NOT NULL)
    ),
    -- Coherencia de la marca de revision reforzada con su motivo.
    CONSTRAINT ck_solicitud_revision_reforzada CHECK (
        b_revision_reforzada = FALSE OR s_motivo_revision_reforzada IS NOT NULL
    )
);

CREATE INDEX ix_solicitud_estatus    ON solicitud (fn_estatus_id);
CREATE INDEX ix_solicitud_persona    ON solicitud (fn_persona_id);
CREATE INDEX ix_solicitud_animal     ON solicitud (fn_animal_id);
CREATE INDEX ix_solicitud_jornada    ON solicitud (fn_jornada_id);
CREATE INDEX ix_solicitud_fecha      ON solicitud (d_fecha_registro DESC);
CREATE INDEX ix_solicitud_canal      ON solicitud (s_canal);
-- Bandeja de trabajo: lo pendiente, lo mas antiguo primero.
CREATE INDEX ix_solicitud_bandeja    ON solicitud (fn_estatus_id, d_fecha_registro);
-- Las marcadas para revision reforzada se revisan aparte.
CREATE INDEX ix_solicitud_reforzada  ON solicitud (b_revision_reforzada)
    WHERE b_revision_reforzada = TRUE;

COMMENT ON TABLE solicitud IS 'Tramite de empadronamiento. Un animal por solicitud.';
COMMENT ON COLUMN solicitud.s_token_consulta IS
    'UUID opaco para seguimiento publico y liga de correccion. No derivable del folio.';
COMMENT ON COLUMN solicitud.s_ip_origen IS
    'Evidencia de consentimiento del aviso de privacidad. Obligatoria.';
COMMENT ON COLUMN solicitud.fn_animal_id IS
    'El animal de ESTE tramite. El vinculo persona-animal se lee de titularidad, nunca de aqui.';

-- ----------------------------------------------------------------------------
-- Historial de la solicitud: un renglon por cada cambio de estatus.
--
-- TODA transicion escribe aqui. Es la bitacora del tramite: quien lo movio,
-- cuando, de que estado a cual y por que. El legado no tenia nada de esto.
-- ----------------------------------------------------------------------------
CREATE TABLE solicitud_historial (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    fn_solicitud_id           BIGINT       NOT NULL,

    -- NULL solo en el primer renglon: el alta, que no viene de otro estado.
    fn_estatus_anterior_id    INTEGER,
    fn_estatus_nuevo_id       INTEGER      NOT NULL,

    -- NULL cuando el cambio lo produce el registro publico (sin sesion).
    fn_usuario_id             BIGINT,

    fn_motivo_rechazo_id      INTEGER,
    s_observacion             VARCHAR(1000),
    d_fecha                   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_solicitud_historial PRIMARY KEY (pn_id),
    CONSTRAINT fk_solicitud_historial_solicitud FOREIGN KEY (fn_solicitud_id)
        REFERENCES solicitud (pn_id) ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_historial_estatus_anterior FOREIGN KEY (fn_estatus_anterior_id)
        REFERENCES cat_estatus_solicitud (pn_id),
    CONSTRAINT fk_solicitud_historial_estatus_nuevo FOREIGN KEY (fn_estatus_nuevo_id)
        REFERENCES cat_estatus_solicitud (pn_id),
    CONSTRAINT fk_solicitud_historial_usuario FOREIGN KEY (fn_usuario_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_solicitud_historial_motivo FOREIGN KEY (fn_motivo_rechazo_id)
        REFERENCES cat_motivo_rechazo (pn_id),
    -- Un cambio de estatus va de un estado a otro distinto.
    CONSTRAINT ck_solicitud_historial_transicion CHECK (
        fn_estatus_anterior_id IS NULL
        OR fn_estatus_anterior_id <> fn_estatus_nuevo_id
    )
);

CREATE INDEX ix_solicitud_historial_solicitud
    ON solicitud_historial (fn_solicitud_id, d_fecha DESC);
CREATE INDEX ix_solicitud_historial_usuario ON solicitud_historial (fn_usuario_id);

COMMENT ON TABLE solicitud_historial IS
    'Un renglon por transicion de estado. Toda transicion escribe aqui, sin excepcion.';
