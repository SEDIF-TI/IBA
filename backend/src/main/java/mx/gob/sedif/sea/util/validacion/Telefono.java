package mx.gob.sedif.sea.util.validacion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

/**
 * Teléfono de exactamente 10 dígitos, sin espacios, guiones ni letras. Acepta vacío o nulo.
 * Área: Núcleo compartido (validación). Misma regla que {@code validarTelefono} del navegador.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = "^$|^[0-9]{10}$")
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Telefono {

    String message() default "El teléfono debe tener exactamente 10 dígitos, sin espacios ni guiones";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
