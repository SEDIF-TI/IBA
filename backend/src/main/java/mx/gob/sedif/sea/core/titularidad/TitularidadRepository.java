package mx.gob.sedif.sea.core.titularidad;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a datos del vinculo persona-animal. */
@Repository
public interface TitularidadRepository extends JpaRepository<Titularidad, Long> {

    /**
     * Titularidad vigente de un animal, que por definicion es una sola: hay un
     * indice unico parcial que impide que existan dos.
     */
    Optional<Titularidad> findByAnimalIdAndVigenteTrue(Long animalId);

    /** Historial completo del animal, del vinculo mas reciente al mas antiguo. */
    List<Titularidad> findByAnimalIdOrderByFechaInicioDesc(Long animalId);

    /** Titularidades vigentes de una persona. */
    List<Titularidad> findByPersonaIdAndVigenteTrue(Long personaId);

    long countByPersonaIdAndVigenteTrue(Long personaId);

    boolean existsByAnimalIdAndVigenteTrue(Long animalId);
}
