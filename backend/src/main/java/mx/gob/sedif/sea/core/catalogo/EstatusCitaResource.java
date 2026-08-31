package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de estados de la cita. */
@RestController
@RequestMapping("/catalogos/estatus-cita")
public class EstatusCitaResource extends CatalogoResourceBase<EstatusCita> {

    private final EstatusCitaService servicio;

    public EstatusCitaResource(EstatusCitaService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<EstatusCita> getServicio() {
        return servicio;
    }
}
