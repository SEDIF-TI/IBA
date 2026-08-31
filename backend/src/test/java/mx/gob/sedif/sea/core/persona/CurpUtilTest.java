package mx.gob.sedif.sea.core.persona;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifica la lectura y comprobacion de la CURP.
 *
 * <p>Importa que sea estricta: la CURP es la identidad del tutor y el unico
 * modo de reconocer que quien registra hoy un gato es la misma persona que
 * manana registra un perro.
 */
class CurpUtilTest {

    @Nested
    @DisplayName("Comprobacion completa")
    class Comprobacion {

        @Test
        @DisplayName("acepta una CURP bien formada")
        void aceptaCurpValida() {
            // Construida con el algoritmo de RENAPO, incluido su digito verificador.
            String curp = construirValida("PEGA850315HPLRRN", "0");
            assertThat(CurpUtil.esValida(curp))
                    .as("la CURP %s deberia ser valida", curp)
                    .isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                "PEGA850315HPLRRN0",      // 17 posiciones
                "PEGA850315HPLRRN012",    // 19 posiciones
                "1234850315HPLRRN01",     // empieza con digitos
                "PEGA850315XPLRRN01",     // sexo invalido
                "PEGA851315HPLRRN01",     // mes 13
                "PEGA850332HPLRRN01",     // dia 32
                "PEGA850315HZZRRN01",     // entidad inexistente
                "PE GA850315HPLRRN0"      // con espacio
        })
        @DisplayName("rechaza CURP mal formadas")
        void rechazaInvalidas(String curp) {
            assertThat(CurpUtil.esValida(curp)).isFalse();
        }

        @Test
        @DisplayName("rechaza una CURP con digito verificador incorrecto")
        void rechazaDigitoIncorrecto() {
            String valida = construirValida("PEGA850315HPLRRN", "0");
            char ultimo = valida.charAt(17);
            char otro = ultimo == '0' ? '1' : '0';
            String alterada = valida.substring(0, 17) + otro;

            assertThat(CurpUtil.esValida(alterada))
                    .as("un digito verificador que no cuadra debe rechazarse")
                    .isFalse();
        }

        @Test
        @DisplayName("acepta la clave NE de nacidos en el extranjero")
        void aceptaNacidoEnExtranjero() {
            String curp = construirValida("PEGA850315HNERRN", "0");
            assertThat(CurpUtil.esValida(curp)).isTrue();
        }

        @Test
        @DisplayName("no distingue mayusculas ni espacios sobrantes")
        void normalizaAntesDeComprobar() {
            String curp = construirValida("PEGA850315HPLRRN", "0");
            assertThat(CurpUtil.esValida("  " + curp.toLowerCase() + "  ")).isTrue();
        }
    }

    @Nested
    @DisplayName("Datos derivados")
    class DatosDerivados {

        @Test
        @DisplayName("obtiene la fecha de nacimiento")
        void obtieneFecha() {
            String curp = construirValida("PEGA850315HPLRRN", "0");
            assertThat(CurpUtil.extraerFechaNacimiento(curp))
                    .contains(LocalDate.of(1985, 3, 15));
        }

        @Test
        @DisplayName("deduce el siglo por la homoclave")
        void deduceSiglo() {
            // Homoclave con digito: siglo XX.
            assertThat(CurpUtil.extraerFechaNacimiento("PEGA850315HPLRRN09"))
                    .contains(LocalDate.of(1985, 3, 15));
            // Homoclave con letra: siglo XXI.
            assertThat(CurpUtil.extraerFechaNacimiento("PEGA050315HPLRRNA9"))
                    .contains(LocalDate.of(2005, 3, 15));
        }

        @Test
        @DisplayName("descarta una fecha posterior a hoy")
        void descartaFechaFutura() {
            int anioFuturo = LocalDate.now().plusYears(1).getYear() % 100;
            String curp = "PEGA%02d0315HPLRRNA9".formatted(anioFuturo);
            assertThat(CurpUtil.extraerFechaNacimiento(curp)).isEmpty();
        }

        @Test
        @DisplayName("obtiene el sexo")
        void obtieneSexo() {
            assertThat(CurpUtil.extraerSexo("PEGA850315HPLRRN09")).contains("H");
            assertThat(CurpUtil.extraerSexo("PEGA850315MPLRRN09")).contains("M");
        }

        @Test
        @DisplayName("obtiene la entidad y su nombre")
        void obtieneEntidad() {
            assertThat(CurpUtil.extraerEntidad("PEGA850315HPLRRN09")).contains("PL");
            assertThat(CurpUtil.nombreEntidad("PL")).contains("Puebla");
            assertThat(CurpUtil.nombreEntidad("NE")).contains("Nacido en el extranjero");
        }
    }

    @Nested
    @DisplayName("Cotejo con el nombre capturado")
    class CotejoConNombre {

        @ParameterizedTest
        @CsvSource({
                "Juan,      Perez,    Gomez,     PEGJ",
                "Ana,       Lopez,    Martinez,  LOMA",
                "Carlos,    Hernandez,Ruiz,      HERC"
        })
        @DisplayName("arma las iniciales segun la regla de RENAPO")
        void armaIniciales(String nombre, String paterno, String materno, String esperado) {
            assertThat(CurpUtil.construirIniciales(nombre, paterno, materno))
                    .isEqualTo(esperado);
        }

        @Test
        @DisplayName("omite Maria o Jose cuando hay un segundo nombre")
        void omiteNombresMuyComunes() {
            // Se toma Guadalupe, no Maria.
            assertThat(CurpUtil.construirIniciales("Maria Guadalupe", "Perez", "Gomez"))
                    .isEqualTo("PEGG");
            assertThat(CurpUtil.construirIniciales("Jose Luis", "Perez", "Gomez"))
                    .isEqualTo("PEGL");
        }

        @Test
        @DisplayName("usa X cuando no hay apellido materno")
        void usaXSinApellidoMaterno() {
            assertThat(CurpUtil.construirIniciales("Juan", "Perez", null))
                    .isEqualTo("PEXJ");
        }

        @Test
        @DisplayName("ignora acentos del nombre capturado")
        void ignoraAcentos() {
            assertThat(CurpUtil.construirIniciales("José", "Pérez", "Gómez"))
                    .isEqualTo(CurpUtil.construirIniciales("Jose", "Perez", "Gomez"));
        }

        @Test
        @DisplayName("detecta que la CURP no corresponde con el nombre")
        void detectaNombreQueNoCorresponde() {
            String curp = construirValida("PEGJ850315HPLRRN", "0");

            assertThat(CurpUtil.coincideConNombre(curp, "Juan", "Perez", "Gomez")).isTrue();
            assertThat(CurpUtil.coincideConNombre(curp, "Ana", "Lopez", "Martinez"))
                    .as("una CURP ajena al nombre capturado debe delatarse")
                    .isFalse();
        }
    }

    /**
     * Completa una CURP de 16 posiciones calculando su digito verificador.
     *
     * @param base       las primeras 16 posiciones
     * @param homoclave  posicion 17
     */
    private static String construirValida(String base, String homoclave) {
        String sinDigito = base + homoclave;
        String alfabeto = "0123456789ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";
        int suma = 0;
        for (int i = 0; i < 17; i++) {
            suma += alfabeto.indexOf(sinDigito.charAt(i)) * (18 - i);
        }
        int digito = (10 - (suma % 10)) % 10;
        return sinDigito + digito;
    }
}
