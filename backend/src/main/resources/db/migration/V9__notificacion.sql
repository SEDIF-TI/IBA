-- ============================================================================
-- V9: Notificaciones y plantillas.
--
-- Los textos de los avisos son ADMINISTRABLES: viven en plantilla_notificacion,
-- no en el codigo. La institucion cambia de SMADSOT a DIF y las leyendas van a
-- cambiar; no se recompila por eso.
-- ============================================================================

CREATE TABLE plantilla_notificacion (
    pn_id                     INTEGER      GENERATED ALWAYS AS IDENTITY,
    s_clave                   VARCHAR(60)  NOT NULL,
    s_nombre                  VARCHAR(150) NOT NULL,
    s_descripcion             VARCHAR(500),

    s_asunto                  VARCHAR(300) NOT NULL,
    -- Admite variables tipo {{folio}}, {{nombre}}, {{sede}}, {{fecha_cita}}.
    s_cuerpo                  TEXT         NOT NULL,

    -- Canal por el que sale este aviso (correo, telefono, WhatsApp).
    fn_medio_contacto_id      INTEGER      NOT NULL,

    -- Documenta que variables acepta la plantilla, para mostrarlas como ayuda
    -- en el editor del panel.
    s_variables_disponibles   VARCHAR(500),

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_plantilla_notificacion PRIMARY KEY (pn_id),
    CONSTRAINT uq_plantilla_notificacion_clave UNIQUE (s_clave),
    CONSTRAINT fk_plantilla_notificacion_medio FOREIGN KEY (fn_medio_contacto_id)
        REFERENCES cat_medio_contacto (pn_id),
    CONSTRAINT fk_plantilla_notificacion_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_plantilla_notificacion_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id)
);

COMMENT ON TABLE plantilla_notificacion IS
    'Textos de los avisos, editables desde el panel. Nunca quemados en el codigo.';

-- ----------------------------------------------------------------------------
-- Notificacion enviada o por enviar.
--
-- BANDEJA DE CONTACTO MANUAL: cuando la persona solo dejo telefono, la
-- notificacion se crea con canal MANUAL y estatus PENDIENTE. El personal la ve
-- en una bandeja, llama o escribe por WhatsApp, y marca el contacto como
-- realizado con su observacion.
--
-- Si esto no se modela, esas notificaciones simplemente no ocurren y nadie se
-- entera de que hay gente esperando respuesta.
-- ----------------------------------------------------------------------------
CREATE TABLE notificacion (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    fn_solicitud_id           BIGINT       NOT NULL,
    fn_plantilla_id           INTEGER,

    -- AUTOMATICO (correo) o MANUAL (llamada / WhatsApp que hace una persona).
    s_canal                   VARCHAR(20)  NOT NULL,

    -- Direccion de correo o numero telefonico, segun el canal.
    s_destinatario            VARCHAR(200) NOT NULL,

    -- Texto ya resuelto, con las variables sustituidas. Se guarda para tener
    -- constancia exacta de lo que se le dijo a la persona, aunque despues se
    -- edite la plantilla.
    s_asunto                  VARCHAR(300),
    s_cuerpo                  TEXT,

    -- PENDIENTE, ENVIADA, FALLIDA, CONTACTADA, NO_CONTACTADA
    s_estatus_envio           VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    d_fecha_envio             TIMESTAMPTZ,
    n_intentos                INTEGER      NOT NULL DEFAULT 0,
    s_error                   VARCHAR(1000),

    -- Solo para el canal MANUAL: quien hizo el contacto y que resulto.
    fn_usuario_contacto_id    BIGINT,
    d_fecha_contacto          TIMESTAMPTZ,
    s_observacion_contacto    VARCHAR(1000),

    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,

    CONSTRAINT pk_notificacion PRIMARY KEY (pn_id),
    CONSTRAINT fk_notificacion_solicitud FOREIGN KEY (fn_solicitud_id)
        REFERENCES solicitud (pn_id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_plantilla FOREIGN KEY (fn_plantilla_id)
        REFERENCES plantilla_notificacion (pn_id),
    CONSTRAINT fk_notificacion_usuario_contacto FOREIGN KEY (fn_usuario_contacto_id)
        REFERENCES usuario (pn_id),

    CONSTRAINT ck_notificacion_canal CHECK (
        s_canal IN ('AUTOMATICO', 'MANUAL')
    ),
    CONSTRAINT ck_notificacion_estatus CHECK (
        s_estatus_envio IN ('PENDIENTE', 'ENVIADA', 'FALLIDA', 'CONTACTADA', 'NO_CONTACTADA')
    ),
    CONSTRAINT ck_notificacion_intentos CHECK (n_intentos >= 0),
    -- Un contacto manual resuelto debe decir quien lo hizo y cuando.
    CONSTRAINT ck_notificacion_contacto_manual CHECK (
        s_estatus_envio NOT IN ('CONTACTADA', 'NO_CONTACTADA')
        OR (fn_usuario_contacto_id IS NOT NULL AND d_fecha_contacto IS NOT NULL)
    )
);

CREATE INDEX ix_notificacion_solicitud ON notificacion (fn_solicitud_id);
CREATE INDEX ix_notificacion_estatus   ON notificacion (s_estatus_envio);
-- LA BANDEJA DE CONTACTO MANUAL: lo que el personal tiene que llamar hoy.
CREATE INDEX ix_notificacion_bandeja_manual
    ON notificacion (d_fecha_creacion)
    WHERE s_canal = 'MANUAL' AND s_estatus_envio = 'PENDIENTE';
-- Reintentos de los envios automaticos fallidos.
CREATE INDEX ix_notificacion_reintento
    ON notificacion (n_intentos)
    WHERE s_canal = 'AUTOMATICO' AND s_estatus_envio IN ('PENDIENTE', 'FALLIDA');

COMMENT ON TABLE notificacion IS
    'Avisos del tramite. Canal MANUAL alimenta la bandeja de contacto telefonico.';
COMMENT ON COLUMN notificacion.s_cuerpo IS
    'Texto ya resuelto: constancia de lo que se comunico, aunque la plantilla cambie despues.';
