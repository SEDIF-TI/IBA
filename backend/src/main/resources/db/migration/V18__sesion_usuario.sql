-- ============================================================================
-- V18: Sesiones revocables del personal.
--
-- Por qué: hasta V17 el token de refresco valía siete días pasara lo que pasara;
-- cerrar sesión solo borraba la cookie del navegador y reiniciar una contraseña
-- no echaba a quien ya estuviera dentro con la anterior.
--
-- 1. usuario.n_token_version: viaja dentro de los tokens. Se incrementa al
--    cambiar o reiniciar la contraseña y al dar de baja la cuenta; todo token
--    emitido con una versión anterior deja de servir en la siguiente petición.
-- 2. sesion_usuario: una fila por dispositivo con sesión abierta. Guarda la
--    huella SHA-256 del identificador de un solo uso del token de refresco
--    vigente; cada renovación lo cambia (rotación). Si llega uno ya usado, la
--    sesión se revoca porque significa que alguien copió el token.
-- ============================================================================

ALTER TABLE usuario
    ADD COLUMN n_token_version INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN usuario.n_token_version IS
    'Versión de los tokens. Subirla invalida de inmediato todas las sesiones de la cuenta.';

CREATE TABLE sesion_usuario (
    pn_id                 BIGINT       GENERATED ALWAYS AS IDENTITY,
    -- Identificador público de la sesión; viaja en el token de refresco (claim sid).
    s_identificador       UUID         NOT NULL,
    fn_usuario_id         BIGINT       NOT NULL,
    -- SHA-256 en hexadecimal del identificador de un solo uso vigente (claim jti).
    -- Nunca se guarda el token ni el identificador en claro.
    s_huella_refresco     VARCHAR(64)  NOT NULL,
    d_fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_ultimo_uso          TIMESTAMPTZ,
    d_expira              TIMESTAMPTZ  NOT NULL,
    -- NULL mientras la sesión siga abierta.
    d_revocada            TIMESTAMPTZ,
    s_motivo_revocacion   VARCHAR(40),
    s_ip                  VARCHAR(45),
    s_user_agent          VARCHAR(400),

    CONSTRAINT pk_sesion_usuario PRIMARY KEY (pn_id),
    CONSTRAINT uq_sesion_usuario_identificador UNIQUE (s_identificador),
    CONSTRAINT fk_sesion_usuario_usuario FOREIGN KEY (fn_usuario_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT ck_sesion_usuario_huella CHECK (s_huella_refresco ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_sesion_usuario_abiertas ON sesion_usuario (fn_usuario_id)
    WHERE d_revocada IS NULL;
CREATE INDEX ix_sesion_usuario_expira ON sesion_usuario (d_expira);

COMMENT ON TABLE sesion_usuario IS
    'Sesiones del panel por dispositivo. Permite cerrar sesión de verdad y detectar tokens de refresco copiados.';
