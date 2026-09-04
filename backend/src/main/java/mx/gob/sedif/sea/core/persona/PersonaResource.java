package mx.gob.sedif.sea.core.persona;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.core.animal.AnimalRecord;
import mx.gob.sedif.sea.core.solicitud.SolicitudRecord;
import mx.gob.sedif.sea.exception.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Tutores responsables.
 *
 * <p>La persona no se empadrona: el padron es de animales. Esta seccion sirve
 * para localizar a quien responde por ellos, sobre todo cuando alguien llega
 * preguntando por sus tramites.
 */
@RestController
@RequestMapping("/tutores")
public class PersonaResource {

    private final PersonaService personaService;

    public PersonaResource(PersonaService personaService) {
        this.personaService = personaService;
    }

    /**
     * Busca tutores por nombre, apellidos o CURP.
     *
     * <p>Ignora acentos. Con una CURP completa devuelve la coincidencia exacta.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('persona:ver')")
    public ResponseEntity<ApiResponse<Page<PersonaRecord>>> buscar(
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PersonaRecord> resultados = personaService.buscar(texto, pageable)
                .map(persona -> PersonaRecord.de(
                        persona, personaService.contarAnimales(persona.getId())));

        String mensaje = resultados.isEmpty()
                ? (texto == null || texto.isBlank()
                        ? "Todavia no hay tutores registrados"
                        : "No se encontro ningun tutor con \"%s\". Verifica el apellido "
                                .formatted(texto) + "o la CURP.")
                : "Se encontraron %d tutor(es)".formatted(resultados.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(resultados, mensaje));
    }

    /**
     * Ficha completa del tutor.
     *
     * <p>Muestra la CURP sin enmascarar, para poder cotejarla contra el
     * documento. Por eso el acceso queda en bitacora.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('persona:ver')")
    public ResponseEntity<ApiResponse<PersonaDetalleRecord>> obtener(@PathVariable Long id) {
        Persona persona = personaService.obtener(id);

        return ResponseEntity.ok(ApiResponse.ok(
                PersonaDetalleRecord.de(persona, personaService.contarAnimales(id))));
    }

    /** Animales que el tutor tiene a su cargo ahora mismo. */
    @GetMapping("/{id}/animales")
    @PreAuthorize("hasAuthority('persona:ver')")
    public ResponseEntity<ApiResponse<List<AnimalRecord>>> animales(@PathVariable Long id) {
        List<AnimalRecord> animales = personaService.animalesVigentes(id).stream()
                .map(AnimalRecord::de)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                animales,
                animales.isEmpty()
                        ? "Este tutor no tiene animales a su cargo actualmente"
                        : "Tiene %d animal(es) a su cargo".formatted(animales.size())));
    }

    /** Tramites del tutor, del mas reciente al mas antiguo. */
    @GetMapping("/{id}/tramites")
    @PreAuthorize("hasAuthority('persona:ver')")
    public ResponseEntity<ApiResponse<Page<SolicitudRecord>>> tramites(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                personaService.tramites(id, pageable).map(SolicitudRecord::de)));
    }

    /**
     * Completa la CURP de un tutor que se registro sin ella.
     *
     * <p>Es el caso de quien llego sin su documento. Al completarla, la
     * identidad queda validada: se supone que el personal cotejo el documento.
     */
    @PatchMapping("/{id}/curp")
    @PreAuthorize("hasAuthority('persona:editar')")
    public ResponseEntity<ApiResponse<PersonaDetalleRecord>> completarCurp(
            @PathVariable Long id,
            @Valid @RequestBody CompletarCurpRequest peticion) {

        Persona persona = personaService.completarCurp(id, peticion.curp());

        return ResponseEntity.ok(ApiResponse.ok(
                PersonaDetalleRecord.de(persona, personaService.contarAnimales(id)),
                "Se registro la CURP y la identidad quedo validada"));
    }

    /** Actualiza contacto y domicilio del tutor. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('persona:editar')")
    public ResponseEntity<ApiResponse<PersonaDetalleRecord>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPersonaRequest peticion) {

        Persona persona = personaService.actualizar(id, peticion);

        return ResponseEntity.ok(ApiResponse.ok(
                PersonaDetalleRecord.de(persona, personaService.contarAnimales(id)),
                "Se guardaron los datos de contacto de " + persona.getNombreCompleto()));
    }

    /**
     * CURP verificada contra el documento oficial.
     *
     * @param curp CURP de 18 caracteres
     */
    public record CompletarCurpRequest(
            @NotBlank(message = "Escribe la CURP tal como aparece en el documento")
            @Size(min = 18, max = 18,
                  message = "La CURP debe tener exactamente 18 caracteres")
            String curp) {
    }
}
