package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de tallas. */
@Service
public class TamanoService extends CatalogoService<Tamano> {

    public TamanoService(TamanoRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, Tamano::new, "cat_tamano", auditoria);
    }
}
