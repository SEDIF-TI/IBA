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
     * Consulta de la bitacora dentro de un rango de fechas.
     *
     * <p>Las fechas no admiten nulos: PostgreSQL no puede deducir el tipo de un
     * parametro nulo suelto y rechaza la consulta. Quien llama sustituye el
     * nulo por el extremo del rango.
     */
    @Query("""
            SELECT b FROM BitacoraAuditoria b
            WHERE (:usuarioId IS NULL OR b.usuario.id = :usuarioId)
              AND (:entidad IS NULL OR b.entidad = :entidad)
              AND (:accion IS NULL OR b.accion = :accion)
              AND b.fecha BETWEEN :desde AND :hasta
            ORDER BY b.fecha DESC
            """)
    Page<BitacoraAuditoria> buscarEnRango(@Param("usuarioId") Long usuarioId,
                                          @Param("entidad") String entidad,
                                          @Param("accion") String accion,
                                          @Param("desde") OffsetDateTime desde,
                                          @Param("hasta") OffsetDateTime hasta,
                                          Pageable pageable);

    /** Consulta filtrada de la bitacora. Un filtro sin valor no restringe. */
    default Page<BitacoraAuditoria> buscar(Long usuarioId, String entidad, String accion,
                                           OffsetDateTime desde, OffsetDateTime hasta,
                                           Pageable pageable) {
        OffsetDateTime inicio = desde != null
                ? desde
                : OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
        OffsetDateTime fin = hasta != null ? hasta : OffsetDateTime.now().plusYears(100);

        return buscarEnRango(usuarioId, entidad, accion, inicio, fin, pageable);
    }

    /** Historial de un registro concreto. */
    Page<BitacoraAuditoria> findByEntidadAndRegistroIdOrderByFechaDesc(
            String entidad, Long registroId, Pageable pageable);
}
