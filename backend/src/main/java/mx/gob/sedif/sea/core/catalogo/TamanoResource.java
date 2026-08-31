package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de tallas. */
@RestController
@RequestMapping("/catalogos/tamanos")
public class TamanoResource extends CatalogoResourceBase<Tamano> {

    private final TamanoService servicio;

    public TamanoResource(TamanoService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<Tamano> getServicio() {
        return servicio;
    }
}
