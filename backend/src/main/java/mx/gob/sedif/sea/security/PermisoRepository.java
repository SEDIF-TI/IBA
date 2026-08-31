package mx.gob.sedif.sea.security;

import java.util.List;

import org.springframework.stereotype.Repository;

import mx.gob.sedif.sea.core.catalogo.CatalogoRepository;

/** Acceso a datos de los permisos. */
@Repository
public interface PermisoRepository extends CatalogoRepository<Permiso> {

    /** Permisos agrupados por modulo, como se muestran en el panel. */
    List<Permiso> findByActivoTrueOrderByModuloAscOrdenAsc();
}
