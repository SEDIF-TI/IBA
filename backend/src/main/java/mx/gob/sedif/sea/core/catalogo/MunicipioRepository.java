package mx.gob.sedif.sea.core.catalogo;

import java.util.Optional;

import org.springframework.stereotype.Repository;

/** Acceso a datos del catalogo de municipios. */
@Repository
public interface MunicipioRepository extends CatalogoRepository<Municipio> {

    /** Localiza por clave INEGI, que es como llegan los datos de otros sistemas. */
    Optional<Municipio> findByClaveInegi(String claveInegi);
}
