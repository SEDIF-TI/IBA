-- ============================================================================
-- V17: Permiso para depurar el padron.
--
-- Da de baja registros que nunca fueron reales: gente que probo el sistema, o
-- datos evidentemente falsos. Se separa de solicitud:cancelar porque son cosas
-- distintas: cancelar es resolver un tramite legitimo que no procede; depurar
-- es reconocer que ese registro no debio existir.
--
-- Es un permiso aparte para poder darlo solo a quien deba tenerlo: borrar del
-- padron, aunque sea de forma logica, no es una accion de captura diaria.
-- ============================================================================

INSERT INTO cat_permiso (s_clave, s_nombre, s_modulo, n_orden) VALUES
    ('solicitud:depurar', 'Dar de baja registros del padron', 'solicitud', 8);

-- El superadministrador recibe todos los permisos.
INSERT INTO rol_permiso (fn_rol_id, fn_permiso_id)
SELECT r.pn_id, p.pn_id
FROM cat_rol r, cat_permiso p
WHERE r.s_clave = 'SUPERADMINISTRADOR'
  AND p.s_clave = 'solicitud:depurar';

-- El administrador del IBA tambien: es quien conoce el padron y puede
-- distinguir un registro de prueba de uno legitimo.
INSERT INTO rol_permiso (fn_rol_id, fn_permiso_id)
SELECT r.pn_id, p.pn_id
FROM cat_rol r, cat_permiso p
WHERE r.s_clave = 'ADMINISTRADOR'
  AND p.s_clave = 'solicitud:depurar';
