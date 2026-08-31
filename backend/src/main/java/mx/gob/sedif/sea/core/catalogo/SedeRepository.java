package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

/** Acceso a datos de las sedes de entrega. */
@Repository
public interface SedeRepository extends CatalogoRepository<Sede> {

    /** Sedes vigentes de un municipio, para asignar la cita mas cercana. */
    @EntityGraph(attributePaths = {"municipio"})
    List<Sede> findByMunicipioIdAndActivoTrueOrderByOrdenAscNombreAsc(Integer municipioId);

    /**
     * Sedes vigentes con su municipio ya cargado, para poder convertirlas a DTO
     * fuera de la transaccion.
     */
    @Override
    @EntityGraph(attributePaths = {"municipio"})
    List<Sede> findByActivoTrueOrderByOrdenAscNombreAsc();

    /** Listado paginado con el municipio ya cargado. */
    @Override
    @EntityGraph(attributePaths = {"municipio"})
    org.springframework.data.domain.Page<Sede> buscarPorPatron(
            String patron, boolean soloActivos,
            org.springframework.data.domain.Pageable pageable);
}
