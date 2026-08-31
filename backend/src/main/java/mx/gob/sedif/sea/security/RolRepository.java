package mx.gob.sedif.sea.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import mx.gob.sedif.sea.core.catalogo.CatalogoRepository;

/** Acceso a datos de los roles. */
@Repository
public interface RolRepository extends CatalogoRepository<Rol> {

    /** Carga el rol junto con sus permisos, para la pantalla de administracion. */
    @EntityGraph(attributePaths = {"permisos"})
    Optional<Rol> findWithPermisosByClave(String clave);
}
