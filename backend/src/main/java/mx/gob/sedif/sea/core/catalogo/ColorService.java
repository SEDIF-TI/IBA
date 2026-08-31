package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de colores del pelaje. */
@Service
public class ColorService extends CatalogoService<Color> {

    public ColorService(ColorRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, Color::new, "cat_color", auditoria);
    }
}
