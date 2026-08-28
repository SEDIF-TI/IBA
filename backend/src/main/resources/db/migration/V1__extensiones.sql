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

-- ----------------------------------------------------------------------------
-- Envoltorio IMMUTABLE de unaccent().
--
-- unaccent() se declara STABLE porque depende de un diccionario que en teoria
-- puede cambiar; PostgreSQL entonces la rechaza dentro de un indice. En la
-- practica el diccionario es fijo, asi que se envuelve en una funcion
-- IMMUTABLE para poder indexar busquedas sin acentos.
--
-- Se fija el search_path para que no dependa del esquema activo de la sesion.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION sea_unaccent(texto TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
STRICT
SET search_path = public, pg_catalog
AS $$
    SELECT public.unaccent('public.unaccent'::regdictionary, texto);
$$;

COMMENT ON FUNCTION sea_unaccent(TEXT) IS
    'unaccent() IMMUTABLE para poder usarse en indices de busqueda por nombre';
