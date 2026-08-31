package mx.gob.sedif.sea.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica cada peticion a partir del token de la cabecera Authorization.
 *
 * <p>Si no hay token o no es valido, deja pasar la peticion sin autenticar: sera
 * la cadena de seguridad la que decida si ese destino exige sesion. Asi los
 * endpoints publicos funcionan sin token y los protegidos responden 401.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtTokenProvider tokenProvider,
                         UserDetailsServiceImpl userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena)
            throws ServletException, IOException {

        String token = extraerToken(peticion);

        if (token != null && tokenProvider.esValido(token)) {
            try {
                // Un token de refresco no sirve para acceder a los recursos:
                // solo vale en el endpoint que emite uno nuevo de acceso.
                if (tokenProvider.esTokenAcceso(token)) {
                    autenticar(token, peticion);
                }
            } catch (UsernameNotFoundException ex) {
                // El token es valido pero la cuenta ya no existe: se dio de baja
                // mientras la sesion seguia abierta.
                log.debug("Token de una cuenta inexistente");
            } catch (Exception ex) {
                log.warn("No se pudo establecer la sesion a partir del token", ex);
            }
        }

        cadena.doFilter(peticion, respuesta);
    }

    /** Coloca al usuario en el contexto de seguridad de esta peticion. */
    private void autenticar(String token, HttpServletRequest peticion) {
        String nombreUsuario = tokenProvider.obtenerUsuario(token);
        if (nombreUsuario == null
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        UserDetails usuario = userDetailsService.loadUserByUsername(nombreUsuario);

        // Cuenta dada de baja o bloqueada despues de emitirse el token.
        if (!usuario.isEnabled() || !usuario.isAccountNonLocked()) {
            log.debug("Cuenta inactiva o bloqueada: {}", nombreUsuario);
            return;
        }

        UsernamePasswordAuthenticationToken autenticacion =
                new UsernamePasswordAuthenticationToken(
                        usuario, null, usuario.getAuthorities());
        autenticacion.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(peticion));

        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }

    /** Lee el token de la cabecera Authorization. */
    private String extraerToken(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader(CABECERA);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            String token = cabecera.substring(PREFIJO.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
