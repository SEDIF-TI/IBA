package mx.gob.sedif.sea.core.catalogo;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion del catalogo de municipios. */
@Service
public class MunicipioService extends CatalogoService<Municipio> {

    private final MunicipioRepository municipioRepository;

    public MunicipioService(MunicipioRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, Municipio::new, "cat_municipio", auditoria);
        this.municipioRepository = repositorio;
    }

    /** Localiza por clave INEGI, que es como llegan los datos de otros sistemas. */
    @Transactional(readOnly = true)
    public Optional<Municipio> buscarPorClaveInegi(String claveInegi) {
        return municipioRepository.findByClaveInegi(claveInegi);
    }
}
