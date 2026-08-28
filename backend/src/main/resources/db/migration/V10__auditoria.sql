-- ============================================================================
-- V10: Bitacora de auditoria.
--
-- Registra como minimo: aprobaciones, rechazos, descargas de documentos
-- personales, CONSULTAS POR CURP, cambios en catalogos y altas/bajas de
-- usuarios.
--
-- El legado no registraba nada: no habia forma de saber quien aprobo un
-- tramite, ni quien consulto los datos de una persona.
-- ============================================================================

CREATE TABLE bitacora_auditoria (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,

    -- NULL cuando la accion la origina el portal publico (sin sesion).
    fn_usuario_id             BIGINT,

    -- Nombre de la entidad afectada: 'solicitud', 'persona', 'documento'...
    s_entidad                 VARCHAR(80)  NOT NULL,
    -- Id del registro afectado. No es llave foranea: apunta a cualquier tabla,
    -- por eso lleva prefijo n_ y no fn_.
    n_registro_id             BIGINT,

    -- APROBAR, RECHAZAR, OBSERVAR, CONSULTAR_CURP, DESCARGAR_DOCUMENTO,
    -- CREAR, MODIFICAR, ELIMINAR, LOGIN, LOGIN_FALLIDO...
    s_accion                  VARCHAR(60)  NOT NULL,

    -- Estado antes y despues, en JSONB. Permite reconstruir que cambio
    -- exactamente sin crear una tabla espejo por cada entidad.
    j_valor_anterior          JSONB,
    j_valor_nuevo             JSONB,

    s_ip                      INET,
    s_user_agent              VARCHAR(400),
    -- Descripcion legible para la pantalla de consulta de bitacora.
    s_detalle                 VARCHAR(1000),

    d_fecha                   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_bitacora_auditoria PRIMARY KEY (pn_id),
    CONSTRAINT fk_bitacora_auditoria_usuario FOREIGN KEY (fn_usuario_id)
        REFERENCES usuario (pn_id)
);

CREATE INDEX ix_bitacora_usuario  ON bitacora_auditoria (fn_usuario_id, d_fecha DESC);
CREATE INDEX ix_bitacora_entidad  ON bitacora_auditoria (s_entidad, n_registro_id);
CREATE INDEX ix_bitacora_accion   ON bitacora_auditoria (s_accion, d_fecha DESC);
CREATE INDEX ix_bitacora_fecha    ON bitacora_auditoria (d_fecha DESC);
-- Consulta frecuente de control: quien anduvo viendo CURP y documentos.
CREATE INDEX ix_bitacora_datos_personales
    ON bitacora_auditoria (d_fecha DESC)
    WHERE s_accion IN ('CONSULTAR_CURP', 'DESCARGAR_DOCUMENTO');

COMMENT ON TABLE bitacora_auditoria IS
    'Bitacora de acciones sensibles. Toda consulta por CURP y toda descarga de
     documento personal queda registrada aqui.';
COMMENT ON COLUMN bitacora_auditoria.n_registro_id IS
    'Id del registro afectado. No es FK: apunta a cualquier entidad del sistema.';
