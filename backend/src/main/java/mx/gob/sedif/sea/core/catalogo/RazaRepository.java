package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catalogo de razas. */
@Repository
public interface RazaRepository extends CatalogoRepository<Raza> {

    /**
     * Razas vigentes de una especie. Alimenta el selector dependiente del
     * formulario: al elegir "felino" solo deben ofrecerse razas felinas.
     */
    @EntityGraph(attributePaths = {"especie"})
    List<Raza> findByEspecieIdAndActivoTrueOrderByOrdenAscNombreAsc(Integer especieId);

    /**
     * Razas vigentes con su especie ya cargada.
     *
     * <p>El grafo es necesario: la conversion a DTO ocurre en el controlador,
     * fuera de la transaccion, y una especie sin cargar no se podria resolver
     * en ese punto.
     */
    @Override
    @EntityGraph(attributePaths = {"especie"})
    List<Raza> findByActivoTrueOrderByOrdenAscNombreAsc();

    /**
     * Listado paginado con la especie ya cargada.
     *
     * <p>Se sobrescribe para agregar el grafo: la conversion a DTO lee el
     * nombre de la especie fuera de la transaccion.
     */
    @Override
    @EntityGraph(attributePaths = {"especie"})
    org.springframework.data.domain.Page<Raza> buscarPorPatron(
            String patron, boolean soloActivos,
            org.springframework.data.domain.Pageable pageable);
}
