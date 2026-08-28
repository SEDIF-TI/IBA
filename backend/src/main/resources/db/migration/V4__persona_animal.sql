-- ============================================================================
-- V4: Persona (tutor responsable), animal y titularidad.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Persona: el tutor responsable.
--
-- La identidad es la CURP, pero NO es llave primaria ni foranea: RENAPO emite
-- correcciones de CURP y no queremos propagar el cambio por diez tablas. Va
-- como columna con indice unico PARCIAL (unico solo cuando no es nulo).
--
-- La CURP es deseable pero no bloqueante: si la persona no la trae, se
-- registra con nombre completo y queda como identidad pendiente
-- (b_identidad_validada = FALSE). El personal la completa despues, ya que de
-- todos modos se pide el INE.
-- ----------------------------------------------------------------------------
CREATE TABLE persona (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,

    -- 18 caracteres. Puede ser NULL: ver nota de arriba.
    s_curp                    VARCHAR(18),
    -- FALSE mientras no se coteje la CURP contra el documento oficial.
    b_identidad_validada      BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Nombre PARTIDO EN TRES. El legado lo tenia en una sola columna y eso
    -- hacia imposible buscar por apellidos, que es justo como se busca a la
    -- gente el dia de una jornada municipal.
    s_nombre                  VARCHAR(120) NOT NULL,
    s_apellido_paterno        VARCHAR(120) NOT NULL,
    s_apellido_materno        VARCHAR(120),

    -- Contacto: es obligatorio dejar al menos uno. Lo garantiza ck_persona_contacto.
    s_correo                  VARCHAR(150),
    s_telefono                VARCHAR(20),
    s_telefono_whatsapp       VARCHAR(20),
    fn_medio_contacto_preferido_id INTEGER,

    -- Domicilio
    fn_municipio_id           INTEGER,
    s_calle                   VARCHAR(200),
    s_numero_exterior         VARCHAR(20),
    s_numero_interior         VARCHAR(20),
    s_colonia                 VARCHAR(150),
    s_codigo_postal           VARCHAR(5),

    -- Se deriva de la CURP cuando esta disponible.
    d_fecha_nacimiento        DATE,
    -- Tambien derivados de la CURP; sirven para cotejar contra lo capturado.
    s_sexo_curp               VARCHAR(1),
    s_entidad_nacimiento      VARCHAR(2),

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_persona PRIMARY KEY (pn_id),
    CONSTRAINT fk_persona_cat_municipio FOREIGN KEY (fn_municipio_id)
        REFERENCES cat_municipio (pn_id),
    CONSTRAINT fk_persona_cat_medio_contacto FOREIGN KEY (fn_medio_contacto_preferido_id)
        REFERENCES cat_medio_contacto (pn_id),
    CONSTRAINT fk_persona_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_persona_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),

    -- Forma de la CURP. El digito verificador se valida en la capa de servicio,
    -- no aqui: la regla de RENAPO es un algoritmo, no una expresion regular.
    CONSTRAINT ck_persona_curp_formato CHECK (
        s_curp IS NULL OR s_curp ~ '^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z][0-9]$'
    ),
    -- CONTACTO OBLIGATORIO: correo o telefono. Sin esto no hay como avisar
    -- el resultado del tramite, y la solicitud se vuelve un callejon sin salida.
    CONSTRAINT ck_persona_contacto CHECK (
        s_correo IS NOT NULL OR s_telefono IS NOT NULL OR s_telefono_whatsapp IS NOT NULL
    ),
    CONSTRAINT ck_persona_correo CHECK (
        s_correo IS NULL OR s_correo ~* '^[^@[:space:]]+@[^@[:space:]]+\.[a-z]{2,}$'
    ),
    CONSTRAINT ck_persona_cp CHECK (
        s_codigo_postal IS NULL OR s_codigo_postal ~ '^[0-9]{5}$'
    ),
    CONSTRAINT ck_persona_sexo_curp CHECK (
        s_sexo_curp IS NULL OR s_sexo_curp IN ('H', 'M')
    )
);

-- Indice unico PARCIAL: impide dos personas con la misma CURP, pero permite
-- muchas personas sin CURP (identidad pendiente). Un UNIQUE normal no serviria
-- aqui... en realidad si (PostgreSQL admite varios NULL en UNIQUE), pero el
-- indice parcial es mas pequeno y expresa la intencion de forma explicita.
CREATE UNIQUE INDEX uq_persona_curp ON persona (s_curp) WHERE s_curp IS NOT NULL;

