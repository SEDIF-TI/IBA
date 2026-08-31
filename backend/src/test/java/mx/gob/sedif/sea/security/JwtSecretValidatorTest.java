package mx.gob.sedif.sea.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifica que un secreto de firma inseguro impida el arranque.
 *
 * <p>Cada caso rechazado representa un despliegue que habria quedado expuesto a
 * falsificacion de tokens.
 */
class JwtSecretValidatorTest {

    /** Secreto realista, del estilo que produce openssl rand -base64 64. */
    private static final String SECRETO_VALIDO =
            "K7f2Bq9xZmW4tR8vL3nH6yJ1cP5dS0aE9gU2iO7kX4wQ8zM1bN6rT3hV5jY0lD2f";

    @Test
    @DisplayName("acepta un secreto aleatorio de longitud suficiente")
    void aceptaSecretoValido() {
        assertThatCode(() -> new JwtSecretValidator(SECRETO_VALIDO).validar())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("rechaza el secreto ausente o en blanco")
    void rechazaSecretoAusente(String secreto) {
        assertThatThrownBy(() -> new JwtSecretValidator(secreto).validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "corto",
            "abc123",
            "K7f2Bq9xZmW4tR8vL3nH6yJ1cP5dS0"  // 30 caracteres: por debajo del minimo
    })
    @DisplayName("rechaza secretos por debajo de la longitud minima")
    void rechazaSecretoCorto(String secreto) {
        assertThatThrownBy(() -> new JwtSecretValidator(secreto).validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demasiado corto");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CAMBIAME_generar_con_openssl_rand_base64_64_minimo_64_caracteres",
            "mi-clave-secreta-para-firmar-tokens-del-sistema-sea-2026-puebla",
            "este-es-un-secret-largo-pero-contiene-palabra-prohibida-adentro",
            "changeme-changeme-abcdefghijklmnop-qrstuvwxyz-0123456789-XYZWQ"
    })
    @DisplayName("rechaza secretos con valores de ejemplo conocidos")
    void rechazaValoresDeEjemplo(String secreto) {
        assertThatThrownBy(() -> new JwtSecretValidator(secreto).validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valor de ejemplo");
    }

    @Test
    @DisplayName("rechaza un secreto de un solo caracter repetido")
    void rechazaCaracterRepetido() {
        assertThatThrownBy(() -> new JwtSecretValidator("a".repeat(80)).validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("poca variedad");
    }

    @Test
    @DisplayName("rechaza un secreto formado repitiendo un patron")
    void rechazaPatronRepetido() {
        // Supera la cuenta de caracteres distintos, pero no aporta aleatoriedad.
        String patron = "aB3xY9zQ7wE2rT5u";
        assertThatThrownBy(() -> new JwtSecretValidator(patron.repeat(4)).validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("repeticion de un patron");
    }

    @Test
    @DisplayName("el mensaje de error explica como generar un secreto valido")
    void mensajeIncluyeInstruccion() {
        assertThatThrownBy(() -> new JwtSecretValidator("corto").validar())
                .hasMessageContaining("openssl rand -base64 64");
    }

    @Test
    @DisplayName("el secreto del archivo de ejemplo versionado no sirve para arrancar")
    void rechazaElSecretoDelEnvExample() {
        String delEjemplo = "CAMBIAME_generar_con_openssl_rand_base64_64_minimo_64_caracteres";
        assertThatThrownBy(() -> new JwtSecretValidator(delEjemplo).validar())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("acepta con aviso un secreto valido pero mas corto de lo recomendado")
    void aceptaConAvisoSecretoAceptable() {
        String cuarentaCaracteres = "K7f2Bq9xZmW4tR8vL3nH6yJ1cP5dS0aE9gU2iO7k";
        assertThat(cuarentaCaracteres).hasSize(40);
        assertThatCode(() -> new JwtSecretValidator(cuarentaCaracteres).validar())
                .doesNotThrowAnyException();
    }
}
