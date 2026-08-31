package mx.gob.sedif.sea.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import mx.gob.sedif.sea.security.JwtAuthFilter;
import mx.gob.sedif.sea.security.RateLimitingFilter;
import mx.gob.sedif.sea.security.UserDetailsServiceImpl;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Cadena de seguridad del sistema.
 *
 * <p>Decisiones que la gobiernan:
 * <ul>
 *   <li>Sin estado: la sesion vive en el token, no en el servidor.</li>
 *   <li>Sin CSRF: no hay cookies de sesion que un sitio ajeno pueda reutilizar.
 *       El token de refresco si viaja en cookie, pero se marca SameSite.</li>
 *   <li>Todo destino exige autenticacion salvo los explicitamente publicos.
 *       Es al reves de lo habitual a proposito: si alguien agrega un endpoint y
 *       olvida protegerlo, queda cerrado en lugar de abierto.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          RateLimitingFilter rateLimitingFilter,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Sin CSRF: no hay sesion en servidor que suplantar.
            .csrf(csrf -> csrf.disable())

            // El origen permitido lo define WebConfig, con lista explicita.
            .cors(cors -> {})

            .sessionManagement(sesion -> sesion
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(peticiones -> peticiones
                    // ---- Publico: sin autenticacion ----
                    // Inicio de sesion y refresco.
                    .requestMatchers("/auth/login", "/auth/refrescar").permitAll()
                    // Portal ciudadano: alta de tramite, consulta de folio,
                    // verificacion del certificado por QR y calendario de jornadas.
                    .requestMatchers("/publico/**").permitAll()
                    // Sonda de salud del contenedor.
                    .requestMatchers("/actuator/health").permitAll()

                    // ---- Todo lo demas exige sesion ----
                    .anyRequest().authenticated())

            .exceptionHandling(manejo -> manejo
                    .authenticationEntryPoint(this::responderNoAutenticado)
                    .accessDeniedHandler((peticion, respuesta, ex) ->
                            responderSinPermiso(respuesta)))

            // Encabezados de proteccion del navegador.
            .headers(encabezados -> encabezados
                    // Impide que el sitio se cargue dentro de un marco ajeno,
                    // que es como se enganan los clics.
                    .frameOptions(marco -> marco.deny())
                    // Obliga a respetar el tipo declarado en lugar de adivinarlo.
                    .contentTypeOptions(tipo -> {})
                    .xssProtection(xss -> xss
                            .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    // Exige HTTPS durante un ano, subdominios incluidos.
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000))
                    // El API solo devuelve datos: no necesita cargar nada de fuera.
                    .contentSecurityPolicy(politica -> politica
                            .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                    // No filtrar la direccion interna al navegar a otro sitio.
                    .referrerPolicy(referencia -> referencia
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))

            // El limite de peticiones va primero: frena la fuerza bruta antes de
            // gastar recursos verificando tokens o consultando la base.
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cifrado de contrasenas.
     *
     * <p>Factor 12: mas lento a proposito, para que probar contrasenas al por
     * mayor resulte costoso.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider();
        proveedor.setUserDetailsService(userDetailsService);
        proveedor.setPasswordEncoder(passwordEncoder());
        // Comprobar siempre la contrasena, incluso si el usuario no existe:
        // si se omitiera, la respuesta seria mas rapida y revelaria que cuentas
        // existen por el tiempo que tarda.
        proveedor.setHideUserNotFoundExceptions(true);
        return proveedor;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    /** Falta de sesion: 401 con el formato uniforme del API. */
    private void responderNoAutenticado(jakarta.servlet.http.HttpServletRequest peticion,
                                        HttpServletResponse respuesta,
                                        org.springframework.security.core.AuthenticationException ex)
            throws IOException {
        respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(
                """
                {"success":false,"message":"La sesion expiro. Inicia sesion nuevamente."}""");
    }

    /** Sesion valida pero sin permisos: 403. */
    private void responderSinPermiso(HttpServletResponse respuesta) throws IOException {
        respuesta.setStatus(HttpStatus.FORBIDDEN.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(
                """
                {"success":false,"message":"No tienes autorizacion para realizar esta accion"}""");
    }
}
