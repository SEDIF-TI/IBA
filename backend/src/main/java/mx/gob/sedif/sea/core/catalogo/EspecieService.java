package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de especies registrables. */
@Service
public class EspecieService extends CatalogoService<Especie> {

    public EspecieService(EspecieRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, Especie::new, "cat_especie", auditoria);
    }
}
