package mx.gob.sedif.sea.exception;

/**
 * Mensajes que devuelve el API.
 *
 * <p>Regla que gobierna este archivo: quien lee el mensaje debe saber que paso
 * y que hacer. Un "operación no válida" obliga a adivinar; un "no puedes
 * aprobar un tramite que ya fue rechazado" se entiende de una vez.
 *
 * <p>Los mensajes de error dicen la causa concreta y, cuando aplica, como
 * corregirla. La unica excepcion es lo relacionado con credenciales: ahi el
 * mensaje es deliberadamente vago, porque precisar si el usuario existe
 * permitiria averiguar que cuentas hay probando nombres.
 *
 * <p>Los detalles tecnicos nunca salen al cliente: van a la bitacora del
 * servidor, donde solo los ve el personal de informatica.
 */
public final class MessageConstants {

    private MessageConstants() {
    }

    // ---- Operaciones correctas ----
    // Dicen que quedo hecho, no solo que "se realizo".
    public static final String OPERACION_EXITOSA = "La operación se completó correctamente";
    public static final String REGISTRO_CREADO = "El registro se dio de alta correctamente";
    public static final String REGISTRO_ACTUALIZADO = "Los cambios se guardaron correctamente";
    public static final String REGISTRO_ELIMINADO =
            "El registro se dio de baja. Deja de aparecer en los formularios, "
                    + "pero los datos que ya lo usan siguen intactos.";

    // ---- Errores generales ----
    public static final String RECURSO_NO_ENCONTRADO =
            "No se encontro el registro. Es posible que se haya dado de baja.";
    public static final String DATOS_INVALIDOS =
            "Revisa los campos marcados: hay datos que faltan o no tienen el formato correcto";
    public static final String ERROR_INTERNO =
            "No se pudo completar la operación por una falla del sistema. "
                    + "Intenta de nuevo; si persiste, reporta a informática.";
    public static final String OPERACION_NO_PERMITIDA =
            "Esta operación no procede en la situación actual del registro";

    // ---- Autenticacion y autorizacion ----
    /**
     * Mismo texto para usuario inexistente y contrasena incorrecta.
     *
     * <p>Es la excepcion a la regla de precision: distinguirlos permitiria
     * averiguar que cuentas existen probando nombres de usuario.
     */
    public static final String CREDENCIALES_INVALIDAS =
            "Usuario o contraseña incorrectos. Verifica ambos datos.";
    public static final String CUENTA_BLOQUEADA =
            "La cuenta quedó bloqueada temporalmente por varios intentos fallidos. "
                    + "Espera unos minutos antes de volver a intentar.";
    public static final String CUENTA_INACTIVA =
            "Esta cuenta fue dada de baja. Solicita a informática que la reactive.";
    public static final String SESION_EXPIRADA =
            "Tu sesión expiró por inactividad. Inicia sesión de nuevo para continuar.";
    public static final String NO_AUTORIZADO =
            "No tienes permiso para esta acción. Si necesitas hacerla, "
                    + "pide a informática que revise tus permisos.";
    public static final String TOKEN_INVALIDO =
            "El enlace no es válido o ya expiró. Solicita uno nuevo.";
    public static final String CUERPO_ILEGIBLE =
            "Los datos enviados no tienen el formato esperado. Recarga la página y vuelve a intentarlo.";
    public static final String METODO_NO_ADMITIDO =
            "Esta operación no está disponible en esa dirección.";
    public static final String CONFLICTO_DATOS =
            "No se pudo guardar porque choca con datos que ya existen (por ejemplo, un registro duplicado). "
                    + "Revisa los datos y vuelve a intentarlo.";
    public static final String EDICION_CONCURRENTE =
            "Otra persona guardó cambios en este registro mientras lo tenías abierto. "
                    + "Recarga la página para ver la versión actual y vuelve a hacer tu cambio.";
    public static final String VERSION_FALTANTE =
            "No se pudo comprobar si el registro cambió mientras lo editabas. "
                    + "Recarga la página y vuelve a intentarlo.";
    public static final String CAMBIO_PASSWORD_REQUERIDO =
            "Define una contraseña propia antes de continuar: la que recibiste es temporal "
                    + "y no permite operar el sistema.";

