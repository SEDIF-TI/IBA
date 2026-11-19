package mx.gob.sedif.sea.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Comprueba al arrancar en producción que las direcciones públicas del sistema sean alcanzables
 * desde fuera.
 * Área: Núcleo compartido (configuración). Lógica: códigos QR de certificados y credenciales y
 * enlaces de los avisos.
 *
 * <p>Esas direcciones quedan impresas en documentos que circulan (el QR de la credencial) o en
 * correos ya enviados: si salen con {@code localhost} o con {@code http}, el QR no lleva a ningún
 * lado y no hay forma de corregir lo ya impreso. Por eso, como {@code JwtSecretValidator}, se
 * prefiere que la aplicación no arranque y el error quede a la vista en el despliegue.
 *
 * <p>Solo actúa con el perfil {@code prod}; en desarrollo {@code http://localhost} es lo esperado.
 */
@Component
@Profile("prod")
public class UrlPublicaValidator {

    private static final Logger log = LoggerFactory.getLogger(UrlPublicaValidator.class);

    private final String urlPublica;
    private final String urlVerificacion;
    private final String urlConsulta;

    /**
     * @param urlPublica      {@code app.url-publica} (variable {@code APP_URL_PUBLICA})
     * @param urlVerificacion {@code app.verificacion.url}, destino del QR
     * @param urlConsulta     {@code app.portal.url-consulta}, enlace de los avisos
     */
    public UrlPublicaValidator(@Value("${app.url-publica}") String urlPublica,
                               @Value("${app.verificacion.url}") String urlVerificacion,
                               @Value("${app.portal.url-consulta}") String urlConsulta) {
        this.urlPublica = urlPublica;
        this.urlVerificacion = urlVerificacion;
        this.urlConsulta = urlConsulta;
    }

    /**
     * Revisa las tres direcciones.
     *
     * @throws IllegalStateException si alguna no es {@code https} o apunta a la propia máquina
     */
    @PostConstruct
    public void validar() {
        List<String> problemas = new ArrayList<>();
        revisar("APP_URL_PUBLICA", urlPublica, problemas);
        revisar("app.verificacion.url (QR)", urlVerificacion, problemas);
        revisar("app.portal.url-consulta (avisos)", urlConsulta, problemas);

        if (!problemas.isEmpty()) {
            String separador = "=".repeat(78);
            throw new IllegalStateException(("%n%s%nDIRECCION PUBLICA INVALIDA - LA APLICACION NO PUEDE ARRANCAR%n%s%n%n"
                    + "%s%n%nDefine en el .env del servidor la direccion con la que la gente entra al sistema,%n"
                    + "por ejemplo:%n    APP_URL_PUBLICA=https://diba.sedif.gob.mx%n%s%n")
                    .formatted(separador, separador, String.join("\n", problemas), separador));
        }
        log.info("Direcciones públicas validadas: QR -> {}, avisos -> {}", urlVerificacion, urlConsulta);
    }

    /** Agrega a {@code problemas} lo que impide usar la dirección fuera del servidor. */
    static void revisar(String nombre, String valor, List<String> problemas) {
        URI uri;
        try {
            uri = URI.create(valor == null ? "" : valor.trim());
        } catch (IllegalArgumentException ex) {
            problemas.add("- " + nombre + " no es una dirección válida: " + valor);
            return;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            problemas.add("- " + nombre + " debe empezar con https:// (es: " + valor + ")");
        }
        if (host.isEmpty() || host.equals("localhost") || host.startsWith("127.") || host.equals("0.0.0.0")
                || host.equals("::1") || host.equals("[::1]")) {
            problemas.add("- " + nombre + " apunta a la propia máquina y no funciona desde fuera (es: "
                    + valor + ")");
        }
    }
}
