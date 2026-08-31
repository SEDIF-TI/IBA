package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de vias de contacto. */
@RestController
@RequestMapping("/catalogos/medios-contacto")
public class MedioContactoResource extends CatalogoResourceBase<MedioContacto> {

    private final MedioContactoService servicio;

    public MedioContactoResource(MedioContactoService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<MedioContacto> getServicio() {
        return servicio;
    }
}
