package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de estados del tramite. */
@RestController
@RequestMapping("/catalogos/estatus-solicitud")
public class EstatusSolicitudResource extends CatalogoResourceBase<EstatusSolicitud> {

    private final EstatusSolicitudService servicio;

    public EstatusSolicitudResource(EstatusSolicitudService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<EstatusSolicitud> getServicio() {
        return servicio;
    }
}
