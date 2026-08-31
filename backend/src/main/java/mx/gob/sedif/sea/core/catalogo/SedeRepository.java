package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.stereotype.Repository;

/** Acceso a datos de las sedes de entrega. */
@Repository
public interface SedeRepository extends CatalogoRepository<Sede> {

    /** Sedes vigentes de un municipio, para asignar la cita mas cercana. */
    List<Sede> findByMunicipioIdAndActivoTrueOrderByOrdenAscNombreAsc(Integer municipioId);
}
