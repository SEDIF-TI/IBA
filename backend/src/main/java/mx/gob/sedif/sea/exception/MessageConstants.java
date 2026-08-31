package mx.gob.sedif.sea.exception;

/**
 * Mensajes que devuelve el API.
 *
 * <p>Los de error son deliberadamente escuetos: no revelan si un usuario
 * existe, que tabla fallo ni por que. El detalle tecnico va a la bitacora del
 * servidor, donde solo lo ve el personal de informatica.
 */
public final class MessageConstants {

    private MessageConstants() {
    }

    // ---- Operaciones ----
    public static final String OPERACION_EXITOSA = "Operacion realizada correctamente";
    public static final String REGISTRO_CREADO = "Registro creado correctamente";
    public static final String REGISTRO_ACTUALIZADO = "Registro actualizado correctamente";
    public static final String REGISTRO_ELIMINADO = "Registro dado de baja correctamente";

    // ---- Errores generales ----
    public static final String RECURSO_NO_ENCONTRADO = "No se encontro el registro solicitado";
    public static final String DATOS_INVALIDOS = "Los datos enviados no son validos";
    public static final String ERROR_INTERNO = "Ocurrio un error al procesar la solicitud";
    public static final String OPERACION_NO_PERMITIDA = "La operacion no esta permitida";

    // ---- Autenticacion y autorizacion ----
    /**
     * Mismo texto para usuario inexistente y contrasena incorrecta: distinguirlos
     * permitiria averiguar que cuentas existen.
     */
    public static final String CREDENCIALES_INVALIDAS = "Usuario o contrasena incorrectos";
    public static final String CUENTA_BLOQUEADA =
            "La cuenta esta bloqueada temporalmente por intentos fallidos. Intenta mas tarde.";
    public static final String CUENTA_INACTIVA = "La cuenta no esta activa";
    public static final String SESION_EXPIRADA = "La sesion expiro. Inicia sesion nuevamente.";
    public static final String NO_AUTORIZADO = "No tienes autorizacion para realizar esta accion";
    public static final String TOKEN_INVALIDO = "El token no es valido";
    public static final String CAMBIO_PASSWORD_REQUERIDO =
            "Debes cambiar tu contrasena antes de continuar";

    // ---- Limite de peticiones ----
    public static final String DEMASIADAS_PETICIONES =
            "Demasiadas peticiones. Espera un momento antes de volver a intentar.";

    // ---- Tramite ----
    public static final String TRANSICION_INVALIDA =
            "El tramite no puede pasar a ese estado desde su situacion actual";
    public static final String MOTIVO_REQUERIDO =
            "Debes indicar el motivo para realizar esta accion";
    public static final String SOLICITUD_NO_ENCONTRADA =
            "No se encontro ningun tramite con el folio y la CURP proporcionados";
    public static final String CONTACTO_REQUERIDO =
            "Debes proporcionar al menos un correo o un telefono de contacto";

    // ---- Documentos ----
    public static final String ARCHIVO_VACIO = "El archivo esta vacio";
    public static final String ARCHIVO_MUY_GRANDE = "El archivo excede el tamano maximo permitido";
    public static final String TIPO_ARCHIVO_NO_PERMITIDO = "El tipo de archivo no esta permitido";
    public static final String ERROR_ALMACENAR_ARCHIVO = "No se pudo almacenar el archivo";
    public static final String DOCUMENTO_NO_ENCONTRADO = "No se encontro el documento";

    // ---- Padron ----
    public static final String CURP_DUPLICADA = "Ya existe una persona registrada con esa CURP";
    public static final String CURP_INVALIDA = "La CURP no tiene un formato valido";
    public static final String ANIMAL_YA_TIENE_TITULAR =
            "El animal ya tiene una titularidad vigente";
}
