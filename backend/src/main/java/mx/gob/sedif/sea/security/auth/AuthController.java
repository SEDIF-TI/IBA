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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Acceso al panel administrativo.
 * Área: Núcleo compartido (seguridad). Lógica: inicio, renovación y cierre de sesión.
 *
 * <p>Solo el personal se autentica: la ciudadanía usa el portal público sin cuenta. Es el único
 * controlador con sufijo Controller, por la convención del estándar del equipo.
 *
 * <p>El token de acceso viaja en el cuerpo y el frontend lo guarda solo en memoria; el de refresco
 * viaja en una cookie {@code HttpOnly} (JavaScript no puede leerla), {@code Secure} y
 * {@code SameSite=Strict} en producción, acotada a {@code /api/auth}, y se reemplaza en cada uso.
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
     * {@code POST /api/auth/login} — público, con límite de peticiones por IP.
     *
     * @param peticion usuario y contraseña
     * @param http     petición, para la IP y el navegador de la sesión
     * @return 200 con el token de acceso y la cookie de refresco; 401 si las credenciales no son
     *         correctas; 423 si la cuenta está bloqueada
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> iniciarSesion(
            @Valid @RequestBody LoginRequest peticion, HttpServletRequest http) {

        AuthService.ResultadoAutenticacion resultado = authService.autenticar(
                peticion, SecurityUtil.obtenerIpCliente(http), SecurityUtil.obtenerUserAgent(http));

        return conCookie(resultado, "Sesión iniciada correctamente");
    }

    /**
     * {@code POST /api/auth/refrescar} — público (solo necesita la cookie), con límite por IP.
     * Rota la cookie: el token de refresco recibido deja de servir.
     *
     * @param tokenRefresco valor de la cookie {@code sea_refresh}; puede faltar
     * @return 200 con un token de acceso nuevo y la cookie nueva; 401 si la sesión ya no es válida
     */
    @PostMapping("/refrescar")
    public ResponseEntity<ApiResponse<JwtResponse>> refrescar(
            @CookieValue(name = COOKIE_REFRESCO, required = false) String tokenRefresco) {

        return conCookie(authService.refrescar(tokenRefresco), "Sesión renovada");
    }

    /**
     * {@code POST /api/auth/logout} — público (solo necesita la cookie). Cierra la sesión en el
     * servidor, con lo que el token de refresco y el de acceso de esa sesión dejan de servir, y vacía
     * la cookie.
     *
     * @param tokenRefresco valor de la cookie {@code sea_refresh}; puede faltar
     * @return siempre 200
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> cerrarSesion(
            @CookieValue(name = COOKIE_REFRESCO, required = false) String tokenRefresco) {

        authService.cerrarSesion(tokenRefresco);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .body(ApiResponse.ok("Sesión cerrada correctamente"));
    }

    /**
     * {@code POST /api/auth/cambiar-password} — requiere sesión; es la única operación permitida
     * mientras la contraseña temporal no se haya cambiado. Cierra las demás sesiones de la cuenta y
     * devuelve tokens nuevos para este dispositivo.
     *
     * @param peticion contraseña actual y nueva
     * @param http     petición, para la IP y el navegador de la sesión nueva
     * @return 200 con la sesión nueva; 401 si la actual no es correcta; 400 si la nueva no cumple la
     *         política; 409 si es igual a la actual
     */
    @PostMapping("/cambiar-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<JwtResponse>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest peticion, HttpServletRequest http) {

        Long usuarioId = SecurityUtil.obtenerUsuarioId()
                .orElseThrow(() -> new IllegalStateException(MessageConstants.NO_AUTORIZADO));

        AuthService.ResultadoAutenticacion resultado = authService.cambiarPassword(usuarioId, peticion,
                SecurityUtil.obtenerIpCliente(http), SecurityUtil.obtenerUserAgent(http));
        return conCookie(resultado, "Contraseña actualizada correctamente. "
                + "Se cerró la sesión en tus otros dispositivos.");
    }

    /** Respuesta 200 con la cookie de refresco nueva y el token de acceso en el cuerpo. */
    private ResponseEntity<ApiResponse<JwtResponse>> conCookie(
            AuthService.ResultadoAutenticacion resultado, String mensaje) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(resultado.tokenRefresco(),
                        Duration.ofSeconds(tokenProvider.getExpiracionRefrescoSegundos())).toString())
                .body(ApiResponse.ok(resultado.respuesta(), mensaje));
    }

    /**
     * Cookie del token de refresco: HttpOnly, Secure en producción, SameSite según el perfil y ruta
     * acotada a {@code /api/auth} para que no viaje en cada llamada del API.
     */
    private ResponseCookie cookie(String valor, Duration vigencia) {
        return ResponseCookie.from(COOKIE_REFRESCO, valor)
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(vigencia)
                .build();
    }
}
