package mx.gob.sedif.sea.core.catalogo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogo de los 217 municipios de Puebla.
 *
 * <p>Devuelve ademas la clave INEGI, que se usa para armar el folio oficial del
 * certificado.
 */
@RestController
@RequestMapping("/catalogos/municipios")
public class MunicipioResource extends CatalogoResourceBase<Municipio> {

    private final MunicipioService servicio;

    public MunicipioResource(MunicipioService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<Municipio> getServicio() {
        return servicio;
    }

    @Override
    protected Object aRecord(Municipio municipio) {
        return MunicipioRecord.de(municipio);
    }
}
