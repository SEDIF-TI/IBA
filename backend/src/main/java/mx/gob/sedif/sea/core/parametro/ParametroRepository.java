package mx.gob.sedif.sea.core.parametro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a los ajustes configurables del sistema. */
@Repository
public interface ParametroRepository extends JpaRepository<Parametro, Integer> {

    Optional<Parametro> findByClaveAndActivoTrue(String clave);

    List<Parametro> findByGrupoAndActivoTrueOrderByClave(String grupo);

    List<Parametro> findByActivoTrueOrderByGrupoAscClaveAsc();
}
