package mx.gob.sedif.sea.core.jornada;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a datos de las jornadas municipales. */
@Repository
public interface JornadaRepository extends JpaRepository<Jornada, Long> {

    /**
     * Calendario del portal publico: solo jornadas publicadas que aun no
     * terminan.
     */
    @Query("""
            SELECT j FROM Jornada j
            WHERE j.publicada = TRUE
              AND j.activo = TRUE
              AND j.fechaFin >= :desde
            ORDER BY j.fechaInicio
            """)
    @EntityGraph(attributePaths = {"municipio"})
    List<Jornada> findPublicadasVigentes(@Param("desde") LocalDate desde);

    @EntityGraph(attributePaths = {"municipio", "usuarioResponsable"})
    List<Jornada> findByMunicipioIdAndActivoTrueOrderByFechaInicioDesc(Integer municipioId);

    /** Jornadas que se estan celebrando hoy, para la vista de operacion en sitio. */
    @Query("""
            SELECT j FROM Jornada j
            WHERE j.activo = TRUE
              AND :hoy BETWEEN j.fechaInicio AND j.fechaFin
            ORDER BY j.nombre
            """)
    @EntityGraph(attributePaths = {"municipio", "usuarioResponsable"})
    List<Jornada> findEnCurso(@Param("hoy") LocalDate hoy);

    /**
     * Jornada con su municipio y responsable ya cargados.
     *
     * <p>El DTO los lee fuera de la transaccion.
     */
    @Query("SELECT j FROM Jornada j WHERE j.id = :id")
    @EntityGraph(attributePaths = {"municipio", "usuarioResponsable"})
    java.util.Optional<Jornada> findConDetallePorId(@Param("id") Long id);

    /** Todas las jornadas, con lo necesario para listarlas. */
    @Override
    @EntityGraph(attributePaths = {"municipio", "usuarioResponsable"})
    List<Jornada> findAll();
}
