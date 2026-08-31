package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de estados del tramite. */
@Service
public class EstatusSolicitudService extends CatalogoService<EstatusSolicitud> {

    public EstatusSolicitudService(EstatusSolicitudRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, EstatusSolicitud::new, "cat_estatus_solicitud", auditoria);
    }
}
