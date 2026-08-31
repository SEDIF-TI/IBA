package mx.gob.sedif.sea.security.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.exception.ApiResponse;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.security.JwtTokenProvider;
import mx.gob.sedif.sea.security.SecurityUtil;

import jakarta.validation.Valid;

/**
 * Acceso al panel administrativo.
 *
 * <p>Solo el personal se autentica: la ciudadania usa el portal publico sin
 * cuenta.
 *
 * <p>Es el unico controlador que conserva el sufijo Controller, por la
 * convencion heredada del proyecto base; el resto usa Resource.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Nombre de la cookie que transporta el token de refresco. */
    private static final String COOKIE_REFRESCO = "sea_refresh";

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final boolean cookieSegura;
    private final String cookieSameSite;

    public AuthController(AuthService authService,
                          JwtTokenProvider tokenProvider,
                          @Value("${app.jwt.cookie-secure:true}") boolean cookieSegura,
                          @Value("${app.jwt.cookie-same-site:Strict}") String cookieSameSite) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
        this.cookieSegura = cookieSegura;
        this.cookieSameSite = cookieSameSite;
    }

    /**
     * Inicia sesion.
     *
     * <p>El token de acceso va en el cuerpo; el de refresco, en una cookie
     * HttpOnly que JavaScript no puede leer.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> iniciarSesion(
            @Valid @RequestBody LoginRequest peticion) {

        AuthService.ResultadoAutenticacion resultado = authService.autenticar(peticion);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        construirCookieRefresco(resultado.tokenRefresco()).toString())
                .body(ApiResponse.ok(resultado.respuesta(), "Sesion iniciada correctamente"));
    }

    /**
     * Emite un token de acceso nuevo a partir de la cookie de refresco.
     *
     * <p>Los permisos se releen de la base, de modo que revocar uno surte
     * efecto en el siguiente refresco.
     */
    @PostMapping("/refrescar")
    public ResponseEntity<ApiResponse<JwtResponse>> refrescar(
            @CookieValue(name = COOKIE_REFRESCO, required = false) String tokenRefresco) {

        JwtResponse respuesta = authService.refrescar(tokenRefresco);
        return ResponseEntity.ok(ApiResponse.ok(respuesta, "Sesion renovada"));
    }

    /**
     * Cierra la sesion.
     *
     * <p>Vacia la cookie de refresco. El token de acceso sigue siendo valido
     * hasta que expira, por su propia naturaleza; por eso dura poco.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> cerrarSesion() {
        ResponseCookie cookieVacia = ResponseCookie.from(COOKIE_REFRESCO, "")
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieVacia.toString())
                .body(ApiResponse.ok("Sesion cerrada correctamente"));
    }

    /**
     * Cambia la contrasena de quien esta en sesion.
     *
     * <p>Es el paso que el usuario inicial debe completar antes de operar.
     */
    @PostMapping("/cambiar-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest peticion) {

        Long usuarioId = SecurityUtil.obtenerUsuarioId()
                .orElseThrow(() -> new IllegalStateException(MessageConstants.NO_AUTORIZADO));

        authService.cambiarPassword(usuarioId, peticion);
        return ResponseEntity.ok(ApiResponse.ok("Contrasena actualizada correctamente"));
    }

    /**
     * Arma la cookie del token de refresco.
     *
     * <ul>
     *   <li>HttpOnly: JavaScript no puede leerla, asi una inyeccion de script
     *       no puede robarla.</li>
     *   <li>Secure en produccion: solo viaja por HTTPS.</li>
     *   <li>SameSite: el navegador no la envia en peticiones de otros sitios.</li>
     *   <li>Ruta acotada a /api/auth: no se manda en cada llamada del API.</li>
     * </ul>
     */
    private ResponseCookie construirCookieRefresco(String token) {
        return ResponseCookie.from(COOKIE_REFRESCO, token)
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(tokenProvider.getExpiracionRefrescoSegundos()))
                .build();
    }
}
