package mx.gob.sedif.sea.security.auth;

import java.util.List;

/**
 * Respuesta de un inicio de sesion correcto.
 *
 * <p>El token de refresco NO viaja aqui: va en una cookie HttpOnly, fuera del
 * alcance de JavaScript. Devolverlo en el cuerpo permitiria robarlo con una
 * inyeccion de script.
 *
 * @param tokenAcceso             token para la cabecera Authorization
 * @param tipo                    esquema de autorizacion
 * @param expiraEn                vigencia del token de acceso, en milisegundos
 * @param usuario                 nombre de usuario
 * @param nombreCompleto          nombre para mostrar en pantalla
 * @param roles                   roles asignados
 * @param permisos                permisos concretos, para pintar la interfaz
 * @param cambioPasswordRequerido si debe cambiar la contrasena antes de operar
 */
public record JwtResponse(
        String tokenAcceso,
        String tipo,
        long expiraEn,
        String usuario,
        String nombreCompleto,
        List<String> roles,
        List<String> permisos,
        boolean cambioPasswordRequerido) {

    public static JwtResponse de(String tokenAcceso, long expiraEn, String usuario,
                                 String nombreCompleto, List<String> roles,
                                 List<String> permisos, boolean cambioPasswordRequerido) {
        return new JwtResponse(tokenAcceso, "Bearer", expiraEn, usuario, nombreCompleto,
                roles, permisos, cambioPasswordRequerido);
    }
}
