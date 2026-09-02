package mx.gob.sedif.sea.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum;

/**
 * Verifica que los mensajes del sistema digan algo util.
 *
 * <p>Un mensaje vago obliga a la persona a adivinar que hizo mal, y en un
 * tramite de gobierno eso se traduce en una llamada a la oficina o en un
 * tramite abandonado. Estas pruebas fijan el criterio para que no se degrade
 * cuando alguien agregue un mensaje nuevo.
 */
class MensajesTest {

    /** Textos que no dicen nada por si solos. */
    private static final List<String> FRASES_VACIAS = List.of(
            "error desconocido",
            "operacion invalida",
            "datos incorrectos",
            "algo salio mal",
            "error inesperado");

    @Nested
    @DisplayName("Catalogo de mensajes")
    class CatalogoDeMensajes {

        @Test
        @DisplayName("ningun mensaje es tan corto que no explique nada")
        void ningunMensajeEsDemasiadoCorto() throws Exception {
            List<String> cortos = new ArrayList<>();

            for (Field campo : constantes()) {
                String mensaje = (String) campo.get(null);
                // Menos de 25 caracteres no alcanza para decir que paso.
                if (mensaje.length() < 25) {
                    cortos.add(campo.getName() + ": \"" + mensaje + "\"");
                }
            }

            assertThat(cortos)
                    .as("estos mensajes no alcanzan a explicar que ocurrio")
                    .isEmpty();
        }

        @Test
        @DisplayName("ningun mensaje usa frases que no dicen nada")
        void ningunMensajeEsVago() throws Exception {
            List<String> vagos = new ArrayList<>();

            for (Field campo : constantes()) {
                String mensaje = ((String) campo.get(null)).toLowerCase();
                for (String frase : FRASES_VACIAS) {
                    if (mensaje.contains(frase)) {
                        vagos.add(campo.getName() + " contiene \"" + frase + "\"");
                    }
                }
            }

            assertThat(vagos)
                    .as("estas frases obligan a la persona a adivinar")
                    .isEmpty();
        }

        @Test
        @DisplayName("los mensajes de error dicen que hacer a continuacion")
        void losErroresDicenQueHacer() throws Exception {
            // Palabras que indican una salida: un verbo de accion o una via de
            // solucion. No todos los mensajes la necesitan, pero los que
            // describen un problema del lado de la persona, si.
            // Se buscan las raices, no las formas conjugadas: "elegir" y
            // "elige" valen lo mismo para lo que se esta comprobando.
            List<String> conSalida = List.of(
                    "verific", "revis", "intent", "solicit", "esper", "escrib",
                    "reduc", "adjunt", "eleg", "elige", "defin", "vuelv", "pid",
                    "report", "acud", "tomalo", "registr", "cancel", "corrig",
                    "subelo", "comprueba");

            List<String> sinSalida = new ArrayList<>();

            for (Field campo : constantes()) {
                String nombre = campo.getName();
                String mensaje = ((String) campo.get(null)).toLowerCase();

                // Solo se exige a los que describen algo que la persona puede resolver.
                boolean esProblemaDeLaPersona = nombre.contains("INVALID")
                        || nombre.contains("REQUERIDO")
                        || nombre.contains("NO_PERMITIDO")
                        || nombre.contains("MUY_GRANDE")
                        || nombre.contains("VACIO")
                        || nombre.contains("DUPLICADA");

                // Un mensaje tambien orienta cuando enumera lo que si se
                // admite, aunque no lleve un verbo de accion.
                boolean enumeraAlternativas = mensaje.contains("solo se admiten")
                        || mensaje.contains("solo se acepta")
                        || mensaje.contains("se admiten");

                if (esProblemaDeLaPersona
                        && !enumeraAlternativas
                        && conSalida.stream().noneMatch(mensaje::contains)) {
                    sinSalida.add(nombre + ": \"" + mensaje + "\"");
                }
            }

            assertThat(sinSalida)
                    .as("estos mensajes describen el problema pero no como resolverlo")
                    .isEmpty();
        }

        @Test
        @DisplayName("el mensaje de credenciales no revela si el usuario existe")
        void elMensajeDeCredencialesNoRevelaNada() {
            String mensaje = MessageConstants.CREDENCIALES_INVALIDAS.toLowerCase();

            // Es la excepcion deliberada a la regla de precision: precisar cual
            // de los dos fallo permitiria averiguar que cuentas existen.
            assertThat(mensaje)
                    .doesNotContain("no existe")
                    .doesNotContain("no se encontro el usuario")
                    .doesNotContain("contrasena incorrecta para");
            assertThat(mensaje).contains("usuario o contrasena");
        }

        /** Constantes de texto del catalogo de mensajes. */
        private List<Field> constantes() {
            List<Field> campos = new ArrayList<>();
            for (Field campo : MessageConstants.class.getDeclaredFields()) {
                if (Modifier.isStatic(campo.getModifiers())
                        && campo.getType() == String.class) {
                    campos.add(campo);
                }
            }
            return campos;
        }
    }

    @Nested
    @DisplayName("Transiciones invalidas")
    class TransicionesInvalidas {

        @Test
        @DisplayName("explica que se intento, desde donde, y que si se puede hacer")
        void explicaElPorQueYLaSalida() {
            var error = new TransicionInvalidaException(
                    EstatusSolicitudEnum.REGISTRADA, EstatusSolicitudEnum.APROBADA);

            assertThat(error.getMessage())
                    .contains("aprobar")
                    .contains("recien registrado")
                    .contains("Tomalo primero para revision");
        }

        @Test
        @DisplayName("avisa cuando el tramite ya concluyo")
        void avisaQueYaConcluyo() {
            var error = new TransicionInvalidaException(
                    EstatusSolicitudEnum.ENTREGADA, EstatusSolicitudEnum.RECHAZADA);

            assertThat(error.getMessage())
                    .contains("rechazar")
                    .contains("entregado")
                    .contains("ya concluyo");
        }

        @Test
        @DisplayName("no usa las claves internas del sistema")
        void noUsaClavesInternas() {
            var error = new TransicionInvalidaException(
                    EstatusSolicitudEnum.OBSERVADA, EstatusSolicitudEnum.APROBADA);

            // "CERTIFICADO_EMITIDO" no le dice nada a quien esta capturando.
            assertThat(error.getMessage())
                    .doesNotContain("OBSERVADA")
                    .doesNotContain("EN_REVISION")
                    .doesNotContain("_");
        }

        @Test
        @DisplayName("cubre todas las combinaciones sin quedarse sin texto")
        void cubreTodasLasCombinaciones() {
            for (EstatusSolicitudEnum origen : EstatusSolicitudEnum.values()) {
                for (EstatusSolicitudEnum destino : EstatusSolicitudEnum.values()) {
                    var error = new TransicionInvalidaException(origen, destino);
                    assertThat(error.getMessage())
                            .as("%s a %s deberia explicarse", origen, destino)
                            .isNotBlank()
                            .hasSizeGreaterThan(40);
                }
            }
        }
    }
}
