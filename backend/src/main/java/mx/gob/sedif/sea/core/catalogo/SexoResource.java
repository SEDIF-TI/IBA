package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de sexo del animal. */
@RestController
@RequestMapping("/catalogos/sexos")
public class SexoResource extends CatalogoResourceBase<Sexo> {

    private final SexoService servicio;

    public SexoResource(SexoService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<Sexo> getServicio() {
        return servicio;
    }
}
