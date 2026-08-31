package mx.gob.sedif.sea.core.folio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

/** Acceso a los contadores de folio. */
@Repository
public interface FolioConsecutivoRepository extends JpaRepository<FolioConsecutivo, Long> {

    /**
     * Toma el contador con bloqueo de escritura.
     *
     * <p>El bloqueo es lo que impide que dos altas simultaneas obtengan el
     * mismo numero: la segunda espera a que la primera termine. Sin el, el dia
     * de una jornada saldrian folios repetidos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT f FROM FolioConsecutivo f
            WHERE f.tipo = :tipo
              AND f.anio = :anio
              AND ((:municipioId IS NULL AND f.municipio IS NULL)
                   OR f.municipio.id = :municipioId)
            """)
    Optional<FolioConsecutivo> tomarConBloqueo(@Param("tipo") String tipo,
                                               @Param("anio") Integer anio,
                                               @Param("municipioId") Integer municipioId);
}