-- Busqueda por apellidos en jornada: sin acentos y tolerante a errores de
-- captura. 'MUNOZ' encuentra 'MUÑOZ'.
CREATE INDEX ix_persona_apellidos_trgm ON persona
    USING gin (
        (sea_unaccent(upper(s_apellido_paterno || ' ' ||
                        coalesce(s_apellido_materno, '') || ' ' || s_nombre)))
        gin_trgm_ops
    );
CREATE INDEX ix_persona_municipio ON persona (fn_municipio_id);
CREATE INDEX ix_persona_activo    ON persona (b_activo);

COMMENT ON TABLE  persona IS 'Tutor responsable. No es una cuenta: la ciudadania no se autentica.';
COMMENT ON COLUMN persona.s_curp IS 'Identidad de la persona. Unica cuando no es nula. NUNCA se usa como llave.';
COMMENT ON COLUMN persona.b_identidad_validada IS 'TRUE solo cuando el personal coteja la CURP contra el documento';

-- ----------------------------------------------------------------------------
-- Animal
-- ----------------------------------------------------------------------------
CREATE TABLE animal (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    s_nombre                  VARCHAR(120) NOT NULL,

    fn_especie_id             INTEGER      NOT NULL,
    fn_raza_id                INTEGER,
    fn_color_id               INTEGER,
    fn_sexo_id                INTEGER      NOT NULL,
    fn_tamano_id              INTEGER,

    -- Edad NORMALIZADA en meses. El legado guardaba texto libre: '1', '1 ANO',
    -- '2 MESES'. Asi no se puede ordenar, filtrar ni sacar estadistica.
    -- La interfaz captura anos y meses, y convierte antes de guardar.
    n_edad_meses              INTEGER,
    d_fecha_nacimiento_aproximada DATE,

    -- Booleanos de verdad, no la cadena 'SI'.
    b_vacunado                BOOLEAN      NOT NULL DEFAULT FALSE,
    b_esterilizado            BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Senas particulares.
    s_caracteristicas         VARCHAR(1000),

    fn_metodo_identificacion_id INTEGER,
    -- Numero de microchip, placa o tatuaje.
    s_identificador           VARCHAR(60),

    -- Identificador interno del animal (patron configurable en parametro).
    -- El legado lo llamaba "CURP de mascota"; el nombre confunde con la CURP
    -- real de la persona, asi que aqui no se llama asi.
    s_clave_animal            VARCHAR(60),

    b_finado                  BOOLEAN      NOT NULL DEFAULT FALSE,
    d_fecha_defuncion         DATE,

    b_activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_animal PRIMARY KEY (pn_id),
    CONSTRAINT uq_animal_clave UNIQUE (s_clave_animal),
    CONSTRAINT fk_animal_cat_especie FOREIGN KEY (fn_especie_id)
        REFERENCES cat_especie (pn_id),
    CONSTRAINT fk_animal_cat_raza FOREIGN KEY (fn_raza_id)
        REFERENCES cat_raza (pn_id),
    CONSTRAINT fk_animal_cat_color FOREIGN KEY (fn_color_id)
        REFERENCES cat_color (pn_id),
    CONSTRAINT fk_animal_cat_sexo FOREIGN KEY (fn_sexo_id)
        REFERENCES cat_sexo (pn_id),
    CONSTRAINT fk_animal_cat_tamano FOREIGN KEY (fn_tamano_id)
        REFERENCES cat_tamano (pn_id),
    CONSTRAINT fk_animal_cat_metodo_identificacion FOREIGN KEY (fn_metodo_identificacion_id)
        REFERENCES cat_metodo_identificacion (pn_id),
    CONSTRAINT fk_animal_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_animal_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),

    -- 50 anos en meses: cota superior generosa que atrapa errores de captura
    -- del tipo "edad en dias".
    CONSTRAINT ck_animal_edad CHECK (
        n_edad_meses IS NULL OR n_edad_meses BETWEEN 0 AND 600
    ),
    -- Coherencia: si esta finado debe haber fecha, y viceversa.
    CONSTRAINT ck_animal_defuncion CHECK (
        (b_finado = FALSE AND d_fecha_defuncion IS NULL)
        OR (b_finado = TRUE AND d_fecha_defuncion IS NOT NULL)
    )
);
CREATE INDEX ix_animal_especie   ON animal (fn_especie_id);
CREATE INDEX ix_animal_raza      ON animal (fn_raza_id);
CREATE INDEX ix_animal_activo    ON animal (b_activo);
-- Busqueda del animal por nombre, tolerante a acentos y erratas.
CREATE INDEX ix_animal_nombre_trgm ON animal
    USING gin ((sea_unaccent(upper(s_nombre))) gin_trgm_ops);
