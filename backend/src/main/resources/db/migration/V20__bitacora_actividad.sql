-- ============================================================================
-- V20: Bitácora de toda la actividad, de solo inserción, y ajuste de permisos.
--
-- 1. Columnas para registrar cada petición al API (filtro de actividad):
--    método, ruta, código HTTP, resultado y duración. Con ellas quedan las
--    consultas, altas, ediciones, bajas, accesos denegados y errores, además de
--    los eventos explícitos que ya existían (aprobaciones, descargas, accesos).
--
-- 2. Disparadores que impiden modificar o borrar la bitácora, incluso a la
--    cuenta dueña del esquema. La cuenta de la aplicación tampoco puede
--    quitarlos: no es dueña de la tabla.
--
-- 3. Permisos:
--    - bitacora:ver queda solo para SUPERADMINISTRADOR (informática): la
--      bitácora la consulta únicamente quien administra el sistema. El IBA
--      sigue viendo el expediente de cada trámite (solicitud_historial).
--    - documento:subir separa la carga de documentos de su consulta; antes la
--      carga se autorizaba con documento:ver. Ambos roles lo reciben, así que
--      nadie pierde lo que ya podía hacer.
-- ============================================================================

ALTER TABLE bitacora_auditoria
    ADD COLUMN s_metodo       VARCHAR(10),
    ADD COLUMN s_ruta         VARCHAR(300),
    ADD COLUMN n_codigo_http  SMALLINT,
    -- EXITO, RECHAZADO (4xx de validación o regla), DENEGADO (401/403), ERROR (5xx).
    ADD COLUMN s_resultado    VARCHAR(20),
    ADD COLUMN n_duracion_ms  INTEGER,
    ADD CONSTRAINT ck_bitacora_resultado CHECK (
        s_resultado IS NULL OR s_resultado IN ('EXITO', 'RECHAZADO', 'DENEGADO', 'ERROR'));

-- Revisión de control: accesos denegados y errores recientes.
CREATE INDEX ix_bitacora_incidencias ON bitacora_auditoria (d_fecha DESC)
    WHERE s_resultado IN ('DENEGADO', 'ERROR');

COMMENT ON COLUMN bitacora_auditoria.s_ruta IS
    'Ruta del API sin parámetros de consulta: los valores buscados nunca se guardan, solo los nombres de los filtros (en s_detalle).';

CREATE FUNCTION bitacora_solo_insercion() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'La bitácora es de solo inserción: no se permite %', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

CREATE TRIGGER tg_bitacora_sin_cambios
    BEFORE UPDATE OR DELETE ON bitacora_auditoria
    FOR EACH ROW EXECUTE FUNCTION bitacora_solo_insercion();

CREATE TRIGGER tg_bitacora_sin_truncate
    BEFORE TRUNCATE ON bitacora_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION bitacora_solo_insercion();

-- bitacora:ver solo para informática.
DELETE FROM rol_permiso
WHERE fn_rol_id = (SELECT pn_id FROM cat_rol WHERE s_clave = 'ADMINISTRADOR')
  AND fn_permiso_id = (SELECT pn_id FROM cat_permiso WHERE s_clave = 'bitacora:ver');

-- documento:subir para ambos roles.
INSERT INTO cat_permiso (s_clave, s_nombre, s_modulo, n_orden) VALUES
    ('documento:subir', 'Adjuntar documentos a un trámite', 'documento', 14);

INSERT INTO rol_permiso (fn_rol_id, fn_permiso_id)
SELECT r.pn_id, p.pn_id
FROM cat_rol r, cat_permiso p
WHERE r.s_clave IN ('SUPERADMINISTRADOR', 'ADMINISTRADOR')
  AND p.s_clave = 'documento:subir';
