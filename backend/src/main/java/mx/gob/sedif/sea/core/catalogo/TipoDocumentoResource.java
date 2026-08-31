package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de tipos de documento. */
@RestController
@RequestMapping("/catalogos/tipos-documento")
public class TipoDocumentoResource extends CatalogoResourceBase<TipoDocumento> {

    private final TipoDocumentoService servicio;

    public TipoDocumentoResource(TipoDocumentoService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<TipoDocumento> getServicio() {
        return servicio;
    }
}
