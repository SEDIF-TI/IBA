package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de motivos de rechazo u observacion. */
@RestController
@RequestMapping("/catalogos/motivos-rechazo")
public class MotivoRechazoResource extends CatalogoResourceBase<MotivoRechazo> {

    private final MotivoRechazoService servicio;

    public MotivoRechazoResource(MotivoRechazoService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<MotivoRechazo> getServicio() {
        return servicio;
    }
}
