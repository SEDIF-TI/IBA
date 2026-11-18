-- ============================================================================
-- V19: Bloqueo optimista en todas las tablas editables.
--
-- Por qué: varias personas del IBA pueden abrir el mismo trámite, tutor,
-- jornada o catálogo a la vez. Sin control de versión, la última en guardar
-- sobrescribía en silencio los cambios de la otra.
--
-- Cada fila lleva n_version; Hibernate la incrementa en cada actualización y
-- rechaza el cambio si la versión ya no coincide (la aplicación responde 409).
-- Son las tablas cuya entidad hereda de Auditable; las filas existentes
-- arrancan en 0.
-- ============================================================================

ALTER TABLE animal ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_color ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_especie ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_estatus_cita ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_estatus_solicitud ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_medio_contacto ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_metodo_identificacion ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_motivo_rechazo ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_municipio ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_permiso ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_raza ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_rol ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_sede ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_sexo ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_tamano ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cat_tipo_documento ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE certificado ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cita ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE documento ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE jornada ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE parametro ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE persona ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE plantilla_notificacion ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE solicitud ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE titularidad ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN n_version BIGINT NOT NULL DEFAULT 0;

