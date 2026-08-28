-- ============================================================================
-- V1: Extensiones de PostgreSQL requeridas por el sistema.
-- ============================================================================

-- Generacion de UUID en la BD. Se usa para:
--   solicitud.s_token_consulta       token opaco de seguimiento publico
--   certificado.s_codigo_verificacion codigo del QR de verificacion
--   documento.s_nombre_almacenado    nombre generado del archivo en disco
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Busqueda por similitud (trigramas). Necesaria para el buscador del padron:
-- en jornada se busca por apellidos con errores de captura y acentos.
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Quita acentos en las busquedas: 'MUNOZ' encuentra 'MUÑOZ', 'PEREZ' a 'PÉREZ'.
CREATE EXTENSION IF NOT EXISTS "unaccent";
