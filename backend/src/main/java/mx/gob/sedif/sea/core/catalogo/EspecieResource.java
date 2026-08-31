package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de especies registrables. */
@RestController
@RequestMapping("/catalogos/especies")
public class EspecieResource extends CatalogoResourceBase<Especie> {

    private final EspecieService servicio;

    public EspecieResource(EspecieService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<Especie> getServicio() {
        return servicio;
    }
}
