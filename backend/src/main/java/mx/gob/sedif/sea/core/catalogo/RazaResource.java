package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.exception.ApiResponse;
import mx.gob.sedif.sea.exception.MessageConstants;

import jakarta.validation.Valid;

/** Catalogo de razas, siempre subordinadas a una especie. */
@RestController
@RequestMapping("/catalogos/razas")
public class RazaResource extends CatalogoResourceBase<Raza> {

    private final RazaService servicio;

    public RazaResource(RazaService servicio) {
        this.servicio = servicio;
    }

    @Override
    protected CatalogoService<Raza> getServicio() {
        return servicio;
    }

    @Override
    protected Object aRecord(Raza raza) {
        return RazaRecord.de(raza);
    }

    /**
     * Razas de una especie.
     *
     * <p>Es la consulta del desplegable dependiente: un gato no puede ser
     * Chihuahua.
     */
    @GetMapping("/especie/{especieId}")
    @PreAuthorize("hasAuthority('catalogo:ver')")
    public ResponseEntity<ApiResponse<List<RazaRecord>>> porEspecie(
            @PathVariable Integer especieId) {

        List<RazaRecord> razas = servicio.listarPorEspecie(especieId).stream()
                .map(RazaRecord::de)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(razas));
    }

    /** Alta de una raza indicando a que especie pertenece. */
    @PostMapping("/especie/{especieId}")
    @PreAuthorize("hasAuthority('catalogo:administrar')")
    public ResponseEntity<ApiResponse<RazaRecord>> crearEnEspecie(
            @PathVariable Integer especieId,
            @Valid @RequestBody CatalogoRequest peticion) {

        Raza creada = servicio.crearConEspecie(especieId, peticion);
        return ResponseEntity.ok(
                ApiResponse.ok(RazaRecord.de(creada), MessageConstants.REGISTRO_CREADO));
    }
}
