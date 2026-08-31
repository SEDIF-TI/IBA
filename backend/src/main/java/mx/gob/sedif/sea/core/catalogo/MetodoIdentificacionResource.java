package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de formas de identificar al animal. */
@RestController
@RequestMapping("/catalogos/metodos-identificacion")
public class MetodoIdentificacionResource extends CatalogoResourceBase<MetodoIdentificacion> {

    private final MetodoIdentificacionService servicio;

    public MetodoIdentificacionResource(MetodoIdentificacionService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<MetodoIdentificacion> getServicio() {
        return servicio;
    }
}
