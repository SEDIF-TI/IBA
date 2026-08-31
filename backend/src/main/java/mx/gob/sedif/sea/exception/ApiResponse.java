package mx.gob.sedif.sea.exception;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envoltorio uniforme de toda respuesta del API.
 *
 * <p>Que el formato sea siempre el mismo permite al frontend tratar exito y
 * error con un solo interceptor, en lugar de adivinar la forma de cada
 * respuesta.
 *
 * @param success   si la operacion se completo
 * @param message   texto para mostrar a la persona usuaria
 * @param data      carga util; nula en los errores
 * @param errors    detalle de validacion por campo; nulo si no aplica
 * @param timestamp momento de la respuesta
 * @param <T>       tipo de la carga util
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        List<Map<String, String>> errors,
        OffsetDateTime timestamp) {

    /** Respuesta correcta con datos y mensaje. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null, OffsetDateTime.now());
    }

    /** Respuesta correcta con datos y mensaje generico. */
    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, MessageConstants.OPERACION_EXITOSA);
    }

    /** Respuesta correcta sin carga util, para bajas y operaciones sin retorno. */
    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null, null, OffsetDateTime.now());
    }

    /** Respuesta de error. Nunca lleva datos. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, OffsetDateTime.now());
    }

    /** Error de validacion con el detalle por campo. */
    public static <T> ApiResponse<T> error(String message, List<Map<String, String>> errors) {
        return new ApiResponse<>(false, message, null, errors, OffsetDateTime.now());
    }
}
