package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;

/** Administracion del catalogo de razas. */
@Service
public class RazaService extends CatalogoService<Raza> {

    private final RazaRepository razaRepository;
    private final EspecieRepository especieRepository;

    public RazaService(RazaRepository repositorio,
                       EspecieRepository especieRepository,
                       AuditoriaService auditoria) {
        super(repositorio, Raza::new, "cat_raza", auditoria);
        this.razaRepository = repositorio;
        this.especieRepository = especieRepository;
    }

    /**
     * Razas vigentes de una especie.
     *
     * <p>Alimenta el desplegable dependiente del formulario: al elegir felino
     * solo deben ofrecerse razas felinas.
     */
    @Transactional(readOnly = true)
    public List<Raza> listarPorEspecie(Integer especieId) {
        return razaRepository.findByEspecieIdAndActivoTrueOrderByOrdenAscNombreAsc(especieId);
    }

    /** Alta de una raza, que siempre pertenece a una especie. */
    @Transactional
    public Raza crearConEspecie(Integer especieId, CatalogoRequest peticion) {
        Especie especie = especieRepository.findById(especieId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especie", especieId));

        Raza raza = crear(peticion);
        raza.setEspecie(especie);
        return razaRepository.save(raza);
    }
}
