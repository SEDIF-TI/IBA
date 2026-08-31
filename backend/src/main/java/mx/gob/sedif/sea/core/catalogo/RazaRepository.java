package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.stereotype.Repository;

/** Acceso a datos del catalogo de razas. */
@Repository
public interface RazaRepository extends CatalogoRepository<Raza> {

    /**
     * Razas vigentes de una especie. Alimenta el selector dependiente del
     * formulario: al elegir "felino" solo deben ofrecerse razas felinas.
     */
    List<Raza> findByEspecieIdAndActivoTrueOrderByOrdenAscNombreAsc(Integer especieId);
}
