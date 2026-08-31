package mx.gob.sedif.sea.core.catalogo;

import java.util.Optional;

import org.springframework.stereotype.Repository;

/** Acceso a datos de los estados de la solicitud. */
@Repository
public interface EstatusSolicitudRepository extends CatalogoRepository<EstatusSolicitud> {

    /**
     * Resuelve un estado por su clave.
     *
     * <p>La capa de servicio lo usa en cada transicion; el resultado se cachea
     * porque el catalogo cambia muy rara vez.
     */
    Optional<EstatusSolicitud> findByClave(String clave);
}
