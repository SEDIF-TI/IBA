package mx.gob.sedif.sea.core.animal;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.exception.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Padron de animales.
 *
 * <p>Este es el padron propiamente dicho: lo que se empadrona es el animal.
 */
@RestController
@RequestMapping("/padron")
public class AnimalResource {

    private final AnimalService animalService;

    public AnimalResource(AnimalService animalService) {
        this.animalService = animalService;
    }

    /**
     * Busca animales por nombre, clave interna o numero de chip.
     *
     * <p>La busqueda por chip sirve cuando aparece un animal perdido: se lee su
     * identificador y se localiza a su tutor.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('animal:ver')")
    public ResponseEntity<ApiResponse<Page<AnimalRecord>>> buscar(
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AnimalRecord> resultados = animalService.buscar(texto, pageable)
                .map(AnimalRecord::de);

        String mensaje = resultados.isEmpty()
                ? (texto == null || texto.isBlank()
                        ? "Todavia no hay animales empadronados"
                        : "No se encontro ningun animal con \"%s\". Puedes buscar por "
                                .formatted(texto)
                                + "nombre, por su identificador o por numero de chip.")
                : "Se encontraron %d animal(es)".formatted(resultados.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(resultados, mensaje));
    }

    /** Ficha del animal con su tutor responsable actual. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('animal:ver')")
    public ResponseEntity<ApiResponse<AnimalRecord>> obtener(@PathVariable Long id) {
        Animal animal = animalService.obtener(id);
        Persona tutor = animalService.tutorVigente(id);

        return ResponseEntity.ok(ApiResponse.ok(AnimalRecord.de(
                animal,
                tutor == null ? null : tutor.getNombreCompleto(),
                tutor == null ? null : tutor.getId())));
    }

    /**
     * Historial de tutores del animal.
     *
     * <p>Se conserva completo: saber quien respondio por el animal y hasta
     * cuando es parte del padron.
     */
    @GetMapping("/{id}/titularidad")
    @PreAuthorize("hasAuthority('animal:ver')")
    public ResponseEntity<ApiResponse<List<TitularidadRecord>>> historial(
            @PathVariable Long id) {

        List<TitularidadRecord> historial = animalService.historialTitularidad(id).stream()
                .map(TitularidadRecord::de)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(historial));
    }

    /** Transfiere el animal a otro tutor. */
    @PostMapping("/{id}/transferir")
    @PreAuthorize("hasAuthority('titularidad:transferir')")
    public ResponseEntity<ApiResponse<TitularidadRecord>> transferir(
            @PathVariable Long id,
            @Valid @RequestBody TransferenciaRequest peticion) {

        var titularidad = animalService.transferir(
                id, peticion.nuevoTutorId(), peticion.motivo());

        return ResponseEntity.ok(ApiResponse.ok(
                TitularidadRecord.de(titularidad),
                "%s es ahora el tutor responsable. El tutor anterior queda en el "
                        .formatted(titularidad.getPersona().getNombreCompleto())
                        + "historial del animal."));
    }

    /** Registra la defuncion del animal. */
    @PatchMapping("/{id}/defuncion")
    @PreAuthorize("hasAuthority('animal:editar')")
    public ResponseEntity<ApiResponse<AnimalRecord>> registrarDefuncion(
            @PathVariable Long id,
            @Valid @RequestBody DefuncionRequest peticion) {

        Animal animal = animalService.registrarDefuncion(
                id, peticion.fecha(), peticion.observacion());

        return ResponseEntity.ok(ApiResponse.ok(
                AnimalRecord.de(animal),
                "Se registro la defuncion. El animal permanece en el padron con su "
                        + "historial completo."));
    }

    /**
     * Transferencia a otro tutor.
     *
     * @param nuevoTutorId quien recibe la responsabilidad
     * @param motivo       por que se transfiere
     */
    public record TransferenciaRequest(
            @NotNull(message = "Elige al tutor que recibe la responsabilidad")
            Long nuevoTutorId,

            @NotBlank(message = "Escribe el motivo de la transferencia: adopcion, "
                    + "cambio de responsable o fallecimiento del tutor")
            @Size(max = 300)
            String motivo) {
    }

    /**
     * Registro de defuncion.
     *
     * @param fecha       cuando ocurrio; si se omite, se toma hoy
     * @param observacion detalle opcional
     */
    public record DefuncionRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha,

            @Size(max = 300)
            String observacion) {
    }
}
