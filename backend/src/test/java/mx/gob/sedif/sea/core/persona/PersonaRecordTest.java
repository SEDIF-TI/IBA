package mx.gob.sedif.sea.core.persona;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del enmascarado de datos de contacto en el listado del padrón de tutores.
 */
class PersonaRecordTest {

    @Test
    @DisplayName("el teléfono muestra solo los últimos cuatro dígitos")
    void enmascaraTelefono() {
        assertThat(PersonaRecord.enmascararTelefono("2221234567")).isEqualTo("••••••4567");
        assertThat(PersonaRecord.enmascararTelefono(null)).isNull();
        assertThat(PersonaRecord.enmascararTelefono("123")).isEqualTo("••••");
    }

    @Test
    @DisplayName("el correo muestra la inicial y el dominio")
    void enmascaraCorreo() {
        assertThat(PersonaRecord.enmascararCorreo("juana.perez@gmail.com")).isEqualTo("j•••@gmail.com");
        assertThat(PersonaRecord.enmascararCorreo("")).isNull();
        assertThat(PersonaRecord.enmascararCorreo("sin-arroba")).isEqualTo("•••");
    }
}
