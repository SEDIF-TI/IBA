package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de los puntos de entrega. */
@Service
public class SedeService extends CatalogoService<Sede> {

    private final SedeRepository sedeRepository;

    public SedeService(SedeRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, Sede::new, "cat_sede", auditoria);
        this.sedeRepository = repositorio;
    }

    /** Sedes vigentes de un municipio, para asignar la cita mas cercana. */
    @Transactional(readOnly = true)
    public List<Sede> listarPorMunicipio(Integer municipioId) {
        return sedeRepository.findByMunicipioIdAndActivoTrueOrderByOrdenAscNombreAsc(municipioId);
    }
}
