package mx.gob.sedif.sea.core.notificacion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mx.gob.sedif.sea.util.enums.CanalNotificacion;
import mx.gob.sedif.sea.util.enums.EstatusEnvio;

/** Acceso a datos de los avisos. */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findBySolicitudIdOrderByFechaCreacionDesc(Long solicitudId);

    /**
     * Bandeja de contacto manual: a quien hay que llamar hoy.
     *
     * <p>Son las solicitudes de personas que solo dejaron telefono. Sin esta
     * vista, esos avisos no ocurririan y nadie se enteraria de que hay gente
     * esperando respuesta.
     */
    @EntityGraph(attributePaths = {"solicitud", "solicitud.persona"})
    Page<Notificacion> findByCanalAndEstatusEnvioOrderByFechaCreacionAsc(
            CanalNotificacion canal, EstatusEnvio estatus, Pageable pageable);

    /** Envios automaticos pendientes o fallidos que aun admiten reintento. */
    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.canal = mx.gob.sedif.sea.util.enums.CanalNotificacion.AUTOMATICO
              AND n.estatusEnvio IN (mx.gob.sedif.sea.util.enums.EstatusEnvio.PENDIENTE,
                                     mx.gob.sedif.sea.util.enums.EstatusEnvio.FALLIDA)
              AND n.intentos < :maxIntentos
            ORDER BY n.fechaCreacion
            """)
    List<Notificacion> findPendientesDeEnvio(@Param("maxIntentos") int maxIntentos);

    long countByCanalAndEstatusEnvio(CanalNotificacion canal, EstatusEnvio estatus);
}
