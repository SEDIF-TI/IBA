/**
 * Reglas de validación de los campos con formato, en un solo lugar para todo el backend.
 * Área: Núcleo compartido.
 *
 * <p>Cada anotación corresponde a una regla del navegador en {@code frontend/src/utils/validaciones.ts}
 * y a la restricción de la base: {@link CurpValida} (estructura, fecha, entidad y dígito
 * verificador), {@link Correo} (usuario, arroba y dominio con punto), {@link Telefono} (10 dígitos)
 * y {@link CodigoPostal} (5 dígitos). Todas aceptan el valor vacío: la obligatoriedad se declara
 * aparte con {@code @NotBlank}. La prueba {@code ValidacionCamposTest} falla si un {@code *Request}
 * tiene un campo de estos datos sin su anotación.
 */
package mx.gob.sedif.sea.util.validacion;
