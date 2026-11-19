package mx.gob.sedif.sea.util.validacion;

import mx.gob.sedif.sea.core.persona.CurpUtil;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementación de {@link CurpValida}.
 * Área: Núcleo compartido (validación).
 */
public class CurpValidaValidator implements ConstraintValidator<CurpValida, String> {

    /**
     * @param valor   CURP recibida; nula o vacía se considera válida (la obligatoriedad va aparte)
     * @param contexto contexto de Bean Validation
     * @return verdadero si está vacía o si {@code CurpUtil.esValida} la acepta
     */
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || valor.isBlank() || CurpUtil.esValida(valor);
    }
}
