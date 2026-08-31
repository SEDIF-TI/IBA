package mx.gob.sedif.sea.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Traduce cualquier excepcion a la respuesta uniforme del API.
 *
 * <p>Regla que gobierna esta clase: al cliente NUNCA se le devuelve el detalle
 * interno. Las trazas y los mensajes tecnicos van a la bitacora del servidor;
 * la respuesta lleva un texto redactado para quien la va a leer.
 *
 * <p>Esto no es cosmetico: un mensaje de error detallado revela nombres de
 * tablas, rutas y versiones, que es justo lo que busca quien esta tanteando el
 * sistema.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Registro no encontrado. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> manejarNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest peticion) {
        log.debug("Recurso no encontrado en {}: {}", peticion.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** Regla de negocio incumplida. El mensaje si es para la persona usuaria. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiResponse<Void>> manejarReglaNegocio(
            ReglaNegocioException ex, HttpServletRequest peticion) {
        log.info("Regla de negocio en {}: {}", peticion.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** Validacion de los DTO de entrada: detalle por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> manejarValidacion(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errores = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.add(Map.of(
                    "campo", error.getField(),
                    "mensaje", error.getDefaultMessage() == null
                            ? MessageConstants.DATOS_INVALIDOS
                            : error.getDefaultMessage()));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(MessageConstants.DATOS_INVALIDOS, errores));
    }

    /** Validacion de parametros sueltos de los controladores. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> manejarViolacionRestriccion(
            ConstraintViolationException ex) {
        List<Map<String, String>> errores = ex.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "campo", v.getPropertyPath().toString(),
                        "mensaje", v.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(MessageConstants.DATOS_INVALIDOS, errores));
    }

    /**
     * Credenciales incorrectas.
     *
     * <p>Se responde lo mismo para usuario inexistente y contrasena erronea:
     * distinguirlos permitiria averiguar que cuentas existen.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCredenciales(HttpServletRequest peticion) {
        log.warn("Intento de acceso fallido desde {}", peticion.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(MessageConstants.CREDENCIALES_INVALIDAS));
    }

    /** Cuenta bloqueada por intentos fallidos. */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCuentaBloqueada() {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.error(MessageConstants.CUENTA_BLOQUEADA));
    }

    /** Cuenta dada de baja. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCuentaInactiva() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(MessageConstants.CUENTA_INACTIVA));
    }

    /** Falta de permisos para la accion solicitada. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarAccesoDenegado(HttpServletRequest peticion) {
        log.warn("Acceso denegado a {}", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(MessageConstants.NO_AUTORIZADO));
    }

    /** Archivo por encima del limite configurado. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> manejarArchivoGrande() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(MessageConstants.ARCHIVO_MUY_GRANDE));
    }

    /** Fallo al guardar o leer del volumen de documentos. */
    @ExceptionHandler(AlmacenamientoException.class)
    public ResponseEntity<ApiResponse<Void>> manejarAlmacenamiento(AlmacenamientoException ex) {
        // La causa tecnica va a la bitacora; al cliente solo el mensaje generico.
        log.error("Fallo de almacenamiento", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(MessageConstants.ERROR_ALMACENAR_ARCHIVO));
    }

    /** Ruta inexistente. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> manejarRutaInexistente() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(MessageConstants.RECURSO_NO_ENCONTRADO));
    }

    /**
     * Red de seguridad para lo no previsto.
     *
     * <p>Registra la traza completa del lado del servidor y devuelve un mensaje
     * generico: el cliente no debe enterarse de que fallo por dentro.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> manejarNoPrevisto(
            Exception ex, HttpServletRequest peticion) {
        log.error("Error no controlado en {} {}", peticion.getMethod(), peticion.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(MessageConstants.ERROR_INTERNO));
    }
}
