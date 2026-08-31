package mx.gob.sedif.sea.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion de intercambio entre origenes.
 *
 * <p>La lista de origenes permitidos es explicita y se define por entorno. No
 * se usan comodines: con credenciales habilitadas, un comodin permitiria que
 * cualquier sitio hiciera peticiones autenticadas en nombre de la persona
 * usuaria.
 */
@Configuration
public class WebConfig {

    private final List<String> origenesPermitidos;

    public WebConfig(@Value("${app.cors.allowed-origins}") String origenes) {
        this.origenesPermitidos = Arrays.stream(origenes.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();

        configuracion.setAllowedOrigins(origenesPermitidos);
        configuracion.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        // Necesario para que el navegador envie la cookie del token de refresco.
        configuracion.setAllowCredentials(true);
        // El navegador puede guardar la respuesta de verificacion una hora.
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource origen = new UrlBasedCorsConfigurationSource();
        origen.registerCorsConfiguration("/**", configuracion);
        return origen;
    }
}
