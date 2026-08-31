package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de sexo del animal. */
@Service
public class SexoService extends CatalogoService<Sexo> {

    public SexoService(SexoRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, Sexo::new, "cat_sexo", auditoria);
    }
}
