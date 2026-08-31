package mx.gob.sedif.sea.core.jornada;

import java.time.LocalDate;
import java.util.List;

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
    List<Jornada> findPublicadasVigentes(@Param("desde") LocalDate desde);

    List<Jornada> findByMunicipioIdAndActivoTrueOrderByFechaInicioDesc(Integer municipioId);

    /** Jornadas que se estan celebrando hoy, para la vista de operacion en sitio. */
    @Query("""
            SELECT j FROM Jornada j
            WHERE j.activo = TRUE
              AND :hoy BETWEEN j.fechaInicio AND j.fechaFin
            ORDER BY j.nombre
            """)
    List<Jornada> findEnCurso(@Param("hoy") LocalDate hoy);
}
