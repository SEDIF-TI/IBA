package mx.gob.sedif.sea.security.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.exception.ApiResponse;
import mx.gob.sedif.sea.security.Rol;
import mx.gob.sedif.sea.security.Usuario;

import jakarta.validation.Valid;

/**
 * Administracion de las cuentas del personal.
 *
 * <p>Ver la lista y crear cuentas son permisos distintos a proposito: el IBA
 * necesita saber quien tiene acceso, pero dar de alta a alguien es de
 * informatica, que responde por los accesos.
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioResource {

    private final UsuarioAdminService usuarioAdminService;

    public UsuarioResource(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    /** Personal dado de alta. */
    @GetMapping
    @PreAuthorize("hasAuthority('usuario:ver')")
    public ResponseEntity<ApiResponse<Page<UsuarioRecord>>> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false, defaultValue = "true") Boolean soloActivos,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UsuarioRecord> pagina = usuarioAdminService
                .buscar(texto, soloActivos, pageable)
                .map(UsuarioRecord::de);

        String mensaje = pagina.getTotalElements() == 0
                ? "No hay cuentas que coincidan con la busqueda"
                : "Se encontraron %d cuenta(s)".formatted(pagina.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(pagina, mensaje));
    }

    /** Roles que se pueden asignar. */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('usuario:ver')")
    public ResponseEntity<ApiResponse<List<UsuarioRecord.RolResumen>>> roles() {
        List<UsuarioRecord.RolResumen> roles = usuarioAdminService.rolesDisponibles().stream()
                .map(UsuarioRecord.RolResumen::de)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    /** Una cuenta concreta. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('usuario:ver')")
    public ResponseEntity<ApiResponse<UsuarioRecord>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(UsuarioRecord.de(usuarioAdminService.obtener(id))));
    }

    /**
     * Alta de una cuenta.
     *
     * <p>La respuesta trae la contrasena temporal. Es la unica vez que se
     * muestra: no se guarda en claro, asi que hay que entregarla en ese
     * momento.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('usuario:administrar')")
    public ResponseEntity<ApiResponse<AltaRespuesta>> crear(
            @Valid @RequestBody GuardarUsuarioRequest peticion) {

        UsuarioAdminService.ResultadoAlta alta = usuarioAdminService.crear(peticion);

        AltaRespuesta respuesta = new AltaRespuesta(
                UsuarioRecord.de(alta.usuario()),
                alta.passwordTemporal());

        return ResponseEntity.ok(ApiResponse.ok(respuesta,
                "Cuenta '%s' creada. Entrega la contrasena temporal a la persona: "
                        .formatted(alta.usuario().getUsuario())
                        + "no se vuelve a mostrar y debera cambiarla al entrar."));
    }

    /** Cambio de datos y de roles. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('usuario:administrar')")
    public ResponseEntity<ApiResponse<UsuarioRecord>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GuardarUsuarioRequest peticion) {

        Usuario usuario = usuarioAdminService.actualizar(id, peticion);
        return ResponseEntity.ok(ApiResponse.ok(UsuarioRecord.de(usuario),
                "Se guardaron los cambios de la cuenta '%s'".formatted(usuario.getUsuario())));
    }

    /** Genera una contrasena temporal nueva. */
    @PatchMapping("/{id}/reiniciar-password")
    @PreAuthorize("hasAuthority('usuario:administrar')")
    public ResponseEntity<ApiResponse<AltaRespuesta>> reiniciarPassword(@PathVariable Long id) {
        UsuarioAdminService.ResultadoAlta alta = usuarioAdminService.reiniciarPassword(id);

        AltaRespuesta respuesta = new AltaRespuesta(
                UsuarioRecord.de(alta.usuario()),
                alta.passwordTemporal());

        return ResponseEntity.ok(ApiResponse.ok(respuesta,
                "Contrasena reiniciada. Entregala a la persona: no se vuelve a mostrar "
                        + "y debera cambiarla al entrar."));
    }

    /** Baja logica o reactivacion. */
    @PatchMapping("/{id}/vigencia")
    @PreAuthorize("hasAuthority('usuario:administrar')")
    public ResponseEntity<ApiResponse<UsuarioRecord>> cambiarVigencia(
            @PathVariable Long id,
            @RequestParam boolean activa) {

        Usuario usuario = usuarioAdminService.cambiarVigencia(id, activa);
        return ResponseEntity.ok(ApiResponse.ok(UsuarioRecord.de(usuario),
                activa
                        ? "La cuenta '%s' quedo activa de nuevo".formatted(usuario.getUsuario())
                        : "La cuenta '%s' quedo dada de baja y ya no puede entrar"
                                .formatted(usuario.getUsuario())));
    }

    /** Libera una cuenta bloqueada por intentos fallidos. */
    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasAuthority('usuario:administrar')")
    public ResponseEntity<ApiResponse<UsuarioRecord>> desbloquear(@PathVariable Long id) {
        Usuario usuario = usuarioAdminService.desbloquear(id);
        return ResponseEntity.ok(ApiResponse.ok(UsuarioRecord.de(usuario),
                "La cuenta '%s' quedo desbloqueada y ya puede entrar"
                        .formatted(usuario.getUsuario())));
    }

    /** Cuenta recien creada junto con su contrasena temporal. */
    public record AltaRespuesta(UsuarioRecord usuario, String passwordTemporal) {
    }
}
