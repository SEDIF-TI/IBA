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
 * Código postal de exactamente 5 dígitos. Acepta vacío o nulo.
 * Área: Núcleo compartido (validación). Misma regla que {@code validarCodigoPostal} del navegador.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = "^$|^[0-9]{5}$")
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CodigoPostal {

    String message() default "El código postal debe tener 5 dígitos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
