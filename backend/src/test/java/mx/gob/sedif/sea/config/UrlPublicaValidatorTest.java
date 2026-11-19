package mx.gob.sedif.sea.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas de la validación de la dirección pública en producción.
 *
 * <p>Flujo cubierto: el QR de certificados y credenciales y los enlaces de los avisos deben apuntar
 * al dominio público con https; si no, el sistema no arranca en producción.
 */
class UrlPublicaValidatorTest {

    private static UrlPublicaValidator con(String base) {
        return new UrlPublicaValidator(base, base + "/verificar", base + "/consulta");
    }

    @Test
    @DisplayName("acepta el dominio público con https")
    void aceptaDominio() {
        assertThatCode(() -> con("https://diba.sedif.gob.mx").validar()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost", "https://localhost", "http://diba.sedif.gob.mx",
                            "https://127.0.0.1", "", "diba.sedif.gob.mx"})
    @DisplayName("rechaza localhost, http, la propia máquina o una dirección sin esquema")
    void rechazaDireccionesInservibles(String base) {
        assertThatThrownBy(() -> con(base).validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_URL_PUBLICA=https://diba.sedif.gob.mx");
    }
}
