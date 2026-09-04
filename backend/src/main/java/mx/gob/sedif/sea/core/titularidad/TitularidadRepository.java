package mx.gob.sedif.sea.core.titularidad;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a datos del vinculo persona-animal. */
@Repository
public interface TitularidadRepository extends JpaRepository<Titularidad, Long> {

    /**
     * Titularidad vigente de un animal, que por definicion es una sola: hay un
     * indice unico parcial que impide que existan dos.
     */
    @EntityGraph(attributePaths = {"persona", "persona.municipio", "animal"})
    Optional<Titularidad> findByAnimalIdAndVigenteTrue(Long animalId);

    /** Historial completo del animal, del vinculo mas reciente al mas antiguo. */
    @EntityGraph(attributePaths = {"persona"})
    List<Titularidad> findByAnimalIdOrderByFechaInicioDesc(Long animalId);

    /** Titularidades vigentes de una persona. */
    @EntityGraph(attributePaths = {"animal", "animal.especie"})
    List<Titularidad> findByPersonaIdAndVigenteTrue(Long personaId);

    long countByPersonaIdAndVigenteTrue(Long personaId);

    boolean existsByAnimalIdAndVigenteTrue(Long animalId);
}