    // ---- Limite de peticiones ----
    public static final String DEMASIADAS_PETICIONES =
            "Se hicieron demasiadas peticiones desde este equipo. "
                    + "Espera un minuto antes de volver a intentar.";

    // ---- Tramite ----
    // El mensaje de transicion invalida no vive aqui: TransicionInvalidaException
    // lo arma con los dos estados concretos y con lo que si se puede hacer, que
    // es mas util que un texto fijo.
    public static final String MOTIVO_REQUERIDO =
            "Debes elegir un motivo del catalogo. Es lo que se le informa a la "
                    + "persona para que sepa que corregir.";
    public static final String SOLICITUD_NO_ENCONTRADA =
            "No hay ningún trámite con ese folio y esa CURP. Verifica que el folio "
                    + "este completo y que la CURP sea la que usó al registrar.";
    public static final String CONTACTO_REQUERIDO =
            "Falta un medio de contacto. Escribe al menos un correo o un teléfono: "
                    + "es por donde se avisa el resultado del trámite.";
    public static final String TRAMITE_YA_RESUELTO =
            "Este trámite ya fue resuelto y no admite más cambios";

    // ---- Documentos ----
    public static final String ARCHIVO_VACIO =
            "El archivo está vacío. Vuelve a seleccionarlo y comprueba que se "
                    + "haya subido completo.";
    public static final String ARCHIVO_MUY_GRANDE =
            "El archivo pesa más de lo permitido. Reduce su tamaño o toma la "
                    + "fotografia con menor resolución.";
    public static final String TIPO_ARCHIVO_NO_PERMITIDO =
            "Ese tipo de archivo no se acepta. Solo se admiten archivos PDF, "
                    + "JPG y PNG.";
    public static final String ARCHIVO_CONTENIDO_NO_CORRESPONDE =
            "El contenido del archivo no corresponde con su extension. "
                    + "Vuelve a guardarlo en el formato correcto y súbelo de nuevo.";
    public static final String ERROR_ALMACENAR_ARCHIVO =
            "No se pudo guardar el archivo. Intenta de nuevo; si persiste, "
                    + "reporta a informática.";
    public static final String DOCUMENTO_NO_ENCONTRADO =
            "No se encontro el documento. Es posible que se haya retirado del trámite.";

    // ---- Padron ----
    public static final String CURP_DUPLICADA =
            "Ya hay una persona registrada con esa CURP. Si es la misma persona, "
                    + "el sistema reutiliza su registro; si no, verifica la CURP capturada.";
    public static final String CURP_INVALIDA =
            "La CURP no es válida. Revisa que este completa, con sus 18 caracteres, "
                    + "y que corresponda a la que aparece en el documento oficial.";
    public static final String ANIMAL_YA_TIENE_TITULAR =
            "Este animal ya tiene un tutor responsable vigente. Para cambiarlo, "
                    + "registra primero la transferencia de titularidad.";

    // ---- Certificados ----
    public static final String CERTIFICADO_YA_EMITIDO =
            "Este trámite ya tiene certificado emitido. Si necesitas otro, "
                    + "cancela el actual e indica el motivo.";
    public static final String CERTIFICADO_NO_APROBADO =
            "Solo se emite certificado de un trámite aprobado";
    public static final String CERTIFICADO_YA_CANCELADO =
            "Este certificado ya estaba cancelado";

    // ---- Notificaciones ----
    public static final String CONTACTO_YA_REGISTRADO =
            "Este contacto ya se marco como atendido";
    public static final String SIN_CORREO_PARA_ENVIAR =
            "La persona no dejó correo, así que el aviso no se puede enviar solo. "
                    + "Queda en la bandeja de contacto para llamarle.";
}
