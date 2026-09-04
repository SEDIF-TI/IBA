package mx.gob.sedif.sea.core.persona;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a datos del padron de tutores. */
@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    /**
     * Localiza a la persona por su CURP.
     *
     * <p>Es la operacion central del flujo publico: si la CURP ya existe se
     * reutiliza la persona y no se le vuelve a pedir informacion que ya dio.
     *
     * <p>Toda invocacion desde el panel debe quedar registrada en bitacora.
     */
    @EntityGraph(attributePaths = {"municipio", "medioContactoPreferido"})
    Optional<Persona> findByCurp(String curp);

    /**
     * Tutor con su municipio ya cargado.
     *
     * <p>El DTO lo lee fuera de la transaccion.
     */
    @Query("SELECT p FROM Persona p WHERE p.id = :id")
    @EntityGraph(attributePaths = {"municipio", "medioContactoPreferido"})
    Optional<Persona> findConDetallePorId(@Param("id") Long id);

    boolean existsByCurp(String curp);

    /**
     * Buscador del padron por nombre o apellidos, ignorando acentos y
     * mayusculas: escribir "munoz" encuentra "Muñoz".
     *
     * <p>Es la consulta que se usa el dia de una jornada, con la gente
     * formada enfrente, asi que se apoya en un indice de trigramas.
     */
    @Query("""
            SELECT p FROM Persona p
            WHERE p.activo = TRUE
              AND sea_unaccent(UPPER(CONCAT(p.apellidoPaterno, ' ',
                      COALESCE(p.apellidoMaterno, ''), ' ', p.nombre)))
                  LIKE sea_unaccent(:patron)
            ORDER BY p.apellidoPaterno, p.apellidoMaterno, p.nombre
            """)
    @EntityGraph(attributePaths = {"municipio"})
    Page<Persona> buscarPorPatron(@Param("patron") String patron, Pageable pageable);

    /**
     * Buscador del padron por nombre o apellidos.
     *
     * @param texto texto a buscar, sin comodines
     */
    default Page<Persona> buscarPorNombre(String texto, Pageable pageable) {
        String patron = (texto == null || texto.isBlank())
                ? "%"
                : "%" + texto.trim().toUpperCase() + "%";
        return buscarPorPatron(patron, pageable);
    }

    /**
     * Cuenta los animales que la persona tiene a su cargo ahora mismo.
     *
     * <p>Son las titularidades vigentes, no todas las historicas: un animal
     * transferido ya no cuenta para el tutor anterior.
     */
    @Query("""
            SELECT COUNT(t) FROM Titularidad t
            WHERE t.persona.id = :personaId AND t.vigente = TRUE
            """)
    long contarAnimalesVigentes(@Param("personaId") Long personaId);
}
