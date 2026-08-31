package mx.gob.sedif.sea.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Comprueba al arranque que el secreto de firma sea utilizable.
 *
 * <p>Si el secreto es corto, repetitivo o sigue siendo el de ejemplo, la
 * aplicacion NO arranca. Es deliberado: un despliegue con secreto debil
 * permite falsificar tokens y entrar como cualquier usuario, y ese fallo es
 * silencioso, no da sintomas hasta que alguien lo aprovecha.
 *
 * <p>Preferimos que el sistema se niegue a levantar y quede evidente en la
 * bitacora de despliegue.
 */
@Component
public class JwtSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);

    /** HMAC-SHA256 exige al menos 256 bits de clave; se pide holgura. */
    private static final int LONGITUD_MINIMA = 32;

    /** Longitud recomendada, la que produce {@code openssl rand -base64 64}. */
    private static final int LONGITUD_RECOMENDADA = 64;

    /** Caracteres distintos minimos: descarta secretos como "aaaa...". */
    private static final int ENTROPIA_MINIMA = 16;

    /** Valores conocidos que jamas deben llegar a un despliegue. */
    private static final List<String> VALORES_PROHIBIDOS = List.of(
            "cambiame",
            "changeme",
            "secret",
            "password",
            "12345678",
            "mi-clave-secreta",
            "your-256-bit-secret",
            "generar_con_openssl");

    private final String secreto;

    public JwtSecretValidator(@Value("${app.jwt.secret:}") String secreto) {
        this.secreto = secreto;
    }

    /**
     * Valida el secreto antes de que se construya nada que dependa de el.
     *
     * @throws IllegalStateException si el secreto no sirve, lo que impide el arranque
     */
    @PostConstruct
    public void validar() {
        if (secreto == null || secreto.isBlank()) {
            fallar("""
                    No se configuro el secreto de firma de los tokens.

                    Define APP_JWT_SECRET en el archivo .env. Genera un valor con:
                        openssl rand -base64 64""");
        }

        String limpio = secreto.trim();

        if (limpio.length() < LONGITUD_MINIMA) {
            fallar("""
                    El secreto de firma es demasiado corto: %d caracteres, se requieren %d como minimo.

                    Genera uno adecuado con:
                        openssl rand -base64 64"""
                    .formatted(limpio.length(), LONGITUD_MINIMA));
        }

        String enMinusculas = limpio.toLowerCase();
        for (String prohibido : VALORES_PROHIBIDOS) {
            if (enMinusculas.contains(prohibido)) {
                fallar("""
                        El secreto de firma contiene un valor de ejemplo ("%s"), asi que es publico.

                        Genera uno propio con:
                            openssl rand -base64 64"""
                        .formatted(prohibido));
            }
        }

        long caracteresDistintos = limpio.chars().distinct().count();
        if (caracteresDistintos < ENTROPIA_MINIMA) {
            fallar("""
                    El secreto de firma tiene muy poca variedad: %d caracteres distintos, \
                    se requieren %d como minimo.

                    Genera uno aleatorio con:
                        openssl rand -base64 64"""
                    .formatted(caracteresDistintos, ENTROPIA_MINIMA));
        }

        // Descarta secretos formados repitiendo un patron corto, que superan la
        // cuenta de caracteres distintos pero no aportan aleatoriedad real.
        if (esPatronRepetido(limpio)) {
            fallar("""
                    El secreto de firma es la repeticion de un patron, no un valor aleatorio.

                    Genera uno con:
                        openssl rand -base64 64""");
        }

        int bits = limpio.getBytes(StandardCharsets.UTF_8).length * 8;
        if (limpio.length() < LONGITUD_RECOMENDADA) {
            log.warn("El secreto de firma tiene {} caracteres ({} bits). Es aceptable, "
                            + "pero se recomiendan {} caracteres para mayor margen.",
                    limpio.length(), bits, LONGITUD_RECOMENDADA);
        }
    }

    /** Deja constancia en bitacora una vez que el sistema quedo listo. */
    @EventListener(ApplicationReadyEvent.class)
    public void registrarValidacion() {
        log.info("Secreto de firma validado correctamente");
    }

    /** Verdadero si la cadena es un patron corto repetido, como "abcabcabc". */
    private boolean esPatronRepetido(String valor) {
        for (int tamano = 1; tamano <= valor.length() / 3; tamano++) {
            if (valor.length() % tamano != 0) {
                continue;
            }
            String patron = valor.substring(0, tamano);
            if (patron.repeat(valor.length() / tamano).equals(valor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detiene el arranque con un mensaje que explica como corregirlo.
     *
     * <p>Se enmarca para que resalte entre el resto de la bitacora de arranque.
     */
    private void fallar(String motivo) {
        String separador = "=".repeat(78);
        throw new IllegalStateException(
                "%n%s%nCONFIGURACION DE SEGURIDAD INVALIDA - LA APLICACION NO PUEDE ARRANCAR%n%s%n%n%s%n%s%n"
                        .formatted(separador, separador, motivo, separador));
    }
}