-- El identificador de chip o placa se consulta para detectar duplicados.
CREATE INDEX ix_animal_identificador ON animal (s_identificador)
    WHERE s_identificador IS NOT NULL;

COMMENT ON COLUMN animal.n_edad_meses IS 'Edad normalizada en meses; la UI captura anos/meses y convierte';
COMMENT ON COLUMN animal.s_clave_animal IS 'Identificador interno del animal. NO es una CURP.';

-- ----------------------------------------------------------------------------
-- Titularidad: relacion persona <-> animal CON VIGENCIA.
--
-- Por que no una llave foranea directa de animal a persona: un animal se
-- transfiere, se adopta, el tutor fallece o el animal muere. Con FK directa se
-- pierde la historia y el conteo "cuantos animales tiene esta persona" se
-- vuelve ambiguo. Aqui ese conteo es el numero de titularidades vigentes, y el
-- historico completo se conserva.
-- ----------------------------------------------------------------------------
CREATE TABLE titularidad (
    pn_id                     BIGINT       GENERATED ALWAYS AS IDENTITY,
    fn_persona_id             BIGINT       NOT NULL,
    fn_animal_id              BIGINT       NOT NULL,

    d_fecha_inicio            DATE         NOT NULL DEFAULT CURRENT_DATE,
    d_fecha_fin               DATE,
    s_motivo_baja             VARCHAR(300),

    b_vigente                 BOOLEAN      NOT NULL DEFAULT TRUE,

    d_fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    d_fecha_modificacion      TIMESTAMPTZ,
    fn_usuario_creacion_id    BIGINT,
    fn_usuario_modificacion_id BIGINT,

    CONSTRAINT pk_titularidad PRIMARY KEY (pn_id),
    CONSTRAINT fk_titularidad_persona FOREIGN KEY (fn_persona_id)
        REFERENCES persona (pn_id),
    CONSTRAINT fk_titularidad_animal FOREIGN KEY (fn_animal_id)
        REFERENCES animal (pn_id),
    CONSTRAINT fk_titularidad_usuario_creacion FOREIGN KEY (fn_usuario_creacion_id)
        REFERENCES usuario (pn_id),
    CONSTRAINT fk_titularidad_usuario_modificacion FOREIGN KEY (fn_usuario_modificacion_id)
        REFERENCES usuario (pn_id),

    CONSTRAINT ck_titularidad_fechas CHECK (
        d_fecha_fin IS NULL OR d_fecha_fin >= d_fecha_inicio
    ),
    -- Coherencia: si ya no esta vigente, tiene que haber fecha de fin.
    CONSTRAINT ck_titularidad_vigencia CHECK (
        (b_vigente = TRUE AND d_fecha_fin IS NULL)
        OR (b_vigente = FALSE AND d_fecha_fin IS NOT NULL)
    )
);

-- REGLA CLAVE: un animal no puede tener dos titularidades vigentes a la vez.
-- El indice unico parcial lo impide a nivel de motor, no solo por codigo.
CREATE UNIQUE INDEX uq_titularidad_animal_vigente
    ON titularidad (fn_animal_id) WHERE b_vigente = TRUE;

CREATE INDEX ix_titularidad_persona ON titularidad (fn_persona_id);
CREATE INDEX ix_titularidad_animal  ON titularidad (fn_animal_id);
-- Soporta la ficha de la persona: sus animales vigentes.
CREATE INDEX ix_titularidad_persona_vigente
    ON titularidad (fn_persona_id) WHERE b_vigente = TRUE;

COMMENT ON TABLE titularidad IS
    'Vinculo persona-animal con vigencia. El conteo de animales de una persona
     son sus titularidades vigentes; el historico se conserva completo.';
