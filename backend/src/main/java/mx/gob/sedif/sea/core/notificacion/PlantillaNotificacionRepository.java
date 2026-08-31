package mx.gob.sedif.sea.core.notificacion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a las plantillas de aviso, editables desde el panel. */
@Repository
public interface PlantillaNotificacionRepository extends JpaRepository<PlantillaNotificacion, Integer> {

    Optional<PlantillaNotificacion> findByClaveAndActivoTrue(String clave);
}
