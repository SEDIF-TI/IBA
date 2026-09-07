package mx.gob.sedif.sea.security.admin;

import java.time.OffsetDateTime;
import java.util.List;

import mx.gob.sedif.sea.security.Rol;
import mx.gob.sedif.sea.security.Usuario;

/**
 * Cuenta del personal tal como se muestra en pantalla.
 *
 * <p>Nunca incluye la huella de la contrasena: no le sirve a ninguna pantalla
 * y no tiene por que salir del servidor.
 */
public record UsuarioRecord(
        Long id,
        String usuario,
        String nombreCompleto,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String correo,
        String telefono,
        Integer sedeId,
        String sedeNombre,
        List<RolResumen> roles,
        boolean activo,
        boolean cambioPasswordRequerido,
        boolean bloqueada,
        OffsetDateTime ultimoAcceso) {

    public static UsuarioRecord de(Usuario usuario) {
        boolean bloqueada = usuario.getBloqueoHasta() != null
                && usuario.getBloqueoHasta().isAfter(OffsetDateTime.now());

        return new UsuarioRecord(
                usuario.getId(),
                usuario.getUsuario(),
                usuario.getNombreCompleto(),
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getSede() == null ? null : usuario.getSede().getId(),
                usuario.getSede() == null ? null : usuario.getSede().getNombre(),
                usuario.getRoles().stream()
                        .map(RolResumen::de)
                        .sorted((a, b) -> a.nombre().compareTo(b.nombre()))
                        .toList(),
                Boolean.TRUE.equals(usuario.getActivo()),
                Boolean.TRUE.equals(usuario.getCambioPasswordRequerido()),
                bloqueada,
                usuario.getUltimoAcceso());
    }

    /** Rol como se muestra al asignarlo. */
    public record RolResumen(Integer id, String clave, String nombre) {
        public static RolResumen de(Rol rol) {
            return new RolResumen(rol.getId(), rol.getClave(), rol.getNombre());
        }
    }
}
