package mx.gob.sedif.sea.security;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Cuenta del personal con sus roles, para la ficha de administracion.
     */
    @EntityGraph(attributePaths = {"roles", "sede"})
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findConRolesPorId(@Param("id") Long id);

    /**
     * Listado del personal por nombre, usuario o correo.
     *
     * <p>El texto vacio se trata como "sin filtro": PostgreSQL no puede
     * comparar un parametro nulo sin que se le diga el tipo.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:texto = '' OR
                   LOWER(u.usuario) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                   LOWER(CONCAT(u.nombre, ' ', u.apellidoPaterno)) LIKE
                       LOWER(CONCAT('%', :texto, '%')))
              AND (:soloActivos = FALSE OR u.activo = TRUE)
            ORDER BY u.activo DESC, u.nombre ASC
            """)
    @EntityGraph(attributePaths = {"roles", "sede"})
    Page<Usuario> buscar(@Param("texto") String texto,
                         @Param("soloActivos") boolean soloActivos,
                         Pageable pageable);

    /** Cuantas cuentas activas tienen un rol, para no quedarse sin administrador. */
    @Query("""
            SELECT COUNT(u) FROM Usuario u JOIN u.roles r
            WHERE r.clave = :clave AND u.activo = TRUE
            """)
    long contarActivosPorRol(@Param("clave") String clave);
}
