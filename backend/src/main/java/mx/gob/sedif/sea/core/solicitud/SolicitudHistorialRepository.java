package mx.gob.sedif.sea.core.solicitud;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso al historial de movimientos del tramite. */
@Repository
public interface SolicitudHistorialRepository extends JpaRepository<SolicitudHistorial, Long> {

    /** Expediente completo del tramite, del movimiento mas reciente hacia atras. */
    @EntityGraph(attributePaths = {"estatusAnterior", "estatusNuevo", "usuario", "motivoRechazo"})
    List<SolicitudHistorial> findBySolicitudIdOrderByFechaDesc(Long solicitudId);
}
