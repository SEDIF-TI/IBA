package mx.gob.sedif.sea.core.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica la sustitucion de marcadores en las plantillas de aviso.
 *
 * <p>Las plantillas las edita el personal, asi que pueden traer marcadores mal
 * escritos. Lo que importa es que un error de redaccion no impida que el aviso
 * salga, y que el hueco quede visible para poder corregirlo.
 */
class PlantillaResolverTest {

    private final PlantillaResolver resolver = new PlantillaResolver();

    @Test
    @DisplayName("sustituye los marcadores por sus valores")
    void sustituyeLosMarcadores() {
        String plantilla = "Hola {{nombre}}: tu tramite {{folio}} quedo registrado.";

        String resultado = resolver.resolver(plantilla,
                Map.of("nombre", "Juan", "folio", "SOL-20260901-00001"));

        assertThat(resultado)
                .isEqualTo("Hola Juan: tu tramite SOL-20260901-00001 quedo registrado.");
    }

    @Test
    @DisplayName("sustituye el mismo marcador cuantas veces aparezca")
    void sustituyeMarcadoresRepetidos() {
        String resultado = resolver.resolver(
                "Folio {{folio}}. Guarda el folio {{folio}}.",
                Map.of("folio", "SOL-001"));

        assertThat(resultado).isEqualTo("Folio SOL-001. Guarda el folio SOL-001.");
    }

    @Test
    @DisplayName("deja visible el marcador que no tiene dato, en lugar de vaciarlo")
    void dejaVisibleElMarcadorSinDato() {
        String resultado = resolver.resolver(
                "Hola {{nombre}}, tu cita es el {{fecha_cita}}.",
                Map.of("nombre", "Juan"));

        // Un hueco vacio pasaria inadvertido; el marcador visible delata que
        // falta un dato y permite corregir la plantilla.
        assertThat(resultado)
                .contains("Hola Juan")
                .contains("{{fecha_cita}}");
    }

    @Test
    @DisplayName("no falla con una plantilla sin marcadores")
    void admitePlantillaSinMarcadores() {
        assertThat(resolver.resolver("Texto fijo sin variables", Map.of()))
                .isEqualTo("Texto fijo sin variables");
    }

    @Test
    @DisplayName("no falla con una plantilla vacia")
    void admitePlantillaVacia() {
        assertThat(resolver.resolver(null, Map.of())).isEmpty();
        assertThat(resolver.resolver("", Map.of())).isEmpty();
    }

    @Test
    @DisplayName("no interpreta como marcador lo que no tiene la forma correcta")
    void ignoraFormasIncorrectas() {
        String plantilla = "Un {marcador} suelto y otro {{ con espacio }}";

        assertThat(resolver.resolver(plantilla, Map.of("marcador", "X")))
                .isEqualTo(plantilla);
    }

    @Test
    @DisplayName("enumera los marcadores que quedaron sin resolver")
    void enumeraLosPendientes() {
        String texto = "Hola {{nombre}}, tu cita es el {{fecha_cita}} en {{sede}}.";

        assertThat(resolver.marcadoresSinResolver(texto))
                .containsExactly("nombre", "fecha_cita", "sede");
    }

    @Test
    @DisplayName("un valor con simbolos no rompe la sustitucion")
    void admiteValoresConSimbolos() {
        // Los folios llevan diagonales y los importes signos de peso: ninguno
        // debe interpretarse como parte de la expresion de sustitucion.
        String resultado = resolver.resolver("Folio: {{folio}}",
                Map.of("folio", "SEDIF/IBA/EMP-21114/00001/2026"));

        assertThat(resultado).isEqualTo("Folio: SEDIF/IBA/EMP-21114/00001/2026");
    }
}
