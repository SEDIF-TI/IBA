package mx.gob.sedif.sea.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.exception.MessageConstants;

/**
 * Carga al usuario administrativo para que Spring Security lo autentique.
 *
 * <p>Las autoridades incluyen tanto los roles como los permisos concretos, para
 * poder proteger un endpoint por rol o por permiso segun convenga.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByUsuarioIgnoreCase(nombreUsuario)
                // Mismo mensaje que para contrasena incorrecta: distinguirlos
                // permitiria averiguar que cuentas existen.
                .orElseThrow(() -> new UsernameNotFoundException(
                        MessageConstants.CREDENCIALES_INVALIDAS));

        Set<String> autoridades = new HashSet<>();
        usuario.getRoles().forEach(rol -> {
            autoridades.add(rol.getClave());
            rol.getPermisos().forEach(permiso -> autoridades.add(permiso.getClave()));
        });

        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getUsuario(),
                usuario.getPasswordHash(),
                Boolean.TRUE.equals(usuario.getActivo()),
                Boolean.TRUE.equals(usuario.getCambioPasswordRequerido()),
                !usuario.estaBloqueado(),
                autoridades);
    }
}
