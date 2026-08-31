package mx.gob.sedif.sea.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a datos del personal administrativo. */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Carga el usuario con sus roles y permisos en una sola consulta.
     *
     * <p>El grafo evita la cascada de consultas que haria la carga diferida al
     * construir las autoridades en cada autenticacion.
     */
    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByUsuarioIgnoreCase(String usuario);

    boolean existsByCorreoIgnoreCase(String correo);

    long countByActivoTrue();
}
