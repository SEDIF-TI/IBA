package mx.gob.sedif.sea.core.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para dar de alta o editar un catalogo.
 *
 * @param clave       referencia estable; solo se toma en cuenta al crear
 * @param nombre      texto que vera la persona usuaria
 * @param descripcion detalle opcional
 * @param orden       posicion de despliegue
 * @param activo      si sigue disponible
 * @param version  versión del registro para el bloqueo optimista; obligatoria al modificar y
 *                 debe ser la que la persona tenía a la vista
 */
public record CatalogoRequest(
        @NotBlank(message = "La clave es obligatoria")
        @Size(max = 50)
        // Mayusculas, numeros y guion bajo: la convencion prohibe acentos y
        // caracteres especiales en las claves.
        @Pattern(regexp = "^[A-Z0-9_]+$",
                 message = "La clave solo admite mayusculas, numeros y guion bajo")
        String clave,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String nombre,

        @Size(max = 500)
        String descripcion,

        @PositiveOrZero(message = "El orden no puede ser negativo")
        Integer orden,

        Boolean activo,

        // Versión que la persona tenía a la vista; obligatoria al modificar (bloqueo optimista).
        Long version) {
}
