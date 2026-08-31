package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de vias de contacto. */
@Service
public class MedioContactoService extends CatalogoService<MedioContacto> {

    public MedioContactoService(MedioContactoRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, MedioContacto::new, "cat_medio_contacto", auditoria);
    }
}
