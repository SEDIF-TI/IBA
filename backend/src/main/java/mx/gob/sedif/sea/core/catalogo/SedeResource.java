package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.exception.ApiResponse;

/** Catalogo de puntos de entrega. */
@RestController
@RequestMapping("/catalogos/sedes")
public class SedeResource extends CatalogoResourceBase<Sede> {

    private final SedeService servicio;

    public SedeResource(SedeService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<Sede> getServicio() {
        return servicio;
    }

    @Override
    protected Object aRecord(Sede sede) {
        return SedeRecord.de(sede);
    }

    /** Sedes de un municipio. */
    @GetMapping("/municipio/{municipioId}")
    @PreAuthorize("hasAuthority('catalogo:ver')")
    public ResponseEntity<ApiResponse<List<SedeRecord>>> porMunicipio(
            @PathVariable Integer municipioId) {

        List<SedeRecord> sedes = servicio.listarPorMunicipio(municipioId).stream()
                .map(SedeRecord::de)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(sedes));
    }
}
