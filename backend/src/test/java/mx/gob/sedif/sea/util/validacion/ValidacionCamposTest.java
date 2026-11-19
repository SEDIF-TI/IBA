package mx.gob.sedif.sea.util.validacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Pruebas de las reglas comunes de validación de campos con formato.
 *
 * <p>Flujo cubierto: toda captura (portal, ventanilla, personal, consulta pública) valida CURP,
 * correo, teléfono y código postal con la misma regla en el servidor, aunque se llame a la API sin
 * pasar por el navegador.
 */
class ValidacionCamposTest {

    /** Campo de un Request y la anotación que debe llevar según su nombre. */
    private static final Map<Pattern, Class<? extends Annotation>> REGLAS = Map.of(
            Pattern.compile("(?i)^curp$"), CurpValida.class,
            Pattern.compile("(?i)^(correo|email)$"), Correo.class,
            Pattern.compile("(?i)^telefono\\w*$"), Telefono.class,
            Pattern.compile("(?i)^codigoPostal$"), CodigoPostal.class);

    private static Validator validador;

    @BeforeAll
    static void preparar() {
        validador = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /** Muestra para validar un solo valor con la anotación indicada. */
    record ConCurp(@CurpValida String valor) { }

    record ConCorreo(@Correo String valor) { }

    record ConTelefono(@Telefono String valor) { }

    record ConCodigoPostal(@CodigoPostal String valor) { }

    private static Set<ConstraintViolation<Object>> validar(Object muestra) {
        return validador.validate(muestra);
    }

    @Test
    @DisplayName("todo *Request con CURP, correo, teléfono o código postal lleva su anotación")
    void losCamposConFormatoUsanSuRegla() throws Exception {
        ClassPathScanningCandidateComponentProvider escaner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definicion) {
                        return true;
                    }
                };
        escaner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Request$")));

        List<String> faltantes = new ArrayList<>();
        int revisados = 0;
        for (BeanDefinition definicion : escaner.findCandidateComponents("mx.gob.sedif.sea")) {
            Class<?> clase = Class.forName(definicion.getBeanClassName());
            if (!clase.isRecord()) {
                continue;
            }
            for (RecordComponent componente : clase.getRecordComponents()) {
                for (Map.Entry<Pattern, Class<? extends Annotation>> regla : REGLAS.entrySet()) {
                    if (regla.getKey().matcher(componente.getName()).matches()) {
                        revisados++;
                        boolean anotado = clase.getDeclaredField(componente.getName())
                                .isAnnotationPresent(regla.getValue());
                        if (!anotado) {
                            faltantes.add(clase.getSimpleName() + "." + componente.getName()
                                    + " sin @" + regla.getValue().getSimpleName());
                        }
                    }
                }
            }
        }
        assertThat(revisados).as("campos con formato encontrados").isGreaterThanOrEqualTo(10);
        assertThat(faltantes).as("campos sin su regla de validación").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PEGA850315HPLRRN08", "pega850315hplrrn08", " PEGA850315HPLRRN08 ", ""})
    @DisplayName("acepta una CURP real, en minúsculas o con espacios, y el vacío")
    void curpValida(String curp) {
        assertThat(validar(new ConCurp(curp))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PEGA850315HPLRRN09", "PEGA851315HPLRRN08", "PEGA850315HXXRRN08",
                            "PEGA850315HPLRRN0", "1234567890ABCDEFGH", "PEGA-850315-HPLRRN"})
    @DisplayName("rechaza dígito verificador, mes, entidad, longitud o estructura incorrectos")
    void curpInvalida(String curp) {
        assertThat(validar(new ConCurp(curp))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre@gmail.com", "nombre.apellido@puebla.gob.mx", ""})
    @DisplayName("acepta correos completos y el vacío")
    void correoValido(String correo) {
        assertThat(validar(new ConCorreo(correo))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre@gmail", "nombregmail.com", "nombre@@gmail.com", "nom bre@gmail.com",
                            "@gmail.com", "nombre@.c"})
    @DisplayName("rechaza correos sin terminación, sin arroba, con dos arrobas o con espacios")
    void correoInvalido(String correo) {
        assertThat(validar(new ConCorreo(correo))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"222123456", "22212345678", "222-123-456", "222 123 4567", "22212345a7"})
    @DisplayName("el teléfono solo admite exactamente 10 dígitos")
    void telefonoInvalido(String telefono) {
        assertThat(validar(new ConTelefono(telefono))).hasSize(1);
        assertThat(validar(new ConTelefono("2221234567"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"7200", "720001", "72a00"})
    @DisplayName("el código postal solo admite exactamente 5 dígitos")
    void codigoPostalInvalido(String codigo) {
        assertThat(validar(new ConCodigoPostal(codigo))).hasSize(1);
        assertThat(validar(new ConCodigoPostal("72000"))).isEmpty();
    }
}
