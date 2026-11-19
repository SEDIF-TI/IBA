package mx.gob.sedif.sea.util.validacion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * CURP con estructura oficial, fecha de nacimiento existente, clave de entidad válida y dígito
 * verificador correcto. Acepta minúsculas y espacios alrededor (se normaliza antes de revisar) y
 * acepta vacío o nulo.
 * Área: Núcleo compartido (validación). Usa {@code CurpUtil.esValida}, la misma regla que
 * {@code validarCurp} del navegador.
 */
@Documented
@Constraint(validatedBy = CurpValidaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurpValida {

    String message() default "La CURP no es válida: revisa que tenga 18 caracteres, tu fecha de "
            + "nacimiento y el último dígito, tal como aparece en tu documento oficial";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
