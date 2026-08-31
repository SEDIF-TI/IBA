package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de estados de la cita. */
@Service
public class EstatusCitaService extends CatalogoService<EstatusCita> {

    public EstatusCitaService(EstatusCitaRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, EstatusCita::new, "cat_estatus_cita", auditoria);
    }
}
