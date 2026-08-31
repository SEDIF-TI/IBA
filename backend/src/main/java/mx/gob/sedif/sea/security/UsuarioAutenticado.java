package mx.gob.sedif.sea.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal autenticado del panel administrativo.
 *
 * <p>Solo el personal del IBA y del DIF tiene cuenta: la ciudadania nunca se
 * autentica. Lleva el id del usuario porque las columnas de auditoria y la
 * bitacora necesitan saber quien aprobo, rechazo o consulto una CURP.
 *
 * <p>La contrasena se descarta despues de autenticar: nunca viaja en el token
 * ni sale en una respuesta.
 */
public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String usuario;
    private transient String passwordHash;
    private final boolean activo;
    private final boolean cambioPasswordRequerido;
    private final boolean cuentaNoBloqueada;
    private final List<GrantedAuthority> autoridades;

    /**
     * @param roles    claves de rol, por ejemplo ADMINISTRADOR
     * @param permisos claves de permiso, por ejemplo documento:descargar
     */
    public UsuarioAutenticado(Long id,
                              String usuario,
                              String passwordHash,
                              boolean activo,
                              boolean cambioPasswordRequerido,
                              boolean cuentaNoBloqueada,
                              Collection<String> roles,
                              Collection<String> permisos) {
        this.id = id;
        this.usuario = usuario;
        this.passwordHash = passwordHash;
        this.activo = activo;
        this.cambioPasswordRequerido = cambioPasswordRequerido;
        this.cuentaNoBloqueada = cuentaNoBloqueada;

        // Los roles llevan el prefijo ROLE_, que es lo que espera hasRole().
        // Los permisos van sin prefijo, para poder escribir
        // hasAuthority('documento:descargar') tal cual esta en el catalogo.
        // Mezclarlos hace que ninguna comprobacion de permiso llegue a cumplirse.
        List<GrantedAuthority> lista = new ArrayList<>();
        roles.forEach(rol -> lista.add(new SimpleGrantedAuthority("ROLE_" + rol)));
        permisos.forEach(permiso -> lista.add(new SimpleGrantedAuthority(permiso)));
        this.autoridades = List.copyOf(lista);
    }

    public Long getId() {
        return id;
    }

    public boolean isCambioPasswordRequerido() {
        return cambioPasswordRequerido;
    }

    /** Se invoca tras autenticar para no conservar el hash en memoria. */
    public void borrarPassword() {
        this.passwordHash = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return autoridades;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return usuario;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Falso mientras dure el bloqueo por intentos fallidos de login. */
    @Override
    public boolean isAccountNonLocked() {
        return cuentaNoBloqueada;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Borrado logico: un usuario dado de baja conserva su historial. */
    @Override
    public boolean isEnabled() {
        return activo;
    }
}
