package mx.gob.sedif.sea.core.auditoria;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Consulta de la bitacora de acciones sensibles. */
@Repository
public interface BitacoraAuditoriaRepository extends JpaRepository<BitacoraAuditoria, Long> {

    /**
     * Consulta filtrada de la bitacora. Un parametro nulo no filtra.
     */
    @Query("""
            SELECT b FROM BitacoraAuditoria b
            WHERE (:usuarioId IS NULL OR b.usuario.id = :usuarioId)
              AND (:entidad IS NULL OR b.entidad = :entidad)
              AND (:accion IS NULL OR b.accion = :accion)
              AND (:desde IS NULL OR b.fecha >= :desde)
              AND (:hasta IS NULL OR b.fecha <= :hasta)
            ORDER BY b.fecha DESC
            """)
    Page<BitacoraAuditoria> buscar(@Param("usuarioId") Long usuarioId,
                                   @Param("entidad") String entidad,
                                   @Param("accion") String accion,
                                   @Param("desde") OffsetDateTime desde,
                                   @Param("hasta") OffsetDateTime hasta,
                                   Pageable pageable);

    /** Historial de un registro concreto. */
    Page<BitacoraAuditoria> findByEntidadAndRegistroIdOrderByFechaDesc(
            String entidad, Long registroId, Pageable pageable);
}
