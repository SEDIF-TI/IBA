package mx.gob.sedif.sea.core.animal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a datos del padron de animales. */
@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByClaveAnimal(String claveAnimal);

    /**
     * Busca por numero de microchip o placa. Sirve para detectar que un animal
     * ya esta empadronado antes de crear un duplicado.
     */
    List<Animal> findByIdentificadorAndActivoTrue(String identificador);

    /** Buscador por nombre del animal, tolerante a acentos y mayusculas. */
    @Query("""
            SELECT a FROM Animal a
            WHERE a.activo = TRUE
              AND FUNCTION('sea_unaccent', UPPER(a.nombre))
                  LIKE FUNCTION('sea_unaccent', UPPER(CONCAT('%', :texto, '%')))
            """)
    Page<Animal> buscarPorNombre(@Param("texto") String texto, Pageable pageable);

    /**
     * Animales que una persona tiene a su cargo ahora mismo.
     *
     * <p>Alimenta la ficha del tutor en el panel: el personal necesita ver de
     * un vistazo cuantos y cuales animales responden a esa persona.
     */
    @Query("""
            SELECT t.animal FROM Titularidad t
            WHERE t.persona.id = :personaId
              AND t.vigente = TRUE
              AND t.animal.activo = TRUE
            ORDER BY t.animal.nombre
            """)
    List<Animal> findVigentesPorPersona(@Param("personaId") Long personaId);
}
