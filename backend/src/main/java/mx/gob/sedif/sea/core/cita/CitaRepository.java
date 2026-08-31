package mx.gob.sedif.sea.core.cita;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a datos de las citas de entrega. */
@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByNumeroCita(String numeroCita);

    List<Cita> findBySolicitudIdOrderByFechaCitaDesc(Long solicitudId);

    /** Agenda del dia en una sede. */
    @EntityGraph(attributePaths = {"solicitud", "sede", "estatus"})
    List<Cita> findByFechaCitaAndSedeIdAndActivoTrueOrderByHorario(LocalDate fecha, Integer sedeId);

    long countByFechaCitaAndSedeId(LocalDate fecha, Integer sedeId);
}
