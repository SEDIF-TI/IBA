package mx.gob.sedif.sea.exception;

/**
 * Mensajes que devuelve el API.
 *
 * <p>Regla que gobierna este archivo: quien lee el mensaje debe saber que paso
 * y que hacer. Un "operacion no valida" obliga a adivinar; un "no puedes
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
    public static final String OPERACION_EXITOSA = "La operacion se completo correctamente";
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
            "No se pudo completar la operacion por una falla del sistema. "
                    + "Intenta de nuevo; si persiste, reporta a informatica.";
    public static final String OPERACION_NO_PERMITIDA =
            "Esta operacion no procede en la situacion actual del registro";

    // ---- Autenticacion y autorizacion ----
    /**
     * Mismo texto para usuario inexistente y contrasena incorrecta.
     *
     * <p>Es la excepcion a la regla de precision: distinguirlos permitiria
     * averiguar que cuentas existen probando nombres de usuario.
     */
    public static final String CREDENCIALES_INVALIDAS =
            "Usuario o contrasena incorrectos. Verifica ambos datos.";
    public static final String CUENTA_BLOQUEADA =
            "La cuenta quedo bloqueada temporalmente por varios intentos fallidos. "
                    + "Espera unos minutos antes de volver a intentar.";
    public static final String CUENTA_INACTIVA =
            "Esta cuenta fue dada de baja. Solicita a informatica que la reactive.";
    public static final String SESION_EXPIRADA =
            "Tu sesion expiro por inactividad. Inicia sesion de nuevo para continuar.";
    public static final String NO_AUTORIZADO =
            "No tienes permiso para esta accion. Si necesitas hacerla, "
                    + "pide a informatica que revise tus permisos.";
    public static final String TOKEN_INVALIDO =
            "El enlace no es valido o ya expiro. Solicita uno nuevo.";
    public static final String CAMBIO_PASSWORD_REQUERIDO =
            "Define una contrasena propia antes de continuar: la inicial viene en "
                    + "un archivo de configuracion y no puede seguir en uso.";

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
            "No hay ningun tramite con ese folio y esa CURP. Verifica que el folio "
                    + "este completo y que la CURP sea la que uso al registrar.";
    public static final String CONTACTO_REQUERIDO =
            "Falta un medio de contacto. Escribe al menos un correo o un telefono: "
                    + "es por donde se avisa el resultado del tramite.";
    public static final String TRAMITE_YA_RESUELTO =
            "Este tramite ya fue resuelto y no admite mas cambios";

    // ---- Documentos ----
    public static final String ARCHIVO_VACIO =
            "El archivo esta vacio. Vuelve a seleccionarlo y comprueba que se "
                    + "haya subido completo.";
    public static final String ARCHIVO_MUY_GRANDE =
            "El archivo pesa mas de lo permitido. Reduce su tamano o toma la "
                    + "fotografia con menor resolucion.";
    public static final String TIPO_ARCHIVO_NO_PERMITIDO =
            "Ese tipo de archivo no se acepta. Solo se admiten archivos PDF, "
                    + "JPG y PNG.";
    public static final String ARCHIVO_CONTENIDO_NO_CORRESPONDE =
            "El contenido del archivo no corresponde con su extension. "
                    + "Vuelve a guardarlo en el formato correcto y subelo de nuevo.";
    public static final String ERROR_ALMACENAR_ARCHIVO =
            "No se pudo guardar el archivo. Intenta de nuevo; si persiste, "
                    + "reporta a informatica.";
    public static final String DOCUMENTO_NO_ENCONTRADO =
            "No se encontro el documento. Es posible que se haya retirado del tramite.";

    // ---- Padron ----
    public static final String CURP_DUPLICADA =
            "Ya hay una persona registrada con esa CURP. Si es la misma persona, "
                    + "el sistema reutiliza su registro; si no, verifica la CURP capturada.";
    public static final String CURP_INVALIDA =
            "La CURP no es valida. Revisa que este completa, con sus 18 caracteres, "
                    + "y que corresponda a la que aparece en el documento oficial.";
    public static final String ANIMAL_YA_TIENE_TITULAR =
            "Este animal ya tiene un tutor responsable vigente. Para cambiarlo, "
                    + "registra primero la transferencia de titularidad.";

    // ---- Certificados ----
    public static final String CERTIFICADO_YA_EMITIDO =
            "Este tramite ya tiene certificado emitido. Si necesitas otro, "
                    + "cancela el actual e indica el motivo.";
    public static final String CERTIFICADO_NO_APROBADO =
            "Solo se emite certificado de un tramite aprobado";
    public static final String CERTIFICADO_YA_CANCELADO =
            "Este certificado ya estaba cancelado";

    // ---- Notificaciones ----
    public static final String CONTACTO_YA_REGISTRADO =
            "Este contacto ya se marco como atendido";
    public static final String SIN_CORREO_PARA_ENVIAR =
            "La persona no dejo correo, asi que el aviso no se puede enviar solo. "
                    + "Queda en la bandeja de contacto para llamarle.";
}
