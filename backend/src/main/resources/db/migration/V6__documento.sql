-- ============================================================================
-- V6: Documentos adjuntos a la solicitud (INE, CURP, comprobante, etc.)
--
-- REQUISITO DE SEGURIDAD CRITICO
-- Esta tabla guarda unicamente METADATOS. El archivo vive en un volumen fuera
-- del alcance web, con nombre generado (UUID), y se entrega SOLO por un
-- endpoint autenticado que valida rol y deja registro en bitacora.
--
-- En el sistema legado los documentos personales se guardaban bajo el document
-- root con URL predecible: cualquiera podia descargar el INE de un tercero
-- adivinando la direccion. Es la falla mas grave del legado y no se reproduce.
-- ============================================================================

CREATE TABLE documento (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    fn_solicitud_id           BIGINT       NOT NULL,
    fn_tipo_documento_id      INTEGER      NOT NULL,

    -- Nombre con el que la persona subio el archivo. Solo para mostrarlo:
    -- NUNCA se usa para construir la ruta en disco (evita path traversal).
    s_nombre_original         VARCHAR(255) NOT NULL,

    -- Nombre real en disco: UUID generado. No revela contenido ni titular,
    -- y no se puede adivinar.
    s_nombre_almacenado       VARCHAR(150) NOT NULL,

    -- Ruta relativa dentro del volumen protegido. Nunca una URL publica.
    s_ruta_almacenamiento     VARCHAR(500) NOT NULL,

    -- MIME detectado por CONTENIDO (Apache Tika), no por la extension:
    -- renombrar virus.exe a foto.jpg no debe bastar para pasar.
    s_mime                    VARCHAR(120) NOT NULL,
    n_tamano_bytes            BIGINT       NOT NULL,

    -- SHA-256 del archivo: detecta duplicados y prueba que no fue alterado.
    s_hash_sha256             VARCHAR(64)  NOT NULL,

    -- El validador marca los ilegibles. Problema recurrente con el INE: en el
    -- legado habia que perseguir por WhatsApp a la persona para que reenviara.
    -- NULL = todavia no se revisa.
    b_legible                 BOOLEAN,
    s_observacion_revision    VARCHAR(500),
    fn_usuario_revision_id    BIGINT,
    d_fecha_revision          TIMESTAMPTZ,

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_documento PRIMARY KEY (pn_id),
    CONSTRAINT uq_documento_nombre_almacenado UNIQUE (s_nombre_almacenado),
    CONSTRAINT fk_documento_solicitud FOREIGN KEY (fn_solicitud_id)
        REFERENCES solicitud (pn_id) ON DELETE CASCADE,
    CONSTRAINT fk_documento_cat_tipo FOREIGN KEY (fn_tipo_documento_id)
        REFERENCES cat_tipo_documento (pn_id),
    CONSTRAINT fk_documento_usuario_revision FOREIGN KEY (fn_usuario_revision_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_documento_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_documento_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),

    CONSTRAINT ck_documento_tamano CHECK (n_tamano_bytes > 0),
    CONSTRAINT ck_documento_hash CHECK (s_hash_sha256 ~ '^[a-f0-9]{64}$'),
    -- Si ya se reviso, debe constar quien y cuando.
    CONSTRAINT ck_documento_revision CHECK (
        (b_legible IS NULL AND fn_usuario_revision_id IS NULL AND d_fecha_revision IS NULL)
        OR (b_legible IS NOT NULL AND fn_usuario_revision_id IS NOT NULL AND d_fecha_revision IS NOT NULL)
    )
);

CREATE INDEX ix_documento_solicitud ON documento (fn_solicitud_id);
CREATE INDEX ix_documento_tipo      ON documento (fn_tipo_documento_id);
CREATE INDEX ix_documento_hash      ON documento (s_hash_sha256);
-- Los pendientes de revisar.
CREATE INDEX ix_documento_por_revisar ON documento (fn_solicitud_id)
    WHERE b_legible IS NULL AND b_activo = TRUE;

COMMENT ON TABLE documento IS
    'Metadatos. El archivo vive fuera del document root y solo sale por endpoint autenticado.';
COMMENT ON COLUMN documento.s_nombre_almacenado IS 'UUID generado. Nunca el nombre que subio la persona.';
COMMENT ON COLUMN documento.s_mime IS 'MIME detectado por contenido con Tika, no por extension.';
COMMENT ON COLUMN documento.b_legible IS 'NULL = sin revisar. FALSE = ilegible, hay que pedir reenvio.';
