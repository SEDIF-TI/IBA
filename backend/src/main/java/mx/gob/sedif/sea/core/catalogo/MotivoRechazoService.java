package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de motivos de rechazo u observacion. */
@Service
public class MotivoRechazoService extends CatalogoService<MotivoRechazo> {

    public MotivoRechazoService(MotivoRechazoRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, MotivoRechazo::new, "cat_motivo_rechazo", auditoria);
    }
}
