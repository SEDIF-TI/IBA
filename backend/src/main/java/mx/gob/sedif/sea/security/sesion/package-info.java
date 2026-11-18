/**
 * Sesiones revocables del personal administrativo.
 * Área: Núcleo compartido (seguridad).
 *
 * <p>Implementa la rotación del token de refresco (cada uno sirve una sola vez), el cierre real de
 * sesión y la revocación de todas las sesiones de una cuenta al cambiar o reiniciar su contraseña
 * o al darla de baja.
 */
package mx.gob.sedif.sea.security.sesion;
