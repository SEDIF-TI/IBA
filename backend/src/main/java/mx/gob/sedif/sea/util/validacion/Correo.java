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
import jakarta.validation.constraints.Size;

/**
 * Correo electrónico con usuario, una sola arroba y dominio con terminación (por ejemplo
 * {@code nombre@gmail.com}). Acepta vacío o nulo.
 * Área: Núcleo compartido (validación).
 *
 * <p>Se usa en lugar de {@code @Email}, que acepta {@code usuario@dominio} sin punto: ese valor
 * pasaba la validación y luego lo rechazaba la restricción de la base con un error genérico. Es la
 * misma regla que {@code validarCorreo} del navegador y que los CHECK de {@code persona} y
 * {@code usuario}.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$")
@Size(max = 150)
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Correo {

    String message() default "El correo no tiene un formato válido: debe llevar usuario, arroba y "
            + "un dominio con terminación, por ejemplo nombre@gmail.com";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
