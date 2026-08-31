package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de colores del pelaje. */
@RestController
@RequestMapping("/catalogos/colores")
public class ColorResource extends CatalogoResourceBase<Color> {

    private final ColorService servicio;

    public ColorResource(ColorService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<Color> getServicio() {
        return servicio;
    }
}
